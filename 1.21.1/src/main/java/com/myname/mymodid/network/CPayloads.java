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
import com.myname.mymodid.score.MarkData;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.score.ScoreTickHandler;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CPayloads {
    private CPayloads() {}

    public record ScoreConfigPayload(int action, int initialScore, int scorePerSecond,
                                     boolean togglePve, double zombieAttack, double zombieSpeed,
                                     double zombieHealth) implements CustomPacketPayload {
        public static final Type<ScoreConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "score_config"));
        public static final StreamCodec<ByteBuf, ScoreConfigPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public ScoreConfigPayload decode(ByteBuf buf) {
                return new ScoreConfigPayload(buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
            @Override public void encode(ByteBuf buf, ScoreConfigPayload p) {
                buf.writeInt(p.action); buf.writeInt(p.initialScore); buf.writeInt(p.scorePerSecond);
                buf.writeBoolean(p.togglePve);
                buf.writeDouble(p.zombieAttack); buf.writeDouble(p.zombieSpeed); buf.writeDouble(p.zombieHealth);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CapturePointConfigPayload(int x, int y, int z, int radius, int captureTime, String name,
                                            int pveInterval, int pveCount, int pveMin, int pveMax) implements CustomPacketPayload {
        public static final Type<CapturePointConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "cp_config"));
        public static final StreamCodec<ByteBuf, CapturePointConfigPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public CapturePointConfigPayload decode(ByteBuf buf) {
                return new CapturePointConfigPayload(buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), readStr(buf),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
            }
            @Override public void encode(ByteBuf buf, CapturePointConfigPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z);
                buf.writeInt(p.radius); buf.writeInt(p.captureTime);
                writeStr(buf, p.name);
                buf.writeInt(p.pveInterval); buf.writeInt(p.pveCount); buf.writeInt(p.pveMin); buf.writeInt(p.pveMax);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChatPayload(String message, boolean teamOnly) implements CustomPacketPayload {
        public static final Type<ChatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "chat_msg"));
        public static final StreamCodec<ByteBuf, ChatPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChatPayload::message,
            ByteBufCodecs.BOOL, ChatPayload::teamOnly,
            ChatPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record VoiceUploadPayload(String name, int chunkIndex, int totalChunks, byte[] data) implements CustomPacketPayload {
        public static final Type<VoiceUploadPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "voice_upload"));
        public static final StreamCodec<ByteBuf, VoiceUploadPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public VoiceUploadPayload decode(ByteBuf buf) {
                return new VoiceUploadPayload(readStr(buf), buf.readInt(), buf.readInt(), readBytes(buf));
            }
            @Override public void encode(ByteBuf buf, VoiceUploadPayload p) {
                writeStr(buf, p.name); buf.writeInt(p.chunkIndex); buf.writeInt(p.totalChunks);
                buf.writeInt(p.data.length); buf.writeBytes(p.data);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record VoiceRequestPayload(String name) implements CustomPacketPayload {
        public static final Type<VoiceRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "voice_req"));
        public static final StreamCodec<ByteBuf, VoiceRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, VoiceRequestPayload::name,
            VoiceRequestPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record MarkRequestPayload(int x, int y, int z, float hitX, float hitY, float hitZ) implements CustomPacketPayload {
        public static final Type<MarkRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "mark_req"));
        public static final StreamCodec<ByteBuf, MarkRequestPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public MarkRequestPayload decode(ByteBuf buf) {
                return new MarkRequestPayload(buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat());
            }
            @Override public void encode(ByteBuf buf, MarkRequestPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z);
                buf.writeFloat(p.hitX); buf.writeFloat(p.hitY); buf.writeFloat(p.hitZ);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BattleConfigPayload(int action, String teamName, String newName, int color, int side,
                                      int maxPlayers, int sx, int sy, int sz, boolean hasSpawn) implements CustomPacketPayload {
        public static final Type<BattleConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "battle_config"));
        public static final StreamCodec<ByteBuf, BattleConfigPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public BattleConfigPayload decode(ByteBuf buf) {
                return new BattleConfigPayload(buf.readInt(), readStr(buf), readStr(buf),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
            }
            @Override public void encode(ByteBuf buf, BattleConfigPayload p) {
                buf.writeInt(p.action);
                writeStr(buf, p.teamName); writeStr(buf, p.newName);
                buf.writeInt(p.color); buf.writeInt(p.side); buf.writeInt(p.maxPlayers);
                buf.writeInt(p.sx); buf.writeInt(p.sy); buf.writeInt(p.sz);
                buf.writeBoolean(p.hasSpawn);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TeamRequestPayload() implements CustomPacketPayload {
        public static final Type<TeamRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "team_req"));
        public static final StreamCodec<ByteBuf, TeamRequestPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public TeamRequestPayload decode(ByteBuf buf) { return new TeamRequestPayload(); }
            @Override public void encode(ByteBuf buf, TeamRequestPayload p) {}
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TeamSelectPayload(int x, int y, int z, String teamName) implements CustomPacketPayload {
        public static final Type<TeamSelectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "team_select"));
        public static final StreamCodec<ByteBuf, TeamSelectPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public TeamSelectPayload decode(ByteBuf buf) {
                return new TeamSelectPayload(buf.readInt(), buf.readInt(), buf.readInt(), readStr(buf));
            }
            @Override public void encode(ByteBuf buf, TeamSelectPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z); writeStr(buf, p.teamName);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TeleporterConfigPayload(int x, int y, int z, boolean enableSwap, boolean enableTp) implements CustomPacketPayload {
        public static final Type<TeleporterConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "teleporter_config"));
        public static final StreamCodec<ByteBuf, TeleporterConfigPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public TeleporterConfigPayload decode(ByteBuf buf) {
                return new TeleporterConfigPayload(buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readBoolean(), buf.readBoolean());
            }
            @Override public void encode(ByteBuf buf, TeleporterConfigPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z);
                buf.writeBoolean(p.enableSwap); buf.writeBoolean(p.enableTp);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SaveBackpackPayload(String teamName) implements CustomPacketPayload {
        public static final Type<SaveBackpackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "save_backpack"));
        public static final StreamCodec<ByteBuf, SaveBackpackPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public SaveBackpackPayload decode(ByteBuf buf) { return new SaveBackpackPayload(readStr(buf)); }
            @Override public void encode(ByteBuf buf, SaveBackpackPayload p) { writeStr(buf, p.teamName); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BackupRestorePayload() implements CustomPacketPayload {
        public static final Type<BackupRestorePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "backup_restore"));
        public static final StreamCodec<ByteBuf, BackupRestorePayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public BackupRestorePayload decode(ByteBuf buf) { return new BackupRestorePayload(); }
            @Override public void encode(ByteBuf buf, BackupRestorePayload p) {}
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CapturePointFlagsPayload(int x, int y, int z, boolean setBase, boolean baseVal,
                                           boolean setRevive, boolean reviveVal, boolean ownerCycle) implements CustomPacketPayload {
        public static final Type<CapturePointFlagsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "cp_flags"));
        public static final StreamCodec<ByteBuf, CapturePointFlagsPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public CapturePointFlagsPayload decode(ByteBuf buf) {
                return new CapturePointFlagsPayload(buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
            }
            @Override public void encode(ByteBuf buf, CapturePointFlagsPayload p) {
                buf.writeInt(p.x); buf.writeInt(p.y); buf.writeInt(p.z);
                buf.writeBoolean(p.setBase); buf.writeBoolean(p.baseVal);
                buf.writeBoolean(p.setRevive); buf.writeBoolean(p.reviveVal);
                buf.writeBoolean(p.ownerCycle);
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
