package com.myname.mymodid.tileentity;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

/**
 * 战场传送器方块 TE（1.7.10 v1.1.8 起新增，1.12.2 同步）：
 * GUI 里选中队伍，整方块收到任意红石信号时——把范围内玩家背包替换为该队伍模板 + 传送到队伍出生点。
 * 边沿触发 + 冷却，避免持续供电反复触发。
 */
public class TileEntityTeamTeleporter extends TileEntity implements ITickable {
    public String selectedTeam = "";
    public boolean enableSwap = true; // 换装开关
    public boolean enableTp = true;   // 传送开关
    private boolean wasPowered = false;
    private int cooldown = 0;

    public void onNeighborChanged() {
        if (world != null && !world.isRemote) checkSignal();
    }

    @Override
    public void update() { // ITickable：1.12.2 TE 每 tick 回调
        if (world == null || world.isRemote) return;
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
        World w = world;
        return w.isBlockPowered(pos) || w.getStrongPower(pos) > 0;
    }

    private void activate() {
        try {
            TeamData data = TeamData.get(world);
            if (data == null) return;
            TeamConfig cfg = (selectedTeam == null || selectedTeam.isEmpty()) ? null : data.find(selectedTeam);
            AxisAlignedBB aabb = new AxisAlignedBB(pos.getX() - 3, pos.getY() - 3, pos.getZ() - 3,
                                                   pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
            List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, aabb);
            if (cfg == null) {
                // v1.1.9：未选择队伍时提示（红石信号本身有反馈，避免"无效果"）
                for (EntityPlayer p : players) {
                    p.sendMessage(new TextComponentTranslation("msg.teleporter.noteam"));
                }
                return;
            }
            for (EntityPlayer p : players) {
                // v1.1.12 同步：红石激活 = 加入该队（原生 scoreboard team，占点判定依赖它）
                joinTeam(p, cfg.name);
                if (enableSwap && cfg.backpack != null) copyBackpack(p, cfg.backpack);
                if (enableTp && cfg.hasSpawn) {
                    p.setPositionAndUpdate(cfg.spawnX + 0.5, cfg.spawnY + 0.5, cfg.spawnZ + 0.5);
                    p.sendMessage(new TextComponentTranslation("msg.enter"));
                }
            }
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }

    /** v1.1.12 同步：把玩家加入原生 scoreboard team（占点判定 getPlayersTeam 依赖它）。 */
    private void joinTeam(EntityPlayer p, String teamName) {
        try {
            Scoreboard sb = world.getScoreboard();
            ScorePlayerTeam st = sb.getTeam(teamName);
            if (st == null) st = sb.createTeam(teamName);
            String pname = p.getName();
            Team cur = sb.getPlayersTeam(pname);
            if (cur != null && teamName.equals(cur.getName())) {
                return; // 已在该队
            }
            if (cur instanceof ScorePlayerTeam) {
                ((ScorePlayerTeam) cur).getMembershipCollection().remove(pname);
            }
            st.getMembershipCollection().add(pname);
            p.sendMessage(new TextComponentTranslation("msg.team.joined", teamName));
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }

    private void copyBackpack(EntityPlayer p, ItemStack[] template) {
        for (int i = 0; i < 36; i++) {
            p.inventory.setInventorySlotContents(i, template[i] == null ? ItemStack.EMPTY : template[i].copy());
        }
        for (int i = 0; i < 4; i++) {
            p.inventory.setInventorySlotContents(36 + i, template[36 + i] == null ? ItemStack.EMPTY : template[36 + i].copy());
        }
        p.inventoryContainer.detectAndSendChanges();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("selectedTeam", selectedTeam == null ? "" : selectedTeam);
        nbt.setBoolean("enableSwap", enableSwap);
        nbt.setBoolean("enableTp", enableTp);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        selectedTeam = nbt.hasKey("selectedTeam") ? nbt.getString("selectedTeam") : "";
        enableSwap = !nbt.hasKey("enableSwap") || nbt.getBoolean("enableSwap");
        enableTp = !nbt.hasKey("enableTp") || nbt.getBoolean("enableTp");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
