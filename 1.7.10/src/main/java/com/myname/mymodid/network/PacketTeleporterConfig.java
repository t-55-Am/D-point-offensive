package com.myname.mymodid.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 传送方块：客户端切换换装/传送开关 → 服务端存入 TE NBT（v1.1.10）。
 */
public class PacketTeleporterConfig implements IMessage {
    public int x, y, z;
    public boolean swap;
    public boolean tp;

    public PacketTeleporterConfig() {}

    public PacketTeleporterConfig(int x, int y, int z, boolean swap, boolean tp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.swap = swap;
        this.tp = tp;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        swap = buf.readBoolean();
        tp = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(swap);
        buf.writeBoolean(tp);
    }

    public static class Handler implements IMessageHandler<PacketTeleporterConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketTeleporterConfig msg, MessageContext ctx) {
            EntityPlayer p = ctx.getServerHandler().playerEntity;
            World world = p.worldObj;
            if (world.getTileEntity(msg.x, msg.y, msg.z) instanceof TileEntityTeamTeleporter) {
                TileEntityTeamTeleporter te = (TileEntityTeamTeleporter) world.getTileEntity(msg.x, msg.y, msg.z);
                te.enableSwap = msg.swap;
                te.enableTp = msg.tp;
                te.markDirty();
                world.markBlockForUpdate(msg.x, msg.y, msg.z);
            }
            return null;
        }
    }
}
