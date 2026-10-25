package com.myname.mymodid.network;

import java.util.ArrayList;
import java.util.List;

import com.myname.mymodid.client.gui.GuiBattlePortal;
import com.myname.mymodid.client.gui.GuiTeamTeleporter;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** 全量队伍列表同步（服务端 → 客户端；不含背包模板，避免包过大） */
public class PacketTeamSync implements IMessage {
    public List<TeamConfig> teams = new ArrayList<TeamConfig>();

    public PacketTeamSync() {}

    public PacketTeamSync(TeamData data) {
        for (TeamConfig t : data.teams) {
            TeamConfig c = new TeamConfig(t.name, t.color, t.side);
            c.maxPlayers = t.maxPlayers;
            c.spawnX = t.spawnX;
            c.spawnY = t.spawnY;
            c.spawnZ = t.spawnZ;
            c.hasSpawn = t.hasSpawn;
            teams.add(c);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        teams.clear();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            TeamConfig c = new TeamConfig();
            c.name = readString(buf);
            c.color = buf.readInt();
            c.side = buf.readInt();
            c.maxPlayers = buf.readInt();
            c.spawnX = buf.readInt();
            c.spawnY = buf.readInt();
            c.spawnZ = buf.readInt();
            c.hasSpawn = buf.readBoolean();
            teams.add(c);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(teams.size());
        for (TeamConfig c : teams) {
            writeString(buf, c.name);
            buf.writeInt(c.color);
            buf.writeInt(c.side);
            buf.writeInt(c.maxPlayers);
            buf.writeInt(c.spawnX);
            buf.writeInt(c.spawnY);
            buf.writeInt(c.spawnZ);
            buf.writeBoolean(c.hasSpawn);
        }
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

    public static class Handler implements IMessageHandler<PacketTeamSync, IMessage> {
        @Override
        public IMessage onMessage(PacketTeamSync msg, MessageContext ctx) {
            TeamData.clientSet(msg.teams);
            // 若队伍配置器/传送器 GUI 正开着，同步后刷新界面
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    GuiScreen cur = mc.currentScreen;
                    if (cur instanceof GuiBattlePortal) {
                        ((GuiBattlePortal) cur).onTeamsSynced();
                    } else if (cur instanceof GuiTeamTeleporter) {
                        ((GuiTeamTeleporter) cur).onTeamsSynced();
                    }
                }
            } catch (Exception e) {
                // 绝不崩游戏
            }
            return null;
        }
    }
}
