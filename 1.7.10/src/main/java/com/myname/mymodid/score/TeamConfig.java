package com.myname.mymodid.score;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 单个队伍配置（自由队伍系统）。
 * name=队伍名（唯一，直接匹配据点归属）；color=色板索引 0-9；
 * side=阵营（0=红方 1=蓝方，决定占点掉分方向）；spawn=传送点；backpack=40 格换装模板。
 */
public class TeamConfig {
    /** 10 色色板（ARGB，无 alpha） */
    public static final int[] PALETTE = {
        0xFF5555, 0x55AAFF, 0x55FF55, 0xFFFF55, 0xFF55FF,
        0x55FFFF, 0xFFAA00, 0xFFA0C0, 0xA05A2C, 0xFFFFFF
    };
    public static final String[] PALETTE_KEYS = {
        "color.red", "color.blue", "color.green", "color.yellow", "color.purple",
        "color.cyan", "color.orange", "color.pink", "color.brown", "color.white"
    };

    public String name = "";
    public int color = 0;
    public int side = 0;
    public int maxPlayers = 10;
    public int spawnX, spawnY, spawnZ;
    public boolean hasSpawn = false;
    public ItemStack[] backpack = null;

    public TeamConfig() {}

    public TeamConfig(String name, int color, int side) {
        this.name = name;
        this.color = color;
        this.side = side;
    }

    public void writeToNBT(NBTTagCompound t) {
        t.setString("name", name);
        t.setInteger("color", color);
        t.setInteger("side", side);
        t.setInteger("max", maxPlayers);
        t.setInteger("sx", spawnX);
        t.setInteger("sy", spawnY);
        t.setInteger("sz", spawnZ);
        t.setBoolean("hasSpawn", hasSpawn);
        writeBackpack(t, "backpack", backpack);
    }

    public void readFromNBT(NBTTagCompound t) {
        name = t.getString("name");
        color = t.getInteger("color");
        side = t.getInteger("side");
        maxPlayers = t.hasKey("max") ? t.getInteger("max") : 10;
        if (maxPlayers <= 0) maxPlayers = 10;
        spawnX = t.getInteger("sx");
        spawnY = t.getInteger("sy");
        spawnZ = t.getInteger("sz");
        hasSpawn = t.getBoolean("hasSpawn");
        backpack = readBackpack(t, "backpack");
    }

    private static void writeBackpack(NBTTagCompound nbt, String key, ItemStack[] arr) {
        NBTTagList list = new NBTTagList();
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null) {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setByte("slot", (byte) i);
                    arr[i].writeToNBT(tag);
                    list.appendTag(tag);
                }
            }
        }
        nbt.setTag(key, list);
    }

    private static ItemStack[] readBackpack(NBTTagCompound nbt, String key) {
        NBTTagList list = nbt.getTagList(key, 10);
        if (list.tagCount() == 0) return null;
        ItemStack[] arr = new ItemStack[40];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("slot");
            if (slot >= 0 && slot < 40) {
                arr[slot] = ItemStack.loadItemStackFromNBT(tag);
            }
        }
        return arr;
    }
}
