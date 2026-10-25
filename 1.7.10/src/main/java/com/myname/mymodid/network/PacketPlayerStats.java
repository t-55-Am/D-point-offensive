package com.myname.mymodid.network;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.myname.mymodid.client.ClientEventHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

// 服务端 -> 客户端：玩家统计广播（名称 -> {击杀,助攻,占点,死亡}），供 Tab 面板显示；pveEnabled 用于切换显示格式
public class PacketPlayerStats implements IMessage {
    public Map<String, int[]> stats = new HashMap<String, int[]>();
    public boolean pveEnabled = false;

    public PacketPlayerStats() {}

    public PacketPlayerStats(Map<String, int[]> stats, boolean pveEnabled) {
        this.stats = stats;
        this.pveEnabled = pveEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        stats = new HashMap<String, int[]>();
        for (int i = 0; i < count; i++) {
            String name = readString(buf);
            int kill = buf.readInt();
            int assist = buf.readInt();
            int cap = buf.readInt();
            int death = buf.readInt();
            stats.put(name, new int[]{kill, assist, cap, death});
        }
        pveEnabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(stats.size());
        for (Map.Entry<String, int[]> e : stats.entrySet()) {
            writeString(buf, e.getKey());
            int[] s = e.getValue();
            buf.writeInt(s[0]);
            buf.writeInt(s[1]);
            buf.writeInt(s[2]);
            buf.writeInt(s[3]);
        }
        buf.writeBoolean(pveEnabled);
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

    public static class Handler implements IMessageHandler<PacketPlayerStats, IMessage> {
        @Override
        public IMessage onMessage(PacketPlayerStats message, MessageContext ctx) {
            if (ctx.side.isClient()) {
                // 1.7.10 没有 addScheduledTask，直接写静态字段
                ClientEventHandler.playerStats = message.stats;
                ClientEventHandler.pveEnabled = message.pveEnabled;
            }
            return null;
        }
    }
}
