package com.myname.mymodid.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.client.ClientEventHandler;
import com.myname.mymodid.client.SoundPlayer;
import com.myname.mymodid.network.CPayloads.BackupRestorePayload;
import com.myname.mymodid.network.CPayloads.BattleConfigPayload;
import com.myname.mymodid.network.CPayloads.CapturePointConfigPayload;
import com.myname.mymodid.network.CPayloads.CapturePointFlagsPayload;
import com.myname.mymodid.network.CPayloads.ChatPayload;
import com.myname.mymodid.network.CPayloads.MarkRequestPayload;
import com.myname.mymodid.network.CPayloads.SaveBackpackPayload;
import com.myname.mymodid.network.CPayloads.ScoreConfigPayload;
import com.myname.mymodid.network.CPayloads.TeamRequestPayload;
import com.myname.mymodid.network.CPayloads.TeamSelectPayload;
import com.myname.mymodid.network.CPayloads.TeleporterConfigPayload;
import com.myname.mymodid.network.CPayloads.VoiceRequestPayload;
import com.myname.mymodid.network.CPayloads.VoiceUploadPayload;
import com.myname.mymodid.network.SPayloads.CapturePointSyncPayload;
import com.myname.mymodid.network.SPayloads.CapturePointSyncRemovePayload;
import com.myname.mymodid.network.SPayloads.ChatReceivePayload;
import com.myname.mymodid.network.SPayloads.MarkSyncPayload;
import com.myname.mymodid.network.SPayloads.PlayerStatsPayload;
import com.myname.mymodid.network.SPayloads.ScoreSyncPayload;
import com.myname.mymodid.network.SPayloads.ScoreUpdatePayload;
import com.myname.mymodid.network.SPayloads.TeamSyncPayload;
import com.myname.mymodid.network.SPayloads.TeamScoresPayload;
import com.myname.mymodid.network.SPayloads.VoiceNeedUploadPayload;
import com.myname.mymodid.network.SPayloads.VoicePlayPayload;
import com.myname.mymodid.score.CapturePointData;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.score.ScoreTickHandler;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Network {

    private static final int CHUNK_SIZE = 30000;
    private static final int MAX_PER_PLAYER = 200;
    private static final int MAX_CACHE_ENTRIES = 100;
    private static final long MAX_CACHE_BYTES = 20L * 1024 * 1024;
    private static long currentCacheBytes = 0;
    private static final Map<String, Set<String>> playerUploads = new HashMap<>();
    private static final Map<String, Object[]> receiving = new HashMap<>();
    public static final Map<String, byte[]> voiceCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            if (size() > MAX_CACHE_ENTRIES || currentCacheBytes > MAX_CACHE_BYTES) {
                currentCacheBytes -= eldest.getValue().length;
                return true;
            }
            return false;
        }
    };
    private static final Map<String, Object[]> clientReceiving = new HashMap<>();

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToAllPlayers(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");

        reg.playToServer(ScoreConfigPayload.TYPE, ScoreConfigPayload.STREAM_CODEC, Network::handleScoreConfig);
        reg.playToServer(CapturePointConfigPayload.TYPE, CapturePointConfigPayload.STREAM_CODEC, Network::handleCapturePointConfig);
        reg.playToServer(ChatPayload.TYPE, ChatPayload.STREAM_CODEC, Network::handleChat);
        reg.playToServer(VoiceUploadPayload.TYPE, VoiceUploadPayload.STREAM_CODEC, Network::handleVoiceUpload);
        reg.playToServer(VoiceRequestPayload.TYPE, VoiceRequestPayload.STREAM_CODEC, Network::handleVoiceRequest);
        reg.playToServer(MarkRequestPayload.TYPE, MarkRequestPayload.STREAM_CODEC, Network::handleMarkRequest);
        reg.playToServer(BattleConfigPayload.TYPE, BattleConfigPayload.STREAM_CODEC, Network::handleBattleConfig);
        reg.playToServer(TeamRequestPayload.TYPE, TeamRequestPayload.STREAM_CODEC, Network::handleTeamRequest);
        reg.playToServer(TeamSelectPayload.TYPE, TeamSelectPayload.STREAM_CODEC, Network::handleTeamSelect);
        reg.playToServer(TeleporterConfigPayload.TYPE, TeleporterConfigPayload.STREAM_CODEC, Network::handleTeleporterConfig);
        reg.playToServer(SaveBackpackPayload.TYPE, SaveBackpackPayload.STREAM_CODEC, Network::handleSaveBackpack);
        reg.playToServer(BackupRestorePayload.TYPE, BackupRestorePayload.STREAM_CODEC, Network::handleBackupRestore);
        reg.playToServer(CapturePointFlagsPayload.TYPE, CapturePointFlagsPayload.STREAM_CODEC, Network::handleCapturePointFlags);

        reg.playToClient(ScoreUpdatePayload.TYPE, ScoreUpdatePayload.STREAM_CODEC, Network::handleScoreUpdate);
        reg.playToClient(ScoreSyncPayload.TYPE, ScoreSyncPayload.STREAM_CODEC, Network::handleScoreSync);
        reg.playToClient(CapturePointSyncPayload.TYPE, CapturePointSyncPayload.STREAM_CODEC, Network::handleCapturePointSync);
        reg.playToClient(CapturePointSyncRemovePayload.TYPE, CapturePointSyncRemovePayload.STREAM_CODEC, Network::handleCapturePointRemove);
        reg.playToClient(PlayerStatsPayload.TYPE, PlayerStatsPayload.STREAM_CODEC, Network::handlePlayerStats);
        reg.playToClient(ChatReceivePayload.TYPE, ChatReceivePayload.STREAM_CODEC, Network::handleChatReceive);
        reg.playToClient(VoicePlayPayload.TYPE, VoicePlayPayload.STREAM_CODEC, Network::handleVoicePlay);
        reg.playToClient(VoiceNeedUploadPayload.TYPE, VoiceNeedUploadPayload.STREAM_CODEC, Network::handleVoiceNeedUpload);
        reg.playToClient(MarkSyncPayload.TYPE, MarkSyncPayload.STREAM_CODEC, Network::handleMarkSync);
        reg.playToClient(TeamSyncPayload.TYPE, TeamSyncPayload.STREAM_CODEC, Network::handleTeamSync);
        reg.playToClient(TeamScoresPayload.TYPE, TeamScoresPayload.STREAM_CODEC, Network::handleTeamScores);
    }

    private static void handleScoreConfig(ScoreConfigPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                ScoreData data = ScoreData.get(level);
                if (data == null) return;

                if (m.action() == 1) {
                    data.reset();
                    ScoreTickHandler.resetScoreboard(level);
                    for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                        if (cp.getLevel() != level) continue;
                        cp.resetToNeutral();
                    }
                    ScoreTickHandler.syncAllStats(level);
                } else if (m.action() == 0) {
                    int init = m.initialScore() <= 0 ? 10000 : m.initialScore();
                    int per = m.scorePerSecond() <= 0 ? 10 : m.scorePerSecond();
                    data.initialScore = init;
                    data.scorePerSecond = per;
                    data.setDirty();
                } else if (m.action() == 2) {
                    data.pveEnabled = !data.pveEnabled;
                    data.setDirty();
                } else if (m.action() == 3) {
                    double atk = Math.max(0, m.zombieAttack());
                    double spd = Math.max(0.5, m.zombieSpeed()) / 15.0;
                    double hp = Math.max(1, m.zombieHealth());
                    data.zombieAttack = atk;
                    data.zombieSpeed = spd;
                    data.zombieHealth = hp;
                    data.setDirty();
                } else if (m.action() == 4) {
                    data.gameStarted = !data.gameStarted;
                    data.setDirty();
                    level.getDataStorage().save();
                    for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                        if (cp.getLevel() != level) continue;
                        cp.resetToNeutral();
                    }
                }

                for (ServerPlayer pl : level.players()) {
                    sendToPlayer(pl, new ScoreSyncPayload(data.pveEnabled, data.gameStarted,
                        data.redScore, data.blueScore, data.initialScore, data.scorePerSecond,
                        data.zombieAttack, data.zombieSpeed, data.zombieHealth));
                }
            } catch (Exception e) {}
        });
    }

    private static void handleCapturePointConfig(CapturePointConfigPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                BlockEntity te = level.getBlockEntity(new net.minecraft.core.BlockPos(m.x(), m.y(), m.z()));
                if (te instanceof TileEntityCapturePoint p) {
                    p.setRadius(m.radius());
                    p.setCaptureTime(m.captureTime());
                    p.setPointName(m.name());
                    p.setPveSpawnInterval(m.pveInterval());
                    p.setPveSpawnCount(m.pveCount());
                    p.setPveMinCount(m.pveMin());
                    p.setPveMaxCount(m.pveMax());
                    level.sendBlockUpdated(new net.minecraft.core.BlockPos(m.x(), m.y(), m.z()),
                        p.getBlockState(), p.getBlockState(), 3);
                }
            } catch (Exception e) {}
        });
    }

    private static void handleChat(ChatPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sender)) return;
                if (m.message().isEmpty()) return;
                String prefix = m.teamOnly() ? "§a[队伍]§r" : "§e[全场]§r";
                String formatted = prefix + sender.getScoreboardName() + ": " + m.message();
                for (ServerPlayer p : sender.serverLevel().players()) {
                    if (m.teamOnly() && !sameTeam(sender, p)) continue;
                    sendToPlayer(p, new ChatReceivePayload(formatted, m.message()));
                }
            } catch (Exception e) {}
        });
    }

    private static boolean sameTeam(Player a, Player b) {
        if (a.getTeam() == null || b.getTeam() == null) return false;
        return a.getTeam().equals(b.getTeam());
    }

    private static void handleVoiceUpload(VoiceUploadPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                if (m.name() == null || m.data() == null || m.data().length == 0) return;
                if (m.data().length > CHUNK_SIZE) return;
                String playerName = sp.getScoreboardName();

                if (m.chunkIndex() == 0) {
                    Set<String> uploaded = playerUploads.computeIfAbsent(playerName, k -> new HashSet<>());
                    if (!uploaded.contains(m.name())) {
                        if (uploaded.size() >= MAX_PER_PLAYER) return;
                        uploaded.add(m.name());
                    }
                }

                Object[] state = receiving.get(m.name());
                if (state == null || (Integer) state[0] != m.totalChunks()) {
                    state = new Object[]{m.totalChunks(), new ArrayList<byte[]>()};
                    receiving.put(m.name(), state);
                }
                @SuppressWarnings("unchecked")
                List<byte[]> chunks = (List<byte[]>) state[1];
                while (chunks.size() <= m.chunkIndex()) chunks.add(null);
                chunks.set(m.chunkIndex(), m.data());

                boolean complete = chunks.size() == m.totalChunks();
                if (complete) {
                    for (byte[] c : chunks) { if (c == null) { complete = false; break; } }
                }
                if (complete) {
                    byte[] full = concat(chunks);
                    receiving.remove(m.name());
                    if (full.length > 1024 * 1024) return;

                    byte[] old = voiceCache.get(m.name());
                    if (old != null) currentCacheBytes -= old.length;
                    voiceCache.put(m.name(), full);
                    currentCacheBytes += full.length;

                    forwardToPlayers(m.name(), full, sp);
                }
            } catch (Exception e) {}
        });
    }

    private static void handleVoiceRequest(VoiceRequestPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sender)) return;
                byte[] cached = voiceCache.get(m.name());
                if (cached != null) {
                    forwardToPlayers(m.name(), cached, sender);
                } else {
                    sendToPlayer(sender, new VoiceNeedUploadPayload(m.name()));
                }
            } catch (Exception e) {}
        });
    }

    private static void handleMarkRequest(MarkRequestPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sender)) return;
                String senderName = sender.getScoreboardName();
                PlayerTeam senderTeam = sender.level().getScoreboard().getPlayersTeam(senderName);
                String senderTeamName = senderTeam == null ? null : senderTeam.getName();

                for (ServerPlayer p : sender.serverLevel().players()) {
                    PlayerTeam t = p.level().getScoreboard().getPlayersTeam(p.getScoreboardName());
                    String tn = t == null ? null : t.getName();
                    boolean sameTeam = senderTeamName == null ? (tn == null) : senderTeamName.equals(tn);
                    if (sameTeam) {
                        sendToPlayer(p, new MarkSyncPayload(m.x(), m.y(), m.z(),
                            m.hitX(), m.hitY(), m.hitZ(), senderName, false));
                    }
                }
            } catch (Exception e) {}
        });
    }

    private static void handleBattleConfig(BattleConfigPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                TeamData data = TeamData.get(level);
                if (data == null) return;
                boolean changed = false;
                if (m.action() == 1) {
                    if (data.addTeam(m.teamName(), m.color())) {
                        sp.sendSystemMessage(Component.literal(ChatFormatting.GREEN
                            + Component.translatable("msg.team.added", m.teamName()).getString()));
                        changed = true;
                    } else if (m.teamName() == null || m.teamName().trim().isEmpty()) {
                        sp.sendSystemMessage(Component.literal(ChatFormatting.RED
                            + Component.translatable("msg.team.empty").getString()));
                    } else if (data.find(m.teamName()) != null) {
                        sp.sendSystemMessage(Component.literal(ChatFormatting.RED
                            + Component.translatable("msg.team.exists", m.teamName()).getString()));
                    } else {
                        sp.sendSystemMessage(Component.literal(ChatFormatting.RED
                            + Component.translatable("msg.team.limit").getString()));
                    }
                } else if (m.action() == 2) {
                    if (data.removeTeam(m.teamName())) {
                        sp.sendSystemMessage(Component.literal(ChatFormatting.YELLOW
                            + Component.translatable("msg.team.removed", m.teamName()).getString()));
                        changed = true;
                    }
                } else {
                    changed = data.updateTeam(m.teamName(), m.newName(), m.color(), m.side(),
                        m.maxPlayers(), m.sx(), m.sy(), m.sz(), m.hasSpawn());
                }
                if (changed) {
                    data.setDirty();
                    sendToAllPlayers(buildTeamSync(level));
                }
            } catch (Exception e) {}
        });
    }

    private static void handleTeamRequest(TeamRequestPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                sendToPlayer(sp, buildTeamSync(level));
            } catch (Exception e) {}
        });
    }

    private static void handleTeamSelect(TeamSelectPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                BlockEntity te = level.getBlockEntity(new net.minecraft.core.BlockPos(m.x(), m.y(), m.z()));
                if (te instanceof TileEntityTeamTeleporter t) {
                    t.selectedTeam = m.teamName() == null ? "" : m.teamName();
                    t.setChanged();
                }
            } catch (Exception e) {}
        });
    }

    private static void handleTeleporterConfig(TeleporterConfigPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                BlockEntity te = level.getBlockEntity(new net.minecraft.core.BlockPos(m.x(), m.y(), m.z()));
                if (te instanceof TileEntityTeamTeleporter t) {
                    t.enableSwap = m.enableSwap();
                    t.enableTp = m.enableTp();
                    t.setChanged();
                }
            } catch (Exception e) {}
        });
    }

    private static void handleSaveBackpack(SaveBackpackPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer p)) return;
                ServerLevel level = p.serverLevel();
                if (m.teamName() == null || m.teamName().isEmpty()) return;
                TeamData data = TeamData.get(level);
                if (data == null) return;
                ItemStack[] template = new ItemStack[41];
                for (int i = 0; i < 36; i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    template[i] = s == null || s.isEmpty() ? null : s.copy();
                }
                for (int i = 0; i < 4; i++) {
                    ItemStack s = p.getInventory().getItem(36 + i);
                    template[36 + i] = s == null || s.isEmpty() ? null : s.copy();
                }
                ItemStack off = p.getInventory().getItem(40);
                template[40] = off == null || off.isEmpty() ? null : off.copy();
                java.util.List<ItemStack> acc = com.myname.mymodid.util.AccessoriesCompat.getAccessories(p);
                data.saveBackpack(m.teamName(), template, acc == null || acc.isEmpty() ? null : acc);
                data.setDirty();
                sendToAllPlayers(buildTeamSync(level));
            } catch (Exception e) {}
        });
    }

    private static void handleBackupRestore(BackupRestorePayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer p)) return;
                ServerLevel level = p.serverLevel();
                com.myname.mymodid.score.TeamBackupData.Backup backup =
                    com.myname.mymodid.score.TeamBackupData.get(level).takeBackup(p.getUUID());
                if (backup == null || backup.inv == null || backup.inv.length < 41) {
                    p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.backup.none"));
                    return;
                }
                for (int i = 0; i < 36; i++) {
                    p.getInventory().setItem(i,
                        backup.inv[i] == null ? ItemStack.EMPTY : backup.inv[i].copy());
                }
                for (int i = 0; i < 4; i++) {
                    p.getInventory().setItem(36 + i,
                        backup.inv[36 + i] == null ? ItemStack.EMPTY : backup.inv[36 + i].copy());
                }
                p.getInventory().setItem(40,
                    backup.inv[40] == null ? ItemStack.EMPTY : backup.inv[40].copy());
                p.getInventory().setChanged();
                p.containerMenu.broadcastChanges();
                com.myname.mymodid.util.AccessoriesCompat.setAccessories(p, backup.acc);
                p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.backup.restored"));
            } catch (Exception e) {}
        });
    }

    private static TeamSyncPayload buildTeamSync(ServerLevel level) {
        TeamData data = TeamData.get(level);
        java.util.List<TeamSyncPayload.TeamSyncEntry> list = new java.util.ArrayList<>();
        if (data != null) {
            for (TeamConfig t : data.teams) {
                list.add(new TeamSyncPayload.TeamSyncEntry(t.name, t.color, t.side, t.maxPlayers,
                    t.spawnX, t.spawnY, t.spawnZ, t.hasSpawn));
            }
        }
        return new TeamSyncPayload(list);
    }

    private static void handleCapturePointFlags(CapturePointFlagsPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                ServerLevel level = sp.serverLevel();
                BlockEntity te = level.getBlockEntity(new net.minecraft.core.BlockPos(m.x(), m.y(), m.z()));
                if (!(te instanceof TileEntityCapturePoint cp)) return;

                if (m.setBase()) cp.setMainBase(m.baseVal());
                if (m.setRevive()) cp.setReviveMode(m.reviveVal());

                if (m.ownerCycle() && cp.isMainBase()) {
                    boolean pve = ScoreData.get(level).pveEnabled;
                    String cur = cp.getOwnerTeam();
                    String newOwner;
                    if (pve) {
                        newOwner = "PVEHUMAN".equals(cur) ? "PvE" : "PVEHUMAN";
                    } else {
                        boolean isRed = cur != null && (cur.toLowerCase().contains("red") || cur.contains("红"));
                        newOwner = isRed ? "Blue" : "Red";
                    }
                    cp.setOwnerTeam(newOwner);
                }

                level.sendBlockUpdated(new net.minecraft.core.BlockPos(m.x(), m.y(), m.z()),
                    cp.getBlockState(), cp.getBlockState(), 3);
            } catch (Exception e) {}
        });
    }

    private static void handleScoreUpdate(ScoreUpdatePayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientEventHandler.redScore = m.redScore();
            ClientEventHandler.blueScore = m.blueScore();
        });
    }

    private static void handleScoreSync(ScoreSyncPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Player p = ctx.player();
                if (p == null || p.level() == null) return;
                ScoreData client = ScoreData.getClient();
                client.pveEnabled = m.pveEnabled();
                client.gameStarted = m.gameStarted();
                client.redScore = m.redScore();
                client.blueScore = m.blueScore();
                client.initialScore = m.initialScore();
                client.scorePerSecond = m.scorePerSecond();
                client.zombieAttack = m.zombieAttack();
                client.zombieSpeed = m.zombieSpeed();
                client.zombieHealth = m.zombieHealth();
            } catch (Exception e) {}
        });
    }

    private static void handleCapturePointSync(CapturePointSyncPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Player p = ctx.player();
                if (p == null || p.level() == null) return;
                CapturePointData data = CapturePointData.getClient();
                data.update(m.x(), m.y(), m.z(), m.name(),
                    m.owner().isEmpty() ? null : m.owner(),
                    m.capturing().isEmpty() ? null : m.capturing(),
                    m.progress(), m.radius());
            } catch (Exception e) {}
        });
    }

    private static void handleCapturePointRemove(CapturePointSyncRemovePayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Player p = ctx.player();
                if (p == null || p.level() == null) return;
                CapturePointData.getClient().remove(m.x(), m.y(), m.z());
            } catch (Exception e) {}
        });
    }

    private static void handlePlayerStats(PlayerStatsPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientEventHandler.playerStats = m.stats();
            ClientEventHandler.pveEnabled = m.pveEnabled();
        });
    }

    private static void handleChatReceive(ChatReceivePayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (net.minecraft.client.Minecraft.getInstance().gui != null) {
                    net.minecraft.client.Minecraft.getInstance().gui.getChat()
                        .addMessage(Component.literal(m.formatted()));
                }
            } catch (Exception e) {}
        });
    }

    private static void handleVoicePlay(VoicePlayPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (m.name() == null || m.data() == null) return;
                Object[] state = clientReceiving.get(m.name());
                if (state == null || (Integer) state[0] != m.totalChunks()) {
                    state = new Object[]{m.totalChunks(), new ArrayList<byte[]>()};
                    clientReceiving.put(m.name(), state);
                }
                @SuppressWarnings("unchecked")
                List<byte[]> chunks = (List<byte[]>) state[1];
                while (chunks.size() <= m.chunkIndex()) chunks.add(null);
                chunks.set(m.chunkIndex(), m.data());

                boolean complete = chunks.size() == m.totalChunks();
                if (complete) {
                    for (byte[] c : chunks) { if (c == null) { complete = false; break; } }
                }
                if (complete) {
                    byte[] full = concat(chunks);
                    clientReceiving.remove(m.name());
                    SoundPlayer.playFromBytes(m.name(), full);
                }
            } catch (Exception e) {}
        });
    }

    private static void handleVoiceNeedUpload(VoiceNeedUploadPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                byte[] data = SoundPlayer.readVoiceData(m.name());
                if (data == null) return;
                int total = (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
                for (int i = 0; i < total; i++) {
                    int start = i * CHUNK_SIZE;
                    int end = Math.min(start + CHUNK_SIZE, data.length);
                    byte[] chunk = Arrays.copyOfRange(data, start, end);
                    sendToServer(new VoiceUploadPayload(m.name(), i, total, chunk));
                }
            } catch (Exception e) {}
        });
    }

    private static void handleMarkSync(MarkSyncPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (m.remove()) {
                ClientEventHandler.removeMark(m.x(), m.y(), m.z());
            } else {
                ClientEventHandler.addMark(m.x(), m.y(), m.z(),
                    m.hitX(), m.hitY(), m.hitZ(), m.playerName());
            }
        });
    }

    private static void handleTeamSync(TeamSyncPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                java.util.List<TeamConfig> list = new java.util.ArrayList<>();
                if (m.teams() != null) {
                    for (TeamSyncPayload.TeamSyncEntry e : m.teams()) {
                        TeamConfig c = new TeamConfig(e.name(), e.color(), e.side());
                        c.maxPlayers = e.maxPlayers();
                        c.spawnX = e.spawnX();
                        c.spawnY = e.spawnY();
                        c.spawnZ = e.spawnZ();
                        c.hasSpawn = e.hasSpawn();
                        list.add(c);
                    }
                }
                TeamData.clientSet(list);
                net.minecraft.client.gui.screens.Screen cur = net.minecraft.client.Minecraft.getInstance().screen;
                if (cur instanceof com.myname.mymodid.client.gui.GuiBattlePortal g) {
                    g.onTeamsSynced();
                } else if (cur instanceof com.myname.mymodid.client.gui.GuiTeamTeleporter g) {
                    g.onTeamsSynced();
                }
            } catch (Exception e) {}
        });
    }

    private static void handleTeamScores(TeamScoresPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ScoreData client = ScoreData.getClient();
                client.mode = m.mode();
                client.teamScores.clear();
                if (m.scores() != null) client.teamScores.putAll(m.scores());
            } catch (Exception e) {}
        });
    }

    private static void forwardToPlayers(String name, byte[] full, ServerPlayer sender) {
        int total = (full.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (ServerPlayer o : sender.serverLevel().players()) {
            if (o != sender) {
                for (int i = 0; i < total; i++) {
                    int start = i * CHUNK_SIZE;
                    int end = Math.min(start + CHUNK_SIZE, full.length);
                    byte[] chunk = Arrays.copyOfRange(full, start, end);
                    sendToPlayer(o, new VoicePlayPayload(name, i, total, chunk));
                }
            }
        }
    }

    private static byte[] concat(List<byte[]> chunks) {
        int total = 0;
        for (byte[] c : chunks) total += c.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }
}
