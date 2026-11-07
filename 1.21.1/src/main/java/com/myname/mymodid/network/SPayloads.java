package com.myname.mymodid.network;

import java.util.HashMap;
import java.util.Map;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.client.ClientEventHandler;
import com.myname.mymodid.client.SoundPlayer;
import com.myname.mymodid.score.CapturePointData;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class SPayloads {
    private SPayloads() {}

    public record ScoreUpdatePayload(int redScore, int blueScore) implements CustomPacketPayload {
        public static final Type<ScoreUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "score_update"));
        public static final StreamCodec<ByteBuf, ScoreUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ScoreUpdatePayload::redScore,
            ByteBufCodecs.INT, ScoreUpdatePayload::blueScore,
            ScoreUpdatePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ScoreSyncPayload(boolean pveEnabled, boolean gameStarted, int redScore, int blueScore,
                                   int initialScore, int scorePerSecond, double zombieAttack,
                                   double zombieSpeed, double zombieHealth) implements CustomPacketPayload {
        public static final Type<ScoreSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "score_sync"));
        public static final StreamCodec<ByteBuf, ScoreSyncPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public ScoreSyncPayload decode(ByteBuf buf) {
                return new ScoreSyncPayload(buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
            @Override public void encode(ByteBuf buf, ScoreSyncPayload p) {
                buf.writeBoolean(p.pveEnabled); buf.writeBoolean(p.gameStarted);
                buf.writeInt(p.redScore); buf.writeInt(p.blueScore);
                buf.writeInt(p.initialScore); buf.writeInt(p.scorePerSecond);
                buf.writeDouble(p.zombieAttack); buf.writeDouble(p.zombieSpeed); buf.writeDouble(p.zombieHealth);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CapturePointSyncPayload(int x, int y, int z, String name, String owner,
                                          String capturing, double progress, int radius) implements CustomPacketPayload {
        public static final Type<CapturePointSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "cp_sync"));
        public static final StreamCodec<ByteBuf, CapturePointSyncPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public CapturePointSyncPayload decode(ByteBuf buf) {
                return new CapturePointSyncPayload(buf.readInt(), buf.readInt(), buf.readInt(),
                    readStr(buf), readStr(buf), readStr(buf), buf.readDouble(), buf.readInt());
            }
            @Override public void encode(ByteBuf buf, CapturePointSyncPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z);
                writeStr(buf, p.name); writeStr(buf, p.owner); writeStr(buf, p.capturing);
                buf.writeDouble(p.progress); buf.writeInt(p.radius);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CapturePointSyncRemovePayload(int x, int y, int z) implements CustomPacketPayload {
        public static final Type<CapturePointSyncRemovePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "cp_remove"));
        public static final StreamCodec<ByteBuf, CapturePointSyncRemovePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CapturePointSyncRemovePayload::x,
            ByteBufCodecs.INT, CapturePointSyncRemovePayload::y,
            ByteBufCodecs.INT, CapturePointSyncRemovePayload::z,
            CapturePointSyncRemovePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PlayerStatsPayload(Map<String, int[]> stats, boolean pveEnabled) implements CustomPacketPayload {
        public static final Type<PlayerStatsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "player_stats"));
        public static final StreamCodec<ByteBuf, PlayerStatsPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public PlayerStatsPayload decode(ByteBuf buf) {
                int count = buf.readInt();
                Map<String, int[]> stats = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    String name = readStr(buf);
                    int k = buf.readInt(), a = buf.readInt(), c = buf.readInt(), d = buf.readInt();
                    stats.put(name, new int[]{k, a, c, d});
                }
                return new PlayerStatsPayload(stats, buf.readBoolean());
            }
            @Override public void encode(ByteBuf buf, PlayerStatsPayload p) {
                buf.writeInt(p.stats.size());
                for (Map.Entry<String, int[]> e : p.stats.entrySet()) {
                    writeStr(buf, e.getKey());
                    int[] s = e.getValue();
                    buf.writeInt(s[0]); buf.writeInt(s[1]); buf.writeInt(s[2]); buf.writeInt(s[3]);
                }
                buf.writeBoolean(p.pveEnabled);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChatReceivePayload(String formatted, String message) implements CustomPacketPayload {
        public static final Type<ChatReceivePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "chat_receive"));
        public static final StreamCodec<ByteBuf, ChatReceivePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChatReceivePayload::formatted,
            ByteBufCodecs.STRING_UTF8, ChatReceivePayload::message,
            ChatReceivePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record VoicePlayPayload(String name, int chunkIndex, int totalChunks, byte[] data) implements CustomPacketPayload {
        public static final Type<VoicePlayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "voice_play"));
        public static final StreamCodec<ByteBuf, VoicePlayPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public VoicePlayPayload decode(ByteBuf buf) {
                return new VoicePlayPayload(readStr(buf), buf.readInt(), buf.readInt(), readBytes(buf));
            }
            @Override public void encode(ByteBuf buf, VoicePlayPayload p) {
                writeStr(buf, p.name); buf.writeInt(p.chunkIndex); buf.writeInt(p.totalChunks);
                buf.writeInt(p.data.length); buf.writeBytes(p.data);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record VoiceNeedUploadPayload(String name) implements CustomPacketPayload {
        public static final Type<VoiceNeedUploadPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "voice_need"));
        public static final StreamCodec<ByteBuf, VoiceNeedUploadPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, VoiceNeedUploadPayload::name,
            VoiceNeedUploadPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record MarkSyncPayload(int x, int y, int z, float hitX, float hitY, float hitZ,
                                  String playerName, boolean remove) implements CustomPacketPayload {
        public static final Type<MarkSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "mark_sync"));
        public static final StreamCodec<ByteBuf, MarkSyncPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public MarkSyncPayload decode(ByteBuf buf) {
                int x = buf.readInt(), y = buf.readInt(), z = buf.readInt();
                float hx = buf.readFloat(), hy = buf.readFloat(), hz = buf.readFloat();
                boolean remove = buf.readBoolean();
                String name = remove ? "" : readStr(buf);
                return new MarkSyncPayload(x, y, z, hx, hy, hz, name, remove);
            }
            @Override public void encode(ByteBuf buf, MarkSyncPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z);
                buf.writeFloat(p.hitX); buf.writeFloat(p.hitY); buf.writeFloat(p.hitZ);
                buf.writeBoolean(p.remove);
                if (!p.remove && p.playerName != null) writeStr(buf, p.playerName);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TeamSyncPayload(java.util.List<TeamSyncEntry> teams) implements CustomPacketPayload {
        public record TeamSyncEntry(String name, int color, int side, int maxPlayers,
                                    int spawnX, int spawnY, int spawnZ, boolean hasSpawn) {}
        public static final Type<TeamSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "team_sync"));
        public static final StreamCodec<ByteBuf, TeamSyncPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public TeamSyncPayload decode(ByteBuf buf) {
                int n = buf.readInt();
                java.util.List<TeamSyncEntry> list = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) {
                    list.add(new TeamSyncEntry(readStr(buf), buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean()));
                }
                return new TeamSyncPayload(list);
            }
            @Override public void encode(ByteBuf buf, TeamSyncPayload p) {
                buf.writeInt(p.teams == null ? 0 : p.teams.size());
                if (p.teams != null) {
                    for (TeamSyncEntry e : p.teams) {
                        writeStr(buf, e.name);
                        buf.writeInt(e.color); buf.writeInt(e.side); buf.writeInt(e.maxPlayers);
                        buf.writeInt(e.spawnX); buf.writeInt(e.spawnY); buf.writeInt(e.spawnZ);
                        buf.writeBoolean(e.hasSpawn);
                    }
                }
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TeamScoresPayload(int mode, java.util.Map<String, Integer> scores) implements CustomPacketPayload {
        public static final Type<TeamScoresPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "team_scores"));
        public static final StreamCodec<ByteBuf, TeamScoresPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public TeamScoresPayload decode(ByteBuf buf) {
                int mode = buf.readInt();
                int n = buf.readInt();
                java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
                for (int i = 0; i < n; i++) {
                    map.put(readStr(buf), buf.readInt());
                }
                return new TeamScoresPayload(mode, map);
            }
            @Override public void encode(ByteBuf buf, TeamScoresPayload p) {
                buf.writeInt(p.mode);
                if (p.scores == null) { buf.writeInt(0); return; }
                buf.writeInt(p.scores.size());
                for (Map.Entry<String, Integer> e : p.scores.entrySet()) {
                    writeStr(buf, e.getKey());
                    buf.writeInt(e.getValue() == null ? 0 : e.getValue());
                }
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void writeStr(ByteBuf buf, String s) {
        byte[] bytes = (s == null ? "" : s).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readStr(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return bytes;
    }
}
