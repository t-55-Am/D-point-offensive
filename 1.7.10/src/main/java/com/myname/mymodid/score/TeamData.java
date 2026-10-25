package com.myname.mymodid.score;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * 全局队伍列表（服务端 WorldSavedData，持久化到世界存档）。
 * 客户端不读 WorldSavedData，用静态 clientTeams 镜像（由 PacketTeamSync 同步）。
 * 默认 2 个队伍：Red（红方）/ Blue（蓝方）；最多 MAX_TEAMS=10 个。
 */
public class TeamData extends WorldSavedData {
    public static final String KEY = "objective_d_teams";
    public static final int MAX_TEAMS = 10;

    public final List<TeamConfig> teams = new ArrayList<TeamConfig>();
    /** 客户端镜像（不含背包模板） */
    public static final List<TeamConfig> clientTeams = new ArrayList<TeamConfig>();

    public TeamData(String key) {
        super(key);
    }

    /** 服务端专用：取世界级队伍数据，不存在则初始化默认 2 队 */
    public static TeamData get(World world) {
        if (world == null || world.isRemote) return null;
        TeamData data = (TeamData) world.mapStorage.loadData(TeamData.class, KEY);
        if (data == null) {
            data = new TeamData(KEY);
            data.initDefaults();
            world.mapStorage.setData(KEY, data);
        }
        return data;
    }

    private void initDefaults() {
        if (teams.isEmpty()) {
            teams.add(new TeamConfig("Red", 0, 0));
            teams.add(new TeamConfig("Blue", 1, 1));
            markDirty();
        }
    }

    public TeamConfig find(String name) {
        if (name == null) return null;
        for (TeamConfig t : teams) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

    /** 客户端查色板用 */
    public static TeamConfig clientFind(String name) {
        if (name == null) return null;
        for (TeamConfig t : clientTeams) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

    public static void clientSet(List<TeamConfig> list) {
        clientTeams.clear();
        if (list != null) clientTeams.addAll(list);
    }

    /** 添加队伍：空名/重名/超上限拒绝 */
    public boolean addTeam(String name, int color) {
        if (name == null || name.trim().isEmpty()) return false;
        if (find(name) != null) return false;
        if (teams.size() >= MAX_TEAMS) return false;
        TeamConfig c = new TeamConfig(name.trim(), color % TeamConfig.PALETTE.length, 0);
        teams.add(c);
        markDirty();
        return true;
    }

    /** 删除队伍：至少保留 1 个 */
    public boolean removeTeam(String name) {
        if (teams.size() <= 1) return false;
        TeamConfig c = find(name);
        if (c == null) return false;
        teams.remove(c);
        markDirty();
        return true;
    }

    /** 保存队伍配置（改名时校验重名） */
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
        c.spawnX = sx;
        c.spawnY = sy;
        c.spawnZ = sz;
        c.hasSpawn = hasSpawn;
        markDirty();
        return true;
    }

    public void saveBackpack(String name, ItemStack[] template) {
        TeamConfig c = find(name);
        if (c == null) return;
        c.backpack = template;
        markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (TeamConfig t : teams) {
            NBTTagCompound c = new NBTTagCompound();
            t.writeToNBT(c);
            list.appendTag(c);
        }
        nbt.setTag("teams", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        teams.clear();
        NBTTagList list = nbt.getTagList("teams", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            TeamConfig t = new TeamConfig();
            t.readFromNBT(list.getCompoundTagAt(i));
            teams.add(t);
        }
        if (teams.isEmpty()) initDefaults();
    }
}
