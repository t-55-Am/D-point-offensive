package com.myname.mymodid.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.score.TeamData;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** 保存队伍背包模板（服务端）：把发送者当前背包 40 格存为该队伍模板 */
public class PacketSaveBackpack implements IMessage {
    public String teamName;

    public PacketSaveBackpack() {}

    public PacketSaveBackpack(String teamName) {
        this.teamName = teamName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        teamName = readString(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeString(buf, teamName == null ? "" : teamName);
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

    public static class Handler implements IMessageHandler<PacketSaveBackpack, IMessage> {
        @Override
        public IMessage onMessage(PacketSaveBackpack msg, MessageContext ctx) {
            EntityPlayer p = ctx.getServerHandler().player;
            World world = p.world;
            try {
                TeamData data = TeamData.get(world);
                if (data == null || msg.teamName == null || msg.teamName.isEmpty()) return null;
                ItemStack[] template = new ItemStack[40];
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = p.inventory.mainInventory.get(i);
                    template[i] = (stack == null || stack.isEmpty()) ? null : stack.copy();
                }
                for (int i = 0; i < 4; i++) {
                    ItemStack stack = p.inventory.armorInventory.get(i);
                    template[36 + i] = (stack == null || stack.isEmpty()) ? null : stack.copy();
                }
                data.saveBackpack(msg.teamName, template);
                data.markDirty();
                MyMod.NETWORK.sendToAll(new PacketTeamSync(data));
            } catch (Exception e) {
                // 绝不崩游戏
            }
            return null;
        }
    }
}
