package com.myname.mymodid.network;

import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;

import com.myname.mymodid.score.CapturePointData;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketCapturePointSync implements IMessage {
    public int x, y, z;
    public String name = "";
    public String owner = "";
    public String capturing = "";
    public double progress;
    public int radius;

    public PacketCapturePointSync() {}

    public PacketCapturePointSync(int x, int y, int z, String name, String owner, String capturing, double progress,
        int radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name == null ? "" : name;
        this.owner = owner == null ? "" : owner;
        this.capturing = capturing == null ? "" : capturing;
        this.progress = progress;
        this.radius = radius;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        name = readString(buf);
        owner = readString(buf);
        capturing = readString(buf);
        progress = buf.readDouble();
        radius = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        writeString(buf, name);
        writeString(buf, owner);
        writeString(buf, capturing);
        buf.writeDouble(progress);
        buf.writeInt(radius);
    }

    private static void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readString(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static class Handler implements IMessageHandler<PacketCapturePointSync, IMessage> {
        @Override
        public IMessage onMessage(PacketCapturePointSync message, MessageContext ctx) {
            if (ctx.side.isClient()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.world != null) {
                    CapturePointData data = CapturePointData.get(mc.world);
                    data.update(message.x, message.y, message.z, message.name,
                        message.owner.isEmpty() ? null : message.owner,
                        message.capturing.isEmpty() ? null : message.capturing,
                        message.progress, message.radius);
                }
            }
            return null;
        }
    }
}
