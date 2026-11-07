package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.network.SPayloads;
import com.myname.mymodid.network.SPayloads.CapturePointSyncPayload;
import com.myname.mymodid.network.SPayloads.PlayerStatsPayload;
import com.myname.mymodid.network.SPayloads.ScoreSyncPayload;
import com.myname.mymodid.network.SPayloads.ScoreUpdatePayload;
import com.myname.mymodid.network.SPayloads.TeamScoresPayload;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class ScoreTickHandler {
    private int tickCounter = 0;
    private int lastRed = -1;
    private int lastBlue = -1;
    private int statsSyncTimer = 0;
    private boolean warnedDisabled = false;

    public static Map<String, int[]> collectAllStats(ServerLevel level) {
        Map<String, int[]> map = new HashMap<>();
        try {
            for (Player p : level.players()) {
                if (p instanceof ServerPlayer sp) {
                    map.put(sp.getScoreboardName(), PlayerStats.get(sp.getScoreboardName()));
                }
            }
        } catch (Exception e) {}
        return map;
    }

    public static void syncAllStats(ServerLevel level) {
        try {
            Network.sendToAllPlayers(new PlayerStatsPayload(collectAllStats(level), ScoreData.get(level).pveEnabled));
        } catch (Exception e) {}
    }

    public static boolean isRedOwner(ServerLevel level, String owner) {
        if (owner == null) return false;
        try {
            TeamData data = TeamData.get(level);
            if (data != null) {
                TeamConfig cfg = data.find(owner);
                if (cfg != null) {
                    if (cfg.color == 0) return true;
                    if (cfg.color == 1) return false;
                    return cfg.side == 0;
                }
            }
        } catch (Exception e) {
        }
        String lower = owner.toLowerCase();
        return lower.contains("red") || owner.contains("红") || lower.contains("attack") || owner.equals("1");
    }

    public static void resetScoreboard(ServerLevel level) {
        try {
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack(), "scoreboard players reset *");
        } catch (Exception e) {}
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        try {
            ScoreData data = ScoreData.get(level);
            Network.sendToPlayer(sp, new ScoreSyncPayload(data.pveEnabled, data.gameStarted,
                data.redScore, data.blueScore, data.initialScore, data.scorePerSecond,
                data.zombieAttack, data.zombieSpeed, data.zombieHealth));
            Network.sendToPlayer(sp, new PlayerStatsPayload(collectAllStats(level), data.pveEnabled));
            for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                if (cp.getLevel() != level) continue;
                Network.sendToPlayer(sp, new CapturePointSyncPayload(
                    cp.getBlockPos().getX(), cp.getBlockPos().getY(), cp.getBlockPos().getZ(),
                    cp.getPointName(), cp.getOwnerTeam(), cp.getCapturingTeam(),
                    cp.getCaptureProgress(), cp.getRadius()));
            }
        } catch (Exception e) {}
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level lv = event.getLevel();
        if (!(lv instanceof ServerLevel level)) return;
        if (level.isClientSide) return;
        tickCounter++;
        statsSyncTimer++;
        if (statsSyncTimer >= 100) {
            statsSyncTimer = 0;
            syncAllStats(level);
        }
        if (tickCounter < 20) return;
        tickCounter = 0;
        ScoreData data = ScoreData.get(level);
        if (data == null) return;
        if (!data.gameStarted) return;
        if (data.pveEnabled) return;
        if (!MyModConfig.ENABLE_SCORE.get()) {
            if (!warnedDisabled) {
                warnedDisabled = true;
                try {
                    for (Player p : level.players()) {
                        if (p instanceof ServerPlayer sp) {
                            sp.sendSystemMessage(Component.literal(
                                ChatFormatting.YELLOW + Component.translatable("dp.msg.scoreoff").getString()));
                        }
                    }
                } catch (Exception e) {}
            }
            return;
        }
        warnedDisabled = false;
        if (data.scorePerSecond <= 0) data.scorePerSecond = 10;
        if (data.mode == 1) {
            tickMultiTeam(level, data);
            return;
        }
        int red = 0, blue = 0;
        for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
            if (cp.getLevel() != level) continue;
            if (cp.isMainBase()) continue;
            String owner = cp.getOwnerTeam();
            if (owner == null) continue;
            if (isRedOwner(level, owner)) red++;
            else blue++;
        }

        if (red > blue) {
            data.blueScore = Math.max(0, data.blueScore - (red - blue) * data.scorePerSecond);
        } else if (blue > red) {
            data.redScore = Math.max(0, data.redScore - (blue - red) * data.scorePerSecond);
        }
        if (data.redScore <= 0 || data.blueScore <= 0) {
            String winner = data.redScore <= 0
                ? Component.translatable("dp.msg.winblue").getString()
                : Component.translatable("dp.msg.winred").getString();
            try {
                level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(ChatFormatting.GOLD + winner), false);
            } catch (Exception e) {}
            data.gameStarted = false;
            data.reset();
            resetScoreboard(level);
            for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                if (cp.getLevel() != level) continue;
                cp.resetToNeutral();
            }
        }
        data.setDirty();
        if (data.redScore != lastRed || data.blueScore != lastBlue) {
            lastRed = data.redScore;
            lastBlue = data.blueScore;
            try {
                Network.sendToAllPlayers(new ScoreUpdatePayload(data.redScore, data.blueScore));
                syncAllStats(level);
            } catch (Exception e) {}
        }
    }

    private int lastTeamScoresSize = -1;
    private int lastTeamMode = -1;

    private void tickMultiTeam(ServerLevel level, ScoreData data) {
        if (data.scorePerSecond <= 0) data.scorePerSecond = 10;
        TeamData teams = TeamData.get(level);
        if (teams == null || teams.teams == null) return;
        java.util.List<String> valid = new java.util.ArrayList<>();
        for (TeamConfig t : teams.teams) valid.add(t.name);
        data.teamScores.keySet().retainAll(valid);
        for (TeamConfig t : teams.teams) {
            if (!data.teamScores.containsKey(t.name)) data.teamScores.put(t.name, data.initialScore);
        }
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
            if (cp.getLevel() != level) continue;
            String owner = cp.getOwnerTeam();
            if (owner == null) continue;
            counts.merge(owner, 1, Integer::sum);
        }
        int max = 0;
        for (Integer v : counts.values()) if (v != null && v > max) max = v;
        boolean changed = false;
        java.util.List<String> dead = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e :
                new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(data.teamScores.entrySet())) {
            Integer mine = counts.get(e.getKey());
            int m = mine == null ? 0 : mine;
            int diff = max - m;
            if (diff > 0) {
                int ns = e.getValue() - diff * data.scorePerSecond;
                if (ns <= 0) {
                    ns = 0;
                    dead.add(e.getKey());
                }
                data.teamScores.put(e.getKey(), ns);
                changed = true;
            }
        }
        for (String d : dead) {
            data.teamScores.remove(d);
            broadcastToAll(level, Component.translatable("dp.msg.eliminated", d).getString());
        }
        if (teams.teams.size() >= 2 && data.teamScores.size() <= 1) {
            String winner = data.teamScores.isEmpty() ? null : data.teamScores.keySet().iterator().next();
            if (winner != null) {
                broadcastToAll(level, Component.translatable("dp.msg.winmulti", winner).getString());
            }
            data.gameStarted = false;
            data.reset();
            resetScoreboard(level);
            for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                if (cp.getLevel() != level) continue;
                cp.resetToNeutral();
            }
        }
        data.setDirty();
        if (changed || data.teamScores.size() != lastTeamScoresSize || data.mode != lastTeamMode) {
            lastTeamScoresSize = data.teamScores.size();
            lastTeamMode = data.mode;
            try {
                Network.sendToAllPlayers(new SPayloads.TeamScoresPayload(data.mode, data.teamScores));
                syncAllStats(level);
            } catch (Exception e) {}
        }
    }

    private void broadcastToAll(ServerLevel level, String msg) {
        try {
            for (Player p : level.players()) {
                if (p instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal(ChatFormatting.GOLD + msg));
                }
            }
        } catch (Exception e) {}
    }

    @SubscribeEvent
    public void onPlayerKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        ScoreData data = ScoreData.get(level);
        if (data == null || !data.gameStarted) return;

        boolean pve = data.pveEnabled;

        if (event.getSource().getEntity() instanceof Player killer) {
            if (pve) {
                PlayerStats.addKill(killer.getScoreboardName());
            } else {
                if (event.getEntity() instanceof Player) {
                    PlayerStats.addKill(killer.getScoreboardName());
                }
            }
        }

        if (event.getEntity() instanceof Player dead) {
            PlayerStats.addDeath(dead.getScoreboardName());
        }

        syncAllStats(level);
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null || player.level() == null) return;
        String msg = event.getMessage() == null ? "" : event.getMessage().getString().trim();
        if (msg.isEmpty()) return;

        if (msg.equalsIgnoreCase("score on") || msg.equalsIgnoreCase("score off")) {
            MyModConfig.ENABLE_SCORE.set(msg.equalsIgnoreCase("score on"));
            player.sendSystemMessage(Component.literal(
                ChatFormatting.GREEN + String.format(Component.translatable("dp.msg.scoretoggle").getString(),
                    MyModConfig.ENABLE_SCORE.get()
                        ? Component.translatable("dp.msg.on").getString()
                        : Component.translatable("dp.msg.off").getString())));
            event.setCanceled(true);
            return;
        }

        if (msg.equalsIgnoreCase("pve on") || msg.equalsIgnoreCase("pve off")) {
            if (player.level() instanceof ServerLevel level) {
                ScoreData data = ScoreData.get(level);
                data.pveEnabled = msg.equalsIgnoreCase("pve on");
                data.setDirty();
                level.getDataStorage().save();
                player.sendSystemMessage(Component.literal(
                    ChatFormatting.GREEN + String.format(Component.translatable("dp.msg.pvetoggle").getString(),
                        data.pveEnabled
                            ? Component.translatable("dp.msg.on").getString()
                            : Component.translatable("dp.msg.off").getString())));
                event.setCanceled(true);
            }
            return;
        }

        if (msg.equalsIgnoreCase("resetscore")) {
            if (player.level() instanceof ServerLevel level) {
                ScoreData data = ScoreData.get(level);
                data.reset();
                resetScoreboard(level);
                for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                    if (cp.getLevel() != level) continue;
                    cp.resetToNeutral();
                }
                level.getDataStorage().save();
                syncAllStats(level);
                player.sendSystemMessage(Component.literal(
                    ChatFormatting.GREEN + Component.translatable("dp.msg.reset").getString()));
                event.setCanceled(true);
            }
            return;
        }
    }
}
