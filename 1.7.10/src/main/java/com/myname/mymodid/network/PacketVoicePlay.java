package com.myname.mymodid.network;

import com.myname.mymodid.client.SoundPlayer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketVoicePlay implements IMessage {
    public String name;
    public int chunkIndex;
    public int totalChunks;
    public byte[] data;

    private static final Map<String, Object[]> receiving = new HashMap<String, Object[]>();

    public PacketVoicePlay() {}
    public PacketVoicePlay(String name, int chunkIndex, int totalChunks, byte[] data) {
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

    public static class Handler implements IMessageHandler<PacketVoicePlay, IMessage> {
        @Override
        public IMessage onMessage(PacketVoicePlay message, MessageContext ctx) {
            if (message.name == null || message.data == null) return null;
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
                SoundPlayer.playFromBytes(message.name, full);
            }
            return null;
        }
    }
}
