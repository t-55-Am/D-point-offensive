package com.myname.mymodid.tileentity;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

/**
 * 战场传送器方块 TE（v1.1.8 新增）：
 * GUI 里选中队伍，整方块收到任意红石信号时——把范围内玩家背包替换为该队伍模板 + 传送到队伍出生点。
 * 边沿触发 + 冷却，避免持续供电反复触发。
 */
public class TileEntityTeamTeleporter extends TileEntity {
    public String selectedTeam = "";
    public boolean enableSwap = true; // 换装开关
    public boolean enableTp = true;   // 传送开关
    private boolean wasPowered = false;
    private int cooldown = 0;

    public void onNeighborChanged() {
        if (worldObj != null && !worldObj.isRemote) checkSignal();
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        checkSignal();
    }

    private void checkSignal() {
        boolean powered = isPowered();
        if (cooldown > 0) cooldown--;
        if (powered && !wasPowered && cooldown <= 0) {
            cooldown = 20;
            activate();
        }
        wasPowered = powered;
    }

    private boolean isPowered() {
        World w = worldObj;
        return w.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)
            || w.getBlockPowerInput(xCoord, yCoord, zCoord) > 0;
    }

    private void activate() {
        try {
            TeamData data = TeamData.get(worldObj);
            if (data == null) return;
            TeamConfig cfg = (selectedTeam == null || selectedTeam.isEmpty()) ? null : data.find(selectedTeam);
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xCoord - 3, yCoord - 3, zCoord - 3, xCoord + 4, yCoord + 4, zCoord + 4);
            @SuppressWarnings("unchecked")
            List<EntityPlayer> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class, aabb);
            if (cfg == null) {
                // v1.1.9：未选择队伍时提示（红石信号本身有反馈，避免"无效果"）
                for (EntityPlayer p : players) {
                    p.addChatMessage(new ChatComponentTranslation("msg.teleporter.noteam"));
                }
                return;
            }
            for (EntityPlayer p : players) {
                // v1.1.12：红石激活 = 加入该队（原生 scoreboard team，占点判定依赖它）
                joinTeam(p, cfg.name);
                if (enableSwap && cfg.backpack != null) copyBackpack(p, cfg.backpack);
                if (enableTp && cfg.hasSpawn) {
                    p.setPositionAndUpdate(cfg.spawnX + 0.5, cfg.spawnY + 0.5, cfg.spawnZ + 0.5);
                    p.addChatMessage(new ChatComponentTranslation("msg.enter"));
                }
            }
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }

    /** v1.1.12：把玩家加入原生 scoreboard team（占点判定 getPlayersTeam 依赖它）。 */
    private void joinTeam(EntityPlayer p, String teamName) {
        try {
            Scoreboard sb = worldObj.getScoreboard();
            ScorePlayerTeam st = sb.getTeam(teamName);
            if (st == null) st = sb.createTeam(teamName);
            String pname = p.getCommandSenderName();
            Team cur = sb.getPlayersTeam(pname);
            if (cur != null && teamName.equals(cur.getRegisteredName())) {
                return; // 已在该队
            }
            if (cur instanceof ScorePlayerTeam) {
                ((ScorePlayerTeam) cur).getMembershipCollection().remove(pname);
            }
            st.getMembershipCollection().add(pname);
            p.addChatMessage(new ChatComponentTranslation("msg.team.joined", teamName));
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }

    private void copyBackpack(EntityPlayer p, ItemStack[] template) {
        for (int i = 0; i < 36; i++) {
            p.inventory.setInventorySlotContents(i, template[i] == null ? null : template[i].copy());
        }
        for (int i = 0; i < 4; i++) {
            p.inventory.setInventorySlotContents(36 + i, template[36 + i] == null ? null : template[36 + i].copy());
        }
        p.inventoryContainer.detectAndSendChanges();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("selectedTeam", selectedTeam == null ? "" : selectedTeam);
        nbt.setBoolean("enableSwap", enableSwap);
        nbt.setBoolean("enableTp", enableTp);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        selectedTeam = nbt.hasKey("selectedTeam") ? nbt.getString("selectedTeam") : "";
        enableSwap = !nbt.hasKey("enableSwap") || nbt.getBoolean("enableSwap");
        enableTp = !nbt.hasKey("enableTp") || nbt.getBoolean("enableTp");
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
