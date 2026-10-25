package com.myname.mymodid.network;

import java.nio.charset.StandardCharsets;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketCapturePointConfig implements IMessage {
    public int x, y, z;
    public int radius;
    public int captureTime;
    public String name = "";
    public int pveInterval = 20;
    public int pveCount = 3;
    public int pveMin = 0;
    public int pveMax = 20;

    public PacketCapturePointConfig() {}

    // 旧构造器（兼容）
    public PacketCapturePointConfig(int x, int y, int z, int r, int t, String name) {
        this.x = x; this.y = y; this.z = z;
        this.radius = r; this.captureTime = t;
        this.name = name == null ? "" : name;
    }

    // 新构造器（带PvE参数）
    public PacketCapturePointConfig(int x, int y, int z, int r, int t, String name, int pi, int pc, int pmin, int pmax) {
        this.x = x; this.y = y; this.z = z;
        this.radius = r; this.captureTime = t;
        this.name = name == null ? "" : name;
        this.pveInterval = pi; this.pveCount = pc;
        this.pveMin = pmin; this.pveMax = pmax;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        radius = buf.readInt();
        captureTime = buf.readInt();
        int len = buf.readInt();
        byte[] bs = new byte[len];
        buf.readBytes(bs);
        name = new String(bs, StandardCharsets.UTF_8);
        pveInterval = buf.readInt();
        pveCount = buf.readInt();
        pveMin = buf.readInt();
        pveMax = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(radius);
        buf.writeInt(captureTime);
        byte[] bs = name.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bs.length);
        buf.writeBytes(bs);
        buf.writeInt(pveInterval);
        buf.writeInt(pveCount);
        buf.writeInt(pveMin);
        buf.writeInt(pveMax);
    }

    public static class Handler implements IMessageHandler<PacketCapturePointConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketCapturePointConfig m, MessageContext ctx) {
            try {
                EntityPlayerMP player = ctx.getServerHandler().player;
                World world = player.world;
                TileEntity te = world.getTileEntity(new net.minecraft.util.math.BlockPos(m.x, m.y, m.z));
                if (te instanceof TileEntityCapturePoint) {
                    TileEntityCapturePoint p = (TileEntityCapturePoint) te;
                    p.setRadius(m.radius);
                    p.setCaptureTime(m.captureTime);
                    p.setPointName(m.name);
                    p.setPveSpawnInterval(m.pveInterval);
                    p.setPveSpawnCount(m.pveCount);
                    p.setPveMinCount(m.pveMin);
                    p.setPveMaxCount(m.pveMax);
                    world.notifyBlockUpdate(new net.minecraft.util.math.BlockPos(m.x, m.y, m.z), world.getBlockState(new net.minecraft.util.math.BlockPos(m.x, m.y, m.z)), world.getBlockState(new net.minecraft.util.math.BlockPos(m.x, m.y, m.z)), 3);
                }
            } catch (Exception e) {
                // 吞掉异常，不让配置包导致崩溃
            }
            return null;
        }
    }
}
