package com.myname.mymodid.network;

import java.util.Map;

import com.myname.mymodid.score.ScoreData;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

/**
 * v1.1.11 同步：多队伍模式每 tick 分数同步（队伍名 + 分数）。
 */
public class PacketTeamScores implements IMessage {
    public int mode;
    public String[] names;
    public int[] scores;

    public PacketTeamScores() {}

    public PacketTeamScores(int mode, Map<String, Integer> map) {
        this.mode = mode;
        this.names = new String[map.size()];
        this.scores = new int[map.size()];
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            this.names[i] = e.getKey();
            this.scores[i] = e.getValue() == null ? 0 : e.getValue();
            i++;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readInt();
        int n = buf.readInt();
        names = new String[n];
        scores = new int[n];
        for (int i = 0; i < n; i++) {
            names[i] = readString(buf);
            scores[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mode);
        buf.writeInt(names == null ? 0 : names.length);
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                writeString(buf, names[i] == null ? "" : names[i]);
                buf.writeInt(scores[i]);
            }
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

    public static class Handler implements IMessageHandler<PacketTeamScores, IMessage> {
        @Override
        public IMessage onMessage(PacketTeamScores message, MessageContext ctx) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.world != null) {
                    ScoreData client = ScoreData.get(mc.world);
                    if (client != null) {
                        client.mode = message.mode;
                        client.teamScores.clear();
                        if (message.names != null) {
                            for (int i = 0; i < message.names.length; i++) {
                                client.teamScores.put(message.names[i], message.scores[i]);
                            }
                        }
                    }
                }
            } catch (Exception e) {}
            return null;
        }
    }
}
