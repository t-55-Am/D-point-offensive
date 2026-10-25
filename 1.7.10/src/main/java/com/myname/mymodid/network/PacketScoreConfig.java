package com.myname.mymodid.network;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.score.ScoreTickHandler;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class PacketScoreConfig implements IMessage {
    public int action;
    public int initialScore;
    public int scorePerSecond;
    public boolean togglePve;
    public double zombieAttack;
    public double zombieSpeed;
    public double zombieHealth;

    public PacketScoreConfig() {}

    public PacketScoreConfig(int initialScore, int scorePerSecond) {
        this.action = 0;
        this.initialScore = initialScore;
        this.scorePerSecond = scorePerSecond;
    }

    public PacketScoreConfig(boolean reset) {
        this.action = 1;
    }

    public PacketScoreConfig(boolean togglePve, boolean dummy) {
        this.action = 2;
        this.togglePve = togglePve;
    }

    public PacketScoreConfig(int action, double attack, double speed, double health) {
        this.action = action;
        this.zombieAttack = attack;
        this.zombieSpeed = speed;
        this.zombieHealth = health;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        if (action == 0) {
            initialScore = buf.readInt();
            scorePerSecond = buf.readInt();
        } else if (action == 2) {
            togglePve = buf.readBoolean();
        } else if (action == 3 || action == 4) {
            zombieAttack = buf.readDouble();
            zombieSpeed = buf.readDouble();
            zombieHealth = buf.readDouble();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        if (action == 0) {
            buf.writeInt(initialScore);
            buf.writeInt(scorePerSecond);
        } else if (action == 2) {
            buf.writeBoolean(togglePve);
        } else if (action == 3 || action == 4) {
            buf.writeDouble(zombieAttack);
            buf.writeDouble(zombieSpeed);
            buf.writeDouble(zombieHealth);
        }
    }

    public static class Handler implements IMessageHandler<PacketScoreConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketScoreConfig message, MessageContext ctx) {
            try {
                if (ctx.getServerHandler() == null || ctx.getServerHandler().playerEntity == null) return null;
                World world = ctx.getServerHandler().playerEntity.worldObj;
                if (world == null) return null;
                final ScoreData data = ScoreData.get(world);
                if (data == null) return null;

                if (message.action == 1) {
                    data.reset();
                    ScoreTickHandler.resetScoreboard(world);
                    for (Object te : world.loadedTileEntityList) {
                        if (te instanceof TileEntityCapturePoint) {
                            ((TileEntityCapturePoint) te).resetToNeutral();
                        }
                    }
                    ScoreTickHandler.syncAllStats(world);
                    world.mapStorage.saveAllData();
                } else if (message.action == 0) {
                    if (message.initialScore <= 0) message.initialScore = 10000;
                    if (message.scorePerSecond <= 0) message.scorePerSecond = 10;
                    data.initialScore = message.initialScore;
                    data.scorePerSecond = message.scorePerSecond;
                    data.markDirty();
                    world.mapStorage.saveAllData();
                } else if (message.action == 2) {
                    data.pveEnabled = !data.pveEnabled;
                    data.markDirty();
                    world.mapStorage.saveAllData();
                } else if (message.action == 3) {
                    if (message.zombieAttack < 0) message.zombieAttack = 0;
                    if (message.zombieSpeed < 0.5) message.zombieSpeed = 0.5;
                    if (message.zombieHealth < 1) message.zombieHealth = 1;
                    data.zombieAttack = message.zombieAttack;
                    data.zombieSpeed = message.zombieSpeed / 15.0;
                    data.zombieHealth = message.zombieHealth;
                    data.markDirty();
                    world.mapStorage.saveAllData();
                } else if (message.action == 4) {
                    // 开始/结束对局
                    data.gameStarted = !data.gameStarted;
                    data.markDirty();
                    world.mapStorage.saveAllData();
                    // 结束对局时重置据点状态
                    if (!data.gameStarted) {
                        for (Object te : world.loadedTileEntityList) {
                            if (te instanceof TileEntityCapturePoint) {
                                ((TileEntityCapturePoint) te).resetToNeutral();
                            }
                        }
                    }
                } else if (message.action == 5) {
                    // v1.1.11：切换对抗模式（0=双队对抗 1=多队伍）
                    data.mode = data.mode == 0 ? 1 : 0;
                    data.teamScores.clear();
                    data.markDirty();
                    world.mapStorage.saveAllData();
                }

                try {
                    for (Object obj : world.playerEntities) {
                        if (obj instanceof EntityPlayerMP) {
                            EntityPlayerMP pl = (EntityPlayerMP) obj;
                            MyMod.NETWORK.sendTo(new PacketScoreSync(data), pl);
                        }
                    }
                } catch (Exception e) {}
            } catch (Exception e) {}
            return null;
        }
    }
}
