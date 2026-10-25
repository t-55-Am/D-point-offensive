package com.myname.mymodid.network;

import net.minecraft.client.Minecraft;

import com.myname.mymodid.score.CapturePointData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

// 服务端 → 客户端：通知某个据点已拆除，客户端从注册表移除（浮空名/指南针标记消失）
public class PacketCapturePointSyncRemove implements IMessage {
    public int x, y, z;

    public PacketCapturePointSyncRemove() {}

    public PacketCapturePointSyncRemove(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static class Handler implements IMessageHandler<PacketCapturePointSyncRemove, IMessage> {
        @Override
        public IMessage onMessage(PacketCapturePointSyncRemove m, MessageContext ctx) {
            if (ctx.side.isClient()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld != null) {
                    CapturePointData.get(mc.theWorld).remove(m.x, m.y, m.z);
                }
            }
            return null;
        }
    }
}
