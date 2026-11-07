package com.myname.mymodid.score;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class TeamConfig {
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
    public List<ItemStack> accessories = null;

    public TeamConfig() {}
    public TeamConfig(String name, int color, int side) {
        this.name = name; this.color = color; this.side = side;
    }

    public void writeToNBT(CompoundTag t, HolderLookup.Provider provider) {
        t.putString("name", name);
        t.putInt("color", color);
        t.putInt("side", side);
        t.putInt("max", maxPlayers);
        t.putInt("sx", spawnX); t.putInt("sy", spawnY); t.putInt("sz", spawnZ);
        t.putBoolean("hasSpawn", hasSpawn);
        writeBackpack(t, "backpack", backpack, provider);
        writeAccessories(t, "acc", accessories, provider);
    }

    public void readFromNBT(CompoundTag t, HolderLookup.Provider provider) {
        name = t.getString("name");
        color = t.getInt("color");
        side = t.getInt("side");
        maxPlayers = t.contains("max") ? t.getInt("max") : 10;
        if (maxPlayers <= 0) maxPlayers = 10;
        spawnX = t.getInt("sx"); spawnY = t.getInt("sy"); spawnZ = t.getInt("sz");
        hasSpawn = t.getBoolean("hasSpawn");
        backpack = readBackpack(t, "backpack", provider);
        accessories = readAccessories(t, "acc", provider);
    }

    private static void writeBackpack(CompoundTag nbt, String key, ItemStack[] arr, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null && !arr[i].isEmpty()) {
                    CompoundTag tag = new CompoundTag();
                    tag.putByte("slot", (byte) i);
                    arr[i].save(provider, tag);
                    list.add(tag);
                }
            }
        }
        nbt.put(key, list);
    }

    private static ItemStack[] readBackpack(CompoundTag nbt, String key, HolderLookup.Provider provider) {
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return null;
        ItemStack[] arr = new ItemStack[41];
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int slot = tag.getByte("slot");
            if (slot >= 0 && slot < 41) arr[slot] = ItemStack.parseOptional(provider, tag);
        }
        return arr;
    }

    private static void writeAccessories(CompoundTag nbt, String key, List<ItemStack> acc, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        if (acc != null) {
            for (int i = 0; i < acc.size(); i++) {
                ItemStack s = acc.get(i);
                if (s != null && !s.isEmpty()) {
                    CompoundTag tag = new CompoundTag();
                    tag.putByte("slot", (byte) i);
                    s.save(provider, tag);
                    list.add(tag);
                }
            }
        }
        nbt.put(key, list);
    }

    private static List<ItemStack> readAccessories(CompoundTag nbt, String key, HolderLookup.Provider provider) {
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return null;
        List<ItemStack> acc = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int slot = tag.getByte("slot");
            while (acc.size() <= slot) acc.add(null);
            acc.set(slot, ItemStack.parseOptional(provider, tag));
        }
        return acc;
    }
}
