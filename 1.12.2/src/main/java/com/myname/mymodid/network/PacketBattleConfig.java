package com.myname.mymodid.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.score.TeamData;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 队伍配置操作（服务端）：action 0=保存配置 1=添加队伍 2=删除队伍。
 * 操作后向全体玩家广播 PacketTeamSync，保证环/罗盘颜色与 GUI 一致。
 */
public class PacketBattleConfig implements IMessage {
    public int action;
    public String teamName;
    public String newName;
    public int color, side, maxPlayers, sx, sy, sz;
    public boolean hasSpawn;

    public PacketBattleConfig() {}

    public PacketBattleConfig(int action, String teamName, String newName, int color, int side,
                              int maxPlayers, int sx, int sy, int sz, boolean hasSpawn) {
        this.action = action;
        this.teamName = teamName;
        this.newName = newName;
        this.color = color;
        this.side = side;
        this.maxPlayers = maxPlayers;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.hasSpawn = hasSpawn;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        teamName = readString(buf);
        newName = readString(buf);
        color = buf.readInt();
        side = buf.readInt();
        maxPlayers = buf.readInt();
        sx = buf.readInt();
        sy = buf.readInt();
        sz = buf.readInt();
        hasSpawn = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        writeString(buf, teamName == null ? "" : teamName);
        writeString(buf, newName == null ? "" : newName);
        buf.writeInt(color);
        buf.writeInt(side);
        buf.writeInt(maxPlayers);
        buf.writeInt(sx);
        buf.writeInt(sy);
        buf.writeInt(sz);
        buf.writeBoolean(hasSpawn);
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

    public static class Handler implements IMessageHandler<PacketBattleConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketBattleConfig msg, MessageContext ctx) {
            EntityPlayer p = ctx.getServerHandler().player;
            World world = p.world;
            try {
                TeamData data = TeamData.get(world);
                if (data == null) return null;
                boolean changed = false;
                if (msg.action == 1) {
                    // 添加
                    if (data.addTeam(msg.teamName, msg.color)) {
                        p.sendMessage(new TextComponentString(
                            "\u00a7a" + String.format(net.minecraft.client.resources.I18n.format("msg.team.added"), msg.teamName)));
                        changed = true;
                    } else if (msg.teamName == null || msg.teamName.trim().isEmpty()) {
                        p.sendMessage(new TextComponentString("\u00a7c" + net.minecraft.client.resources.I18n.format("msg.team.empty")));
                    } else if (data.find(msg.teamName) != null) {
                        p.sendMessage(new TextComponentString("\u00a7c" + String.format(net.minecraft.client.resources.I18n.format("msg.team.exists"), msg.teamName)));
                    } else {
                        p.sendMessage(new TextComponentString("\u00a7c" + net.minecraft.client.resources.I18n.format("msg.team.limit")));
                    }
                } else if (msg.action == 2) {
                    if (data.removeTeam(msg.teamName)) {
                        p.sendMessage(new TextComponentString(
                            "\u00a7e" + String.format(net.minecraft.client.resources.I18n.format("msg.team.removed"), msg.teamName)));
                        changed = true;
                    }
                } else {
                    changed = data.updateTeam(msg.teamName, msg.newName, msg.color, msg.side,
                        msg.maxPlayers, msg.sx, msg.sy, msg.sz, msg.hasSpawn);
                }
                if (changed) {
                    data.markDirty();
                    MyMod.NETWORK.sendToAll(new PacketTeamSync(data));
                }
            } catch (Exception e) {
                // 绝不崩游戏
            }
            return null;
        }
    }
}
