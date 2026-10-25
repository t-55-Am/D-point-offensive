package com.myname.mymodid.network;

import com.myname.mymodid.MyMod;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketMarkRequest implements IMessage {
    public int x, y, z;
    public float hitX, hitY, hitZ;

    public PacketMarkRequest() {}

    public PacketMarkRequest(int x, int y, int z, float hitX, float hitY, float hitZ) {
        this.x = x; this.y = y; this.z = z;
        this.hitX = hitX; this.hitY = hitY; this.hitZ = hitZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt();
        hitX = buf.readFloat(); hitY = buf.readFloat(); hitZ = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z);
        buf.writeFloat(hitX); buf.writeFloat(hitY); buf.writeFloat(hitZ);
    }

    public static class Handler implements IMessageHandler<PacketMarkRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketMarkRequest message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().player;
            String senderName = sender.getName();
            net.minecraft.scoreboard.Team senderTeam = sender.world.getScoreboard().getPlayersTeam(senderName);
            String senderTeamName = (senderTeam == null) ? null : senderTeam.getName();

            for (Object o : net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
                if (o instanceof EntityPlayerMP) {
                    EntityPlayerMP p = (EntityPlayerMP) o;
                    net.minecraft.scoreboard.Team t = p.world.getScoreboard().getPlayersTeam(p.getName());
                    String tn = (t == null) ? null : t.getName();
                    boolean sameTeam = (senderTeamName == null) ? (tn == null) : senderTeamName.equals(tn);
                    if (sameTeam) {
                        MyMod.NETWORK.sendTo(new com.myname.mymodid.network.PacketMarkSync(
                            message.x, message.y, message.z,
                            message.hitX, message.hitY, message.hitZ, senderName, false), p);
                    }
                }
            }
            return null;
        }
    }
}
