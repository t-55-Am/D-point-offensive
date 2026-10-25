package com.myname.mymodid.network;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.score.TeamData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** 客户端请求全量队伍列表（GUI 打开 / 客户端连接服务器时发送） */
public class PacketTeamRequest implements IMessage {

    public PacketTeamRequest() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketTeamRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketTeamRequest msg, MessageContext ctx) {
            try {
                TeamData data = TeamData.get(ctx.getServerHandler().playerEntity.worldObj);
                if (data != null) {
                    MyMod.NETWORK.sendTo(new PacketTeamSync(data), ctx.getServerHandler().playerEntity);
                }
            } catch (Exception e) {
                // 绝不崩游戏
            }
            return null;
        }
    }
}
