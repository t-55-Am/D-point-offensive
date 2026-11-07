package com.myname.mymodid.tileentity;

import java.util.List;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class TileEntityTeamTeleporter extends BlockEntity {
    public String selectedTeam = "";
    public boolean enableSwap = true;
    public boolean enableTp = true;
    private boolean wasPowered = false;
    private int cooldown = 0;

    public TileEntityTeamTeleporter(BlockPos pos, BlockState state) {
        super(MyMod.TEAM_TELEPORTER_TE.get(), pos, state);
    }

    public void onNeighborChanged() {
        if (level != null && !level.isClientSide) checkSignal();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityTeamTeleporter te) {
        te.checkSignal();
    }

    private void checkSignal() {
        if (level == null || level.isClientSide) return;
        boolean powered = isPowered();
        if (cooldown > 0) cooldown--;
        if (powered && !wasPowered && cooldown <= 0) {
            cooldown = 20;
            activate();
        }
        wasPowered = powered;
    }

    private boolean isPowered() {
        return level.hasNeighborSignal(worldPosition)
            || level.getDirectSignalTo(worldPosition) > 0
            || level.getBestNeighborSignal(worldPosition) > 0;
    }

    private void activate() {
        try {
            if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
            TeamData data = TeamData.get(sl);
            if (data == null) return;
            TeamConfig cfg = (selectedTeam == null || selectedTeam.isEmpty()) ? null : data.find(selectedTeam);
            AABB aabb = new AABB(
                worldPosition.getX() - 3, worldPosition.getY() - 3, worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 4, worldPosition.getZ() + 4);
            List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
            if (cfg == null) {
                for (Player p : players) {
                    p.sendSystemMessage(Component.translatable("msg.teleporter.noteam"));
                }
                return;
            }
            for (Player p : players) {
                joinTeam(p, cfg.name);
                if (enableSwap && cfg.backpack != null) {
                    copyBackpack(p, cfg.backpack);
                    com.myname.mymodid.util.AccessoriesCompat.setAccessories(p, cfg.accessories);
                }
                if (enableTp && cfg.hasSpawn) {
                    if (p instanceof ServerPlayer sp) {
                        sp.teleportTo(cfg.spawnX + 0.5, cfg.spawnY + 0.5, cfg.spawnZ + 0.5);
                    }
                    p.sendSystemMessage(Component.translatable("msg.enter"));
                }
            }
        } catch (Exception e) {
        }
    }

    private void joinTeam(Player p, String teamName) {
        try {
            Scoreboard sb = level.getScoreboard();
            PlayerTeam st = sb.getPlayerTeam(teamName);
            if (st == null) st = sb.addPlayerTeam(teamName);
            String pname = p.getScoreboardName();
            PlayerTeam cur = sb.getPlayersTeam(pname);
            if (cur != null && teamName.equals(cur.getName())) {
                return;
            }
            if (cur != null) {
                sb.removePlayerFromTeam(pname, cur);
            }
            sb.addPlayerToTeam(pname, st);
            p.sendSystemMessage(Component.translatable("msg.team.joined", teamName));
        } catch (Exception e) {
        }
    }

    private void copyBackpack(Player p, ItemStack[] template) {
        try {
            ItemStack[] snap = new ItemStack[41];
            for (int i = 0; i < 36; i++) {
                ItemStack s = p.getInventory().getItem(i);
                snap[i] = s == null || s.isEmpty() ? null : s.copy();
            }
            for (int i = 0; i < 4; i++) {
                ItemStack s = p.getInventory().getItem(36 + i);
                snap[36 + i] = s == null || s.isEmpty() ? null : s.copy();
            }
            ItemStack off = p.getInventory().getItem(40);
            snap[40] = off == null || off.isEmpty() ? null : off.copy();
            java.util.List<ItemStack> snapAcc = com.myname.mymodid.util.AccessoriesCompat.getAccessories(p);
            com.myname.mymodid.score.TeamBackupData.get((net.minecraft.server.level.ServerLevel) level)
                .saveBackup(p.getUUID(), snap, snapAcc);
            p.sendSystemMessage(Component.translatable("msg.backup.saved"));
        } catch (Exception e) {
        }
        for (int i = 0; i < 36; i++) {
            p.getInventory().setItem(i, template[i] == null ? ItemStack.EMPTY : template[i].copy());
        }
        for (int i = 0; i < 4; i++) {
            p.getInventory().setItem(36 + i, template[36 + i] == null ? ItemStack.EMPTY : template[36 + i].copy());
        }
        p.getInventory().setItem(40, template.length > 40 && template[40] != null ? template[40].copy() : ItemStack.EMPTY);
        p.getInventory().setChanged();
        p.containerMenu.broadcastChanges();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putString("selectedTeam", selectedTeam == null ? "" : selectedTeam);
        nbt.putBoolean("enableSwap", enableSwap);
        nbt.putBoolean("enableTp", enableTp);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        selectedTeam = nbt.contains("selectedTeam") ? nbt.getString("selectedTeam") : "";
        enableSwap = !nbt.contains("enableSwap") || nbt.getBoolean("enableSwap");
        enableTp = !nbt.contains("enableTp") || nbt.getBoolean("enableTp");
    }
}
