package com.myname.mymodid.network;

import com.myname.mymodid.client.SoundPlayer;
import com.myname.mymodid.MyMod;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketVoiceNeedUpload implements IMessage {
    public String name;

    public PacketVoiceNeedUpload() {}
    public PacketVoiceNeedUpload(String name) { this.name = name; }

    @Override
    public void fromBytes(ByteBuf buf) {
        int nl = buf.readInt();
        byte[] nb = new byte[nl];
        buf.readBytes(nb);
        name = new String(nb);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] nb = name.getBytes();
        buf.writeInt(nb.length);
        buf.writeBytes(nb);
    }

    public static class Handler implements IMessageHandler<PacketVoiceNeedUpload, IMessage> {
        @Override
        public IMessage onMessage(PacketVoiceNeedUpload message, MessageContext ctx) {
            if (message.name == null) return null;
            byte[] data = SoundPlayer.readVoiceData(message.name);
                        if (data != null && data.length > 0) {
                int cs = PacketVoiceUpload.CHUNK_SIZE;
                int total = (data.length + cs - 1) / cs;
                for (int ci = 0; ci < total; ci++) {
                    int s = ci * cs, e = Math.min(s + cs, data.length);
                    byte[] chunk = java.util.Arrays.copyOfRange(data, s, e);
                    MyMod.NETWORK.sendToServer(new PacketVoiceUpload(message.name, ci, total, chunk));
                }
            }

            return null;
        }
    }
}
