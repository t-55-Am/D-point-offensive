package com.myname.mymodid.score;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public class TeamData extends SavedData {
    public static final String KEY = "objective_d_teams";
    public static final int MAX_TEAMS = 10;

    public final List<TeamConfig> teams = new ArrayList<>();
    public static final List<TeamConfig> clientTeams = new ArrayList<>();

    public TeamData() {}
    public TeamData(CompoundTag nbt, HolderLookup.Provider provider) { load(nbt, provider); }

    public static TeamData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<TeamData>(TeamData::new, (tag, reg) -> new TeamData(tag, reg),
                net.minecraft.util.datafix.DataFixTypes.LEVEL), KEY);
    }

    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        teams.clear();
        ListTag list = nbt.getList("teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TeamConfig t = new TeamConfig();
            t.readFromNBT(list.getCompound(i), provider);
            teams.add(t);
        }
        if (teams.isEmpty()) initDefaults();
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (TeamConfig t : teams) {
            CompoundTag c = new CompoundTag();
            t.writeToNBT(c, provider);
            list.add(c);
        }
        nbt.put("teams", list);
        return nbt;
    }

    private void initDefaults() {
        if (teams.isEmpty()) {
            teams.add(new TeamConfig("Red", 0, 0));
            teams.add(new TeamConfig("Blue", 1, 1));
            setDirty();
        }
    }

    public TeamConfig find(String name) {
        if (name == null) return null;
        for (TeamConfig t : teams) if (t.name.equals(name)) return t;
        return null;
    }

    public static TeamConfig clientFind(String name) {
        if (name == null) return null;
        for (TeamConfig t : clientTeams) if (t.name.equals(name)) return t;
        return null;
    }

    public static void clientSet(List<TeamConfig> list) {
        clientTeams.clear();
        if (list != null) clientTeams.addAll(list);
    }

    public boolean addTeam(String name, int color) {
        if (name == null || name.trim().isEmpty()) return false;
        if (find(name) != null) return false;
        if (teams.size() >= MAX_TEAMS) return false;
        TeamConfig c = new TeamConfig(name.trim(), color % TeamConfig.PALETTE.length, 0);
        teams.add(c);
        setDirty();
        return true;
    }

    public boolean removeTeam(String name) {
        if (teams.size() <= 1) return false;
        TeamConfig c = find(name);
        if (c == null) return false;
        teams.remove(c);
        setDirty();
        return true;
    }

    public boolean updateTeam(String name, String newName, int color, int side, int max,
                              int sx, int sy, int sz, boolean hasSpawn) {
        TeamConfig c = find(name);
        if (c == null) return false;
        if (newName != null && !newName.trim().isEmpty()) {
            String nn = newName.trim();
            if (!nn.equals(name) && find(nn) != null) return false;
            c.name = nn;
        }
        c.color = color % TeamConfig.PALETTE.length;
        c.side = side == 0 ? 0 : 1;
        c.maxPlayers = Math.max(1, max);
        c.spawnX = sx; c.spawnY = sy; c.spawnZ = sz;
        c.hasSpawn = hasSpawn;
        setDirty();
        return true;
    }

    public void saveBackpack(String name, ItemStack[] template, java.util.List<ItemStack> accessories) {
        TeamConfig c = find(name);
        if (c == null) return;
        c.backpack = template;
        c.accessories = accessories;
        setDirty();
    }
}
