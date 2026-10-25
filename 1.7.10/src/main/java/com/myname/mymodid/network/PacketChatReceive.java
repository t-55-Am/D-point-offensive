package com.myname.mymodid.network;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import java.nio.charset.StandardCharsets;
import com.myname.mymodid.client.SoundPlayer;

public class PacketChatReceive implements IMessage {
    public String formatted = "";
    public String raw = "";

    public PacketChatReceive() {}
    public PacketChatReceive(String formatted, String raw) {
        this.formatted = formatted == null ? "" : formatted;
        this.raw = raw == null ? "" : raw;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int l1 = buf.readInt(); byte[] b1 = new byte[l1]; buf.readBytes(b1);
        formatted = new String(b1, StandardCharsets.UTF_8);
        int l2 = buf.readInt(); byte[] b2 = new byte[l2]; buf.readBytes(b2);
        raw = new String(b2, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] b1 = formatted.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(b1.length); buf.writeBytes(b1);
        byte[] b2 = raw.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(b2.length); buf.writeBytes(b2);
    }

    public static class Handler implements IMessageHandler<PacketChatReceive, IMessage> {
        @Override
        public IMessage onMessage(PacketChatReceive msg, MessageContext ctx) {
            if (ctx.side.isClient()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null) {
                    mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(msg.formatted));
                    com.myname.mymodid.client.ClientEventHandler.chatHistory.add(msg.formatted);
                    if (com.myname.mymodid.client.ClientEventHandler.chatHistory.size() > com.myname.mymodid.client.ClientEventHandler.MAX_CHAT_HISTORY)
                        com.myname.mymodid.client.ClientEventHandler.chatHistory.remove(0);
                    if (!msg.raw.isEmpty()) SoundPlayer.play(msg.raw);
                }
            }
            return null;
        }
    }
}
