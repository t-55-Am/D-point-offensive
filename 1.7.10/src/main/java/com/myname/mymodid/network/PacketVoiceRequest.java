package com.myname.mymodid.network;

import com.myname.mymodid.MyMod;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketVoiceRequest implements IMessage {
    public String name;

    public PacketVoiceRequest() {}
    public PacketVoiceRequest(String name) { this.name = name; }

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

    public static class Handler implements IMessageHandler<PacketVoiceRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketVoiceRequest message, MessageContext ctx) {
            if (message.name == null) return null;
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            byte[] cached = PacketVoiceUpload.cache.get(message.name);
            if (cached != null && cached.length > 0) {
                PacketVoiceUpload.forwardToPlayers(message.name, cached, sender);
            } else {
                MyMod.NETWORK.sendTo(new PacketVoiceNeedUpload(message.name), sender);
            }
            return null;
        }
    }
}
