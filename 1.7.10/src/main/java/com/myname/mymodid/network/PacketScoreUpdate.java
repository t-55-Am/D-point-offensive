package com.myname.mymodid.network;

import com.myname.mymodid.client.ClientEventHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketScoreUpdate implements IMessage {

    public int redScore;
    public int blueScore;
    public int mode;

    public PacketScoreUpdate() {}

    public PacketScoreUpdate(int redScore, int blueScore, int mode) {
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.mode = mode;
    }

    public PacketScoreUpdate(int redScore, int blueScore) {
        this(redScore, blueScore, 0);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        redScore = buf.readInt();
        blueScore = buf.readInt();
        mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(redScore);
        buf.writeInt(blueScore);
        buf.writeInt(mode);
    }

    public static class Handler implements IMessageHandler<PacketScoreUpdate, IMessage> {

        @Override
        public IMessage onMessage(PacketScoreUpdate message, MessageContext ctx) {
            ClientEventHandler.redScore = message.redScore;
            ClientEventHandler.blueScore = message.blueScore;
            ClientEventHandler.mode = message.mode;
            return null;
        }
    }
}
