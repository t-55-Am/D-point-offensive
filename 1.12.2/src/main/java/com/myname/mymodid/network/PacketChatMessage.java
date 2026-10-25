package com.myname.mymodid.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import java.nio.charset.StandardCharsets;

public class PacketChatMessage implements IMessage {
    public String message = "";
    public boolean teamOnly = false;

    public PacketChatMessage() {}
    public PacketChatMessage(String msg, boolean team) {
        this.message = msg == null ? "" : msg;
        this.teamOnly = team;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        message = new String(bytes, StandardCharsets.UTF_8);
        teamOnly = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
        buf.writeBoolean(teamOnly);
    }

    public static class Handler implements IMessageHandler<PacketChatMessage, IMessage> {
        @Override
        public IMessage onMessage(PacketChatMessage msg, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().player;
            if (sender == null || msg.message.isEmpty()) return null;
            String prefix = msg.teamOnly ? "§a[队伍]§r" : "§e[全场]§r";
            String formatted = prefix + sender.getName() + ": " + msg.message;
            for (Object obj : sender.world.playerEntities) {
                EntityPlayerMP p = (EntityPlayerMP) obj;
                if (msg.teamOnly && !sameTeam(sender, p)) continue;
                com.myname.mymodid.MyMod.NETWORK.sendTo(
                    new PacketChatReceive(formatted, msg.message), p);
            }
            return null;
        }
        private boolean sameTeam(EntityPlayerMP a, EntityPlayerMP b) {
            if (a.getTeam() == null || b.getTeam() == null) return false;
            return a.getTeam().isSameTeam(b.getTeam());
        }
    }
}
