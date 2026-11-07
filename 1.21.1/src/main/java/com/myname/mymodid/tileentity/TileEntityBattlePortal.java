package com.myname.mymodid.tileentity;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityBattlePortal extends BlockEntity {
    public TileEntityBattlePortal(BlockPos pos, BlockState state) {
        super(MyMod.BATTLE_PORTAL_TE.get(), pos, state);
    }

    private void migrateLegacy(CompoundTag nbt, HolderLookup.Provider provider) {
        try {
            if (!(level instanceof ServerLevel sl)) return;
            if (!nbt.contains("redTeamName")) return;
            TeamData data = TeamData.get(sl);
            if (data == null) return;
            TeamConfig r = data.find("Red");
            if (r == null) data.addTeam("Red", 0);
            r = data.find("Red");
            if (r != null) {
                r.name = nbt.contains("redTeamName") ? nbt.getString("redTeamName") : "Red";
                r.maxPlayers = Math.max(1, nbt.getInt("redMaxPlayers"));
                r.hasSpawn = nbt.getBoolean("redHasSpawn");
                r.spawnX = nbt.getInt("redSpawnX");
                r.spawnY = nbt.getInt("redSpawnY");
                r.spawnZ = nbt.getInt("redSpawnZ");
                ItemStack[] bp = readBackpack(nbt, "redBackpack", provider);
                if (bp != null) r.backpack = bp;
            }
            TeamConfig b = data.find("Blue");
            if (b == null) data.addTeam("Blue", 1);
            b = data.find("Blue");
            if (b != null) {
                b.name = nbt.contains("blueTeamName") ? nbt.getString("blueTeamName") : "Blue";
                b.maxPlayers = Math.max(1, nbt.getInt("blueMaxPlayers"));
                b.hasSpawn = nbt.getBoolean("blueHasSpawn");
                b.spawnX = nbt.getInt("blueSpawnX");
                b.spawnY = nbt.getInt("blueSpawnY");
                b.spawnZ = nbt.getInt("blueSpawnZ");
                ItemStack[] bp = readBackpack(nbt, "blueBackpack", provider);
                if (bp != null) b.backpack = bp;
            }
            data.setDirty();
        } catch (Exception e) {}
    }

    private static ItemStack[] readBackpack(CompoundTag nbt, String key, HolderLookup.Provider provider) {
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return null;
        ItemStack[] arr = new ItemStack[40];
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int slot = tag.getByte("slot");
            if (slot >= 0 && slot < 40) arr[slot] = ItemStack.parseOptional(provider, tag);
        }
        return arr;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        migrateLegacy(nbt, provider);
    }
}
