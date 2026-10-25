package com.myname.mymodid.network;

import com.myname.mymodid.MyMod;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PacketVoiceUpload implements IMessage {
    public static final int CHUNK_SIZE = 30000;

    public String name;
    public int chunkIndex;
    public int totalChunks;
    public byte[] data;

    private static final int MAX_PER_PLAYER = 200;
    private static final int MAX_CACHE_ENTRIES = 100;
    private static final long MAX_CACHE_BYTES = 20L * 1024 * 1024;
    private static long currentCacheBytes = 0;

    private static final Map<String, Set<String>> playerUploads = new HashMap<String, Set<String>>();
    private static final Map<String, Object[]> receiving = new HashMap<String, Object[]>();

    public static final Map<String, byte[]> cache = new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            if (size() > MAX_CACHE_ENTRIES || currentCacheBytes > MAX_CACHE_BYTES) {
                currentCacheBytes -= eldest.getValue().length;
                return true;
            }
            return false;
        }
    };

    public PacketVoiceUpload() {}
    public PacketVoiceUpload(String name, int chunkIndex, int totalChunks, byte[] data) {
        this.name = name;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int nl = buf.readInt();
        byte[] nb = new byte[nl];
        buf.readBytes(nb);
        name = new String(nb);
        chunkIndex = buf.readInt();
        totalChunks = buf.readInt();
        int dl = buf.readInt();
        data = new byte[dl];
        buf.readBytes(data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] nb = name.getBytes();
        buf.writeInt(nb.length);
        buf.writeBytes(nb);
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    public static void forwardToPlayers(String name, byte[] full, EntityPlayerMP sender) {
        int total = (full.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (Object o : sender.world.playerEntities) {
            if (o instanceof EntityPlayerMP && o != sender) {
                for (int i = 0; i < total; i++) {
                    int start = i * CHUNK_SIZE;
                    int end = Math.min(start + CHUNK_SIZE, full.length);
                    byte[] chunk = Arrays.copyOfRange(full, start, end);
                    MyMod.NETWORK.sendTo(new PacketVoicePlay(name, i, total, chunk), (EntityPlayerMP) o);
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

    public static class Handler implements IMessageHandler<PacketVoiceUpload, IMessage> {
        @Override
        public IMessage onMessage(PacketVoiceUpload message, MessageContext ctx) {
            if (message.name == null || message.data == null || message.data.length == 0) return null;
            if (message.data.length > CHUNK_SIZE) return null;

            String playerName = ctx.getServerHandler().player.getName();

            if (message.chunkIndex == 0) {
                Set<String> uploaded = playerUploads.get(playerName);
                if (uploaded == null) {
                    uploaded = new HashSet<String>();
                    playerUploads.put(playerName, uploaded);
                }
                if (!uploaded.contains(message.name)) {
                    if (uploaded.size() >= MAX_PER_PLAYER) return null;
                    uploaded.add(message.name);
                }
            }

            Object[] state = receiving.get(message.name);
            if (state == null || (Integer) state[0] != message.totalChunks) {
                state = new Object[]{message.totalChunks, new ArrayList<byte[]>()};
                receiving.put(message.name, state);
            }
            List<byte[]> chunks = (List<byte[]>) state[1];
            while (chunks.size() <= message.chunkIndex) chunks.add(null);
            chunks.set(message.chunkIndex, message.data);

            boolean complete = chunks.size() == message.totalChunks;
            if (complete) {
                for (byte[] c : chunks) { if (c == null) { complete = false; break; } }
            }
            if (complete) {
                byte[] full = concat(chunks);
                receiving.remove(message.name);
                if (full.length > 1024 * 1024) return null;

                byte[] old = cache.get(message.name);
                if (old != null) currentCacheBytes -= old.length;
                cache.put(message.name, full);
                currentCacheBytes += full.length;

                forwardToPlayers(message.name, full, ctx.getServerHandler().player);
            }
            return null;
        }
    }
}
