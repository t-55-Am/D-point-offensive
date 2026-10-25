package com.myname.mymodid.network;

import com.myname.mymodid.client.ClientEventHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketMarkSync implements IMessage {
    public int x, y, z;
    public float hitX, hitY, hitZ;
    public String playerName;
    public boolean remove;

    public PacketMarkSync() {}

    public PacketMarkSync(int x, int y, int z, float hitX, float hitY, float hitZ, String playerName, boolean remove) {
        this.x = x; this.y = y; this.z = z;
        this.hitX = hitX; this.hitY = hitY; this.hitZ = hitZ;
        this.playerName = playerName;
        this.remove = remove;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt();
        hitX = buf.readFloat(); hitY = buf.readFloat(); hitZ = buf.readFloat();
        remove = buf.readBoolean();
        if (!remove) {
            int nl = buf.readInt();
            byte[] nb = new byte[nl];
            buf.readBytes(nb);
            playerName = new String(nb);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z);
        buf.writeFloat(hitX); buf.writeFloat(hitY); buf.writeFloat(hitZ);
        buf.writeBoolean(remove);
        if (!remove && playerName != null) {
            byte[] nb = playerName.getBytes();
            buf.writeInt(nb.length);
            buf.writeBytes(nb);
        }
    }

    public static class Handler implements IMessageHandler<PacketMarkSync, IMessage> {
        @Override
        public IMessage onMessage(PacketMarkSync message, MessageContext ctx) {
            if (message.remove) {
                ClientEventHandler.removeMark(message.x, message.y, message.z);
            } else {
                ClientEventHandler.addMark(message.x, message.y, message.z,
                    message.hitX, message.hitY, message.hitZ, message.playerName);
            }
            return null;
        }
    }
}
