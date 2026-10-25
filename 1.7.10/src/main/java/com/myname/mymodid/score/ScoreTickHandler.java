package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.network.PacketPlayerStats;
import com.myname.mymodid.network.PacketScoreUpdate;
import com.myname.mymodid.network.PacketTeamScores;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

public class ScoreTickHandler {
    private int tickCounter = 0;
    private int lastRed = -1;
    private int lastBlue = -1;
    private int statsSyncTimer = 0;
    private boolean warnedDisabled = false;

    public static Map<String, int[]> collectAllStats(World world) {
        Map<String, int[]> map = new HashMap<String, int[]>();
        try {
            for (Object obj : world.playerEntities) {
                if (obj instanceof EntityPlayerMP) {
                    EntityPlayerMP p = (EntityPlayerMP) obj;
                    map.put(p.getCommandSenderName(), PlayerStats.get(p.getCommandSenderName()));
                }
            }
        } catch (Exception e) {}
        return map;
    }

    public static void syncAllStats(World world) {
        try {
            MyMod.NETWORK.sendToAll(new PacketPlayerStats(collectAllStats(world), true));
        } catch (Exception e) {}
    }

    /** 判断占点归属是否为红方（PvP）。v1.1.11：优先按队伍颜色推断（0=红→红方，1=蓝→蓝方），其余颜色回退 side 字段。 */
    public static boolean isRedOwner(World world, String owner) {
        if (owner == null) return false;
        try {
            TeamData data = TeamData.get(world);
            if (data != null) {
                TeamConfig cfg = data.find(owner);
                if (cfg != null) {
                    if (cfg.color == 0) return true;
                    if (cfg.color == 1) return false;
                    return cfg.side == 0;
                }
            }
        } catch (Exception e) {
            // 兜底走关键字
        }
        String lower = owner.toLowerCase();
        return lower.contains("red") || owner.contains("红") || lower.contains("attack") || owner.equals("1");
    }

