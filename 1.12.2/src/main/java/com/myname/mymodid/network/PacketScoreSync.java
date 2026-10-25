package com.myname.mymodid.network;

import java.util.Map;

import com.myname.mymodid.score.ScoreData;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

public class PacketScoreSync implements IMessage {
    public boolean pveEnabled;
    public boolean gameStarted;
    public int redScore;
    public int blueScore;
    public int initialScore;
    public int scorePerSecond;
    public int mode;
    public String[] teamNames;
    public int[] teamScoreVals;
    public double zombieAttack;
    public double zombieSpeed;
    public double zombieHealth;

    public PacketScoreSync() {}

    public PacketScoreSync(ScoreData data) {
        this.pveEnabled = data.pveEnabled;
        this.gameStarted = data.gameStarted;
        this.redScore = data.redScore;
        this.blueScore = data.blueScore;
        this.initialScore = data.initialScore;
        this.scorePerSecond = data.scorePerSecond;
        this.mode = data.mode;
        this.teamNames = new String[data.teamScores.size()];
        this.teamScoreVals = new int[data.teamScores.size()];
        int i = 0;
        for (Map.Entry<String, Integer> e : data.teamScores.entrySet()) {
            this.teamNames[i] = e.getKey();
            this.teamScoreVals[i] = e.getValue() == null ? 0 : e.getValue();
            i++;
        }
        this.zombieAttack = data.zombieAttack;
        this.zombieSpeed = data.zombieSpeed;
        this.zombieHealth = data.zombieHealth;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pveEnabled = buf.readBoolean();
        gameStarted = buf.readBoolean();
        redScore = buf.readInt();
        blueScore = buf.readInt();
        initialScore = buf.readInt();
        scorePerSecond = buf.readInt();
        mode = buf.readInt();
        int n = buf.readInt();
        teamNames = new String[n];
        teamScoreVals = new int[n];
        for (int i = 0; i < n; i++) {
            teamNames[i] = readString(buf);
            teamScoreVals[i] = buf.readInt();
        }
        zombieAttack = buf.readDouble();
        zombieSpeed = buf.readDouble();
        zombieHealth = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(pveEnabled);
        buf.writeBoolean(gameStarted);
        buf.writeInt(redScore);
        buf.writeInt(blueScore);
        buf.writeInt(initialScore);
        buf.writeInt(scorePerSecond);
        buf.writeInt(mode);
        buf.writeInt(teamNames == null ? 0 : teamNames.length);
        if (teamNames != null) {
            for (int i = 0; i < teamNames.length; i++) {
                writeString(buf, teamNames[i] == null ? "" : teamNames[i]);
                buf.writeInt(teamScoreVals[i]);
            }
        }
        buf.writeDouble(zombieAttack);
        buf.writeDouble(zombieSpeed);
        buf.writeDouble(zombieHealth);
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

    public static class Handler implements IMessageHandler<PacketScoreSync, IMessage> {
        @Override
        public IMessage onMessage(PacketScoreSync message, MessageContext ctx) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.world != null) {
                    ScoreData client = ScoreData.get(mc.world);
                    if (client != null) {
                        client.pveEnabled = message.pveEnabled;
                        client.gameStarted = message.gameStarted;
                        client.redScore = message.redScore;
                        client.blueScore = message.blueScore;
                        client.initialScore = message.initialScore;
                        client.scorePerSecond = message.scorePerSecond;
                        client.mode = message.mode;
                        client.teamScores.clear();
                        if (message.teamNames != null) {
                            for (int i = 0; i < message.teamNames.length; i++) {
                                client.teamScores.put(message.teamNames[i], message.teamScoreVals[i]);
                            }
                        }
                        client.zombieAttack = message.zombieAttack;
                        client.zombieSpeed = message.zombieSpeed;
                        client.zombieHealth = message.zombieHealth;
                        // v1.1.13 同步：进服/配置变更时同步 HUD 静态分数
                        com.myname.mymodid.client.ClientEventHandler.redScore = message.redScore;
                        com.myname.mymodid.client.ClientEventHandler.blueScore = message.blueScore;
                        com.myname.mymodid.client.ClientEventHandler.mode = message.mode;
                    }
                }
            } catch (Exception e) {}
            return null;
        }
    }
}
