package com.myname.mymodid.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketSetSpawn implements IMessage {
    public int x, y, z;
    public int teamIndex;
    public int sx, sy, sz;

    public PacketSetSpawn() {}
    public PacketSetSpawn(int x, int y, int z, int teamIndex, int sx, int sy, int sz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.teamIndex = teamIndex;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        teamIndex = buf.readInt();
        sx = buf.readInt();
        sy = buf.readInt();
        sz = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(teamIndex);
        buf.writeInt(sx);
        buf.writeInt(sy);
        buf.writeInt(sz);
    }

    public static class Handler implements IMessageHandler<PacketSetSpawn, IMessage> {
        @Override
        public IMessage onMessage(PacketSetSpawn msg, MessageContext ctx) {
            EntityPlayer p = ctx.getServerHandler().player;
            World world = p.world;
            if (world.getTileEntity(new net.minecraft.util.math.BlockPos(msg.x, msg.y, msg.z)) instanceof TileEntityBattlePortal) {
                TileEntityBattlePortal portal = (TileEntityBattlePortal) world.getTileEntity(new net.minecraft.util.math.BlockPos(msg.x, msg.y, msg.z));
                if (msg.teamIndex == 0) {
                    portal.redSpawnX = msg.sx;
                    portal.redSpawnY = msg.sy;
                    portal.redSpawnZ = msg.sz;
                    portal.redHasSpawn = true;
                } else {
                    portal.blueSpawnX = msg.sx;
                    portal.blueSpawnY = msg.sy;
                    portal.blueSpawnZ = msg.sz;
                    portal.blueHasSpawn = true;
                }
                portal.markDirty();
                world.notifyBlockUpdate(new net.minecraft.util.math.BlockPos(msg.x, msg.y, msg.z), world.getBlockState(new net.minecraft.util.math.BlockPos(msg.x, msg.y, msg.z)), world.getBlockState(new net.minecraft.util.math.BlockPos(msg.x, msg.y, msg.z)), 3);
            }
            return null;
        }
    }
}
