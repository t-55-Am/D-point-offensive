package com.myname.mymodid.network;

import net.minecraft.world.World;

import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketCapturePointFlags implements IMessage {
    public int x, y, z;
    public boolean setBase, baseVal, setRevive, reviveVal;
    public boolean ownerCycle;

    public PacketCapturePointFlags() {}

    public PacketCapturePointFlags(int x, int y, int z, boolean setBase, boolean baseVal, boolean setRevive, boolean reviveVal) {
        this.x = x; this.y = y; this.z = z;
        this.setBase = setBase; this.baseVal = baseVal;
        this.setRevive = setRevive; this.reviveVal = reviveVal;
    }

    public PacketCapturePointFlags(int x, int y, int z) {
        this.x = x; this.y = y; this.z = z;
        this.ownerCycle = true;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        setBase = buf.readBoolean();
        baseVal = buf.readBoolean();
        setRevive = buf.readBoolean();
        reviveVal = buf.readBoolean();
        ownerCycle = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(setBase);
        buf.writeBoolean(baseVal);
        buf.writeBoolean(setRevive);
        buf.writeBoolean(reviveVal);
        buf.writeBoolean(ownerCycle);
    }

    public static class Handler implements IMessageHandler<PacketCapturePointFlags, IMessage> {
        @Override
        public IMessage onMessage(PacketCapturePointFlags msg, MessageContext ctx) {
            try {
                World world = ctx.getServerHandler().playerEntity.worldObj;
                TileEntityCapturePoint te = (TileEntityCapturePoint) world.getTileEntity(msg.x, msg.y, msg.z);
                if (te == null) return null;

                if (msg.setBase) te.setMainBase(msg.baseVal);
                if (msg.setRevive) te.setReviveMode(msg.reviveVal);

                if (msg.ownerCycle && te.isMainBase()) {
                    // 只有大基地才能切换归属
                    boolean pve = ScoreData.get(world).pveEnabled;
                    String cur = te.getOwnerTeam();
                    String newOwner;
                    if (pve) {
                        newOwner = "PVEHUMAN".equals(cur) ? "PvE" : "PVEHUMAN";
                    } else {
                        boolean isRed = cur != null && (cur.toLowerCase().contains("red") || cur.contains("红"));
                        newOwner = isRed ? "Blue" : "Red";
                    }
                    te.setOwnerTeam(newOwner);
                }

                world.markBlockForUpdate(msg.x, msg.y, msg.z);
            } catch (Exception e) {
                // 全链路保护，不让任何异常导致卡死
            }
            return null;
        }
    }
}