    public static void resetScoreboard(World world) {
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
            if (server != null && server.getCommandManager() != null) {
                // 用游戏命令重置所有玩家所有计分板分数，不依赖Scoreboard API
                server.getCommandManager().executeCommand(server, "scoreboard players reset *");
            }
        } catch (Exception e) {}
    }



    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;
        tickCounter++;
        statsSyncTimer++;
        if (statsSyncTimer >= 100) {
            statsSyncTimer = 0;
            syncAllStats(event.world);
        }
        if (tickCounter < 20) return;
        tickCounter = 0;
        ScoreData data = ScoreData.get(event.world);
        if (data == null) return;
        if (!data.gameStarted) return;
        if (data.pveEnabled) return;
        if (!MyModConfig.enableScore) {
            if (!warnedDisabled) {
                warnedDisabled = true;
                try {
                    for (Object obj : event.world.playerEntities) {
                        if (obj instanceof EntityPlayerMP) {
                            ((EntityPlayerMP) obj).addChatMessage(new ChatComponentText(
                                EnumChatFormatting.YELLOW + StatCollector.translateToLocal("dp.msg.scoreoff")));
                        }
                    }
                } catch (Exception e) {}
            }
            return;
        }
        warnedDisabled = false;
        if (data.scorePerSecond <= 0) data.scorePerSecond = 10;
        // v1.1.11：多队伍模式（每队独立分数，据点落后队掉分，淘汰制）
        if (data.mode == 1) {
            tickMultiTeam(event.world, data);
            return;
        }
        int red = 0, blue = 0;
        for (Object te : event.world.loadedTileEntityList) {
            if (te instanceof TileEntityCapturePoint) {
                TileEntityCapturePoint cp = (TileEntityCapturePoint) te;
                // 修复：主基地也参与掉分统计（此前全部跳过，导致占点差统计缺失）
                String owner = cp.getOwnerTeam();
                if (owner == null) continue;
                if (isRedOwner(event.world, owner)) red++;
                else blue++;   // 非红即蓝：任意队伍名都能被统计到，不再依赖特定关键词
            }
        }

        if (red > blue) {
            data.blueScore = Math.max(0, data.blueScore - (red - blue) * data.scorePerSecond);
        } else if (blue > red) {
            data.redScore = Math.max(0, data.redScore - (blue - red) * data.scorePerSecond);
        }
        if (data.redScore <= 0 || data.blueScore <= 0) {
            String winner = data.redScore <= 0 ? StatCollector.translateToLocal("dp.msg.winblue") : StatCollector.translateToLocal("dp.msg.winred");
            try {
                for (Object obj : event.world.playerEntities) {
                    if (obj instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) obj).addChatMessage(new ChatComponentText(
                            EnumChatFormatting.GOLD + winner));
                    }
                }
            } catch (Exception e) {}
            data.reset();
            resetScoreboard(event.world);
            for (Object te : event.world.loadedTileEntityList) {
                if (te instanceof TileEntityCapturePoint) {
                    ((TileEntityCapturePoint) te).resetToNeutral();
                }
            }
        }
        data.markDirty();
        if (data.redScore != lastRed || data.blueScore != lastBlue) {
            lastRed = data.redScore;
            lastBlue = data.blueScore;
            try {
                MyMod.NETWORK.sendToAll(new PacketScoreUpdate(data.redScore, data.blueScore, data.mode));
                syncAllStats(event.world);
            } catch (Exception e) {}
        }
    }


    /** v1.1.11 多队伍模式：每队按据点数量独立计分，落后队掉分；分数耗尽淘汰；存活 1 队获胜。 */
    private int lastTeamScoresSize = -1;
    private int lastTeamMode = -1;

    private void tickMultiTeam(World world, ScoreData data) {
        if (data.scorePerSecond <= 0) data.scorePerSecond = 10;
        TeamData teams = TeamData.get(world);
        if (teams == null || teams.teams == null) return;
        // 队伍名单同步：清理已删除的队伍，新队伍按初始分加入
        java.util.List<String> valid = new java.util.ArrayList<String>();
        for (TeamConfig t : teams.teams) valid.add(t.name);
        data.teamScores.keySet().retainAll(valid);
        for (TeamConfig t : teams.teams) {
            if (!data.teamScores.containsKey(t.name)) data.teamScores.put(t.name, data.initialScore);
        }
        // 统计每个队伍当前据点数
        java.util.Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
        for (Object te : world.loadedTileEntityList) {
            if (te instanceof TileEntityCapturePoint) {
                String owner = ((TileEntityCapturePoint) te).getOwnerTeam();
                if (owner == null) continue;
                Integer v = counts.get(owner);
                counts.put(owner, v == null ? 1 : v + 1);
            }
        }
        int max = 0;
        for (Integer v : counts.values()) if (v != null && v > max) max = v;
        // 落后队掉分：差 (max - 本队据点数) × 每秒分数
        boolean changed = false;
        java.util.List<String> dead = new java.util.ArrayList<String>();
        for (java.util.Map.Entry<String, Integer> e : new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(data.teamScores.entrySet())) {
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
        // 淘汰广播
        for (String d : dead) {
            data.teamScores.remove(d);
            broadcastToAll(world, StatCollector.translateToLocalFormatted("dp.msg.eliminated", d));
        }
        // 冠军判定：已创建队伍 >= 2 且存活队伍 <= 1
        if (teams.teams.size() >= 2 && data.teamScores.size() <= 1) {
            String winner = data.teamScores.isEmpty() ? null : data.teamScores.keySet().iterator().next();
            if (winner != null) {
                broadcastToAll(world, StatCollector.translateToLocalFormatted("dp.msg.winmulti", winner));
            }
            data.reset();
            resetScoreboard(world);
            for (Object te : world.loadedTileEntityList) {
                if (te instanceof TileEntityCapturePoint) {
                    ((TileEntityCapturePoint) te).resetToNeutral();
                }
            }
        }
        data.markDirty();
        if (changed || data.teamScores.size() != lastTeamScoresSize || data.mode != lastTeamMode) {
            lastTeamScoresSize = data.teamScores.size();
            lastTeamMode = data.mode;
            try {
                MyMod.NETWORK.sendToAll(new PacketTeamScores(data.mode, data.teamScores));
                syncAllStats(world);
            } catch (Exception e) {}
        }
    }

    private void broadcastToAll(World world, String msg) {
        try {
            for (Object obj : world.playerEntities) {
                if (obj instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) obj).addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + msg));
                }
            }
        } catch (Exception e) {}
    }

    @SubscribeEvent
    public void onPlayerKill(LivingDeathEvent event) {
        if (event.entity.worldObj.isRemote) return;

        ScoreData data = ScoreData.get(event.entity.worldObj);
        // 没开始游戏时不统计击杀
        if (data == null || !data.gameStarted) return;

        boolean pve = data.pveEnabled;

        if (event.source.getEntity() instanceof EntityPlayer) {
            EntityPlayer killer = (EntityPlayer) event.source.getEntity();
            if (pve) {
                PlayerStats.addKill(killer.getCommandSenderName());
            } else {
                if (event.entity instanceof EntityPlayer) {
                    PlayerStats.addKill(killer.getCommandSenderName());
                }
            }
        }

        if (event.entity instanceof EntityPlayer) {
            PlayerStats.addDeath(event.entity.getCommandSenderName());
        }

        syncAllStats(event.entity.worldObj);
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (event.player == null || event.player.worldObj == null) return;
        String msg = event.message == null ? "" : event.message.trim();

        if (msg.equalsIgnoreCase("score on") || msg.equalsIgnoreCase("score off")) {
            MyModConfig.enableScore = msg.equalsIgnoreCase("score on");
            event.player.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + String.format(StatCollector.translateToLocal("dp.msg.scoretoggle"),
                    MyModConfig.enableScore ? StatCollector.translateToLocal("dp.msg.on") : StatCollector.translateToLocal("dp.msg.off"))));
            event.setCanceled(true);
            return;
        }

        if (msg.equalsIgnoreCase("pve on") || msg.equalsIgnoreCase("pve off")) {
            ScoreData data = ScoreData.get(event.player.worldObj);
            data.pveEnabled = msg.equalsIgnoreCase("pve on");
            data.markDirty();
            event.player.worldObj.mapStorage.saveAllData();
            event.player.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + String.format(StatCollector.translateToLocal("dp.msg.pvetoggle"),
                    data.pveEnabled ? StatCollector.translateToLocal("dp.msg.on") : StatCollector.translateToLocal("dp.msg.off"))));
            event.setCanceled(true);
            return;
        }

        if (msg.equalsIgnoreCase("resetscore")) {
            ScoreData data = ScoreData.get(event.player.worldObj);
            data.reset();
            resetScoreboard(event.player.worldObj);
            for (Object te : event.player.worldObj.loadedTileEntityList) {
                if (te instanceof TileEntityCapturePoint) {
                    ((TileEntityCapturePoint) te).resetToNeutral();
                }
            }
            event.player.worldObj.mapStorage.saveAllData();
            syncAllStats(event.player.worldObj);
            event.player.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + StatCollector.translateToLocal("dp.msg.reset")));
            event.setCanceled(true);
            return;
        }
    }
}
