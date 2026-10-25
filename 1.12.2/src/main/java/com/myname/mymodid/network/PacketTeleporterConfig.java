package com.myname.mymodid.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 传送方块：客户端切换换装/传送开关 → 服务端存入 TE NBT（v1.1.10 同步）。
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
            EntityPlayer p = ctx.getServerHandler().player;
            World world = p.world;
            BlockPos pos = new BlockPos(msg.x, msg.y, msg.z);
            if (world.getTileEntity(pos) instanceof TileEntityTeamTeleporter) {
                TileEntityTeamTeleporter te = (TileEntityTeamTeleporter) world.getTileEntity(pos);
                te.enableSwap = msg.swap;
                te.enableTp = msg.tp;
                te.markDirty();
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
            return null;
        }
    }
}
