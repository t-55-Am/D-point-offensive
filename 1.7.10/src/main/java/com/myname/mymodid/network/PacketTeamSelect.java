package com.myname.mymodid.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** 传送方块：客户端选中队伍 → 服务端存入 TE */
public class PacketTeamSelect implements IMessage {
    public int x, y, z;
    public String teamName;

    public PacketTeamSelect() {}

    public PacketTeamSelect(int x, int y, int z, String teamName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.teamName = teamName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        teamName = readString(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        writeString(buf, teamName);
    }

    private static void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(java.nio.charset.Charset.forName("UTF-8"));
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readString(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.Charset.forName("UTF-8"));
    }

    public static class Handler implements IMessageHandler<PacketTeamSelect, IMessage> {
        @Override
        public IMessage onMessage(PacketTeamSelect msg, MessageContext ctx) {
            EntityPlayer p = ctx.getServerHandler().playerEntity;
            World world = p.worldObj;
            if (world.getTileEntity(msg.x, msg.y, msg.z) instanceof TileEntityTeamTeleporter) {
                TileEntityTeamTeleporter te = (TileEntityTeamTeleporter) world.getTileEntity(msg.x, msg.y, msg.z);
                te.selectedTeam = msg.teamName == null ? "" : msg.teamName;
                te.markDirty();
                world.markBlockForUpdate(msg.x, msg.y, msg.z);
            }
            return null;
        }
    }
}
