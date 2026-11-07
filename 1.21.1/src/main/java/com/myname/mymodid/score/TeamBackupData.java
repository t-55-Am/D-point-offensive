package com.myname.mymodid.score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public class TeamBackupData extends SavedData {
    public static final String KEY = "objective_d_backup";

    public static class Backup {
        public ItemStack[] inv = new ItemStack[41];
        public List<ItemStack> acc = new ArrayList<>();
    }

    public final Map<UUID, Backup> backups = new HashMap<>();

    public TeamBackupData() {}
    public TeamBackupData(CompoundTag nbt, HolderLookup.Provider provider) { load(nbt, provider); }

    public static TeamBackupData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<TeamBackupData>(TeamBackupData::new, (tag, reg) -> new TeamBackupData(tag, reg),
                net.minecraft.util.datafix.DataFixTypes.LEVEL), KEY);
    }

    public void saveBackup(UUID uuid, ItemStack[] inv, List<ItemStack> acc) {
        Backup b = new Backup();
        b.inv = inv;
        b.acc = acc == null ? new ArrayList<>() : acc;
        backups.put(uuid, b);
        setDirty();
    }

    public Backup takeBackup(UUID uuid) {
        Backup b = backups.remove(uuid);
        if (b != null) setDirty();
        return b;
    }

    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        backups.clear();
        ListTag list = nbt.getList("backups", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            UUID uuid;
            try { uuid = UUID.fromString(t.getString("uuid")); } catch (Exception e) { continue; }
            Backup b = new Backup();
            ListTag slots = t.getList("inv", Tag.TAG_COMPOUND);
            for (int j = 0; j < slots.size(); j++) {
                CompoundTag s = slots.getCompound(j);
                int slot = s.getByte("slot");
                if (slot >= 0 && slot < 41) b.inv[slot] = ItemStack.parseOptional(provider, s);
            }
            ListTag accList = t.getList("acc", Tag.TAG_COMPOUND);
            for (int j = 0; j < accList.size(); j++) {
                CompoundTag s = accList.getCompound(j);
                int slot = s.getByte("slot");
                while (b.acc.size() <= slot) b.acc.add(null);
                b.acc.set(slot, ItemStack.parseOptional(provider, s));
            }
            backups.put(uuid, b);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Backup> e : backups.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("uuid", e.getKey().toString());
            Backup b = e.getValue();
            ListTag slots = new ListTag();
            if (b != null && b.inv != null) {
                for (int j = 0; j < b.inv.length; j++) {
                    if (b.inv[j] != null && !b.inv[j].isEmpty()) {
                        CompoundTag s = new CompoundTag();
                        s.putByte("slot", (byte) j);
                        b.inv[j].save(provider, s);
                        slots.add(s);
                    }
                }
            }
            t.put("inv", slots);
            ListTag accList = new ListTag();
            if (b != null && b.acc != null) {
                for (int j = 0; j < b.acc.size(); j++) {
                    ItemStack s = b.acc.get(j);
                    if (s != null && !s.isEmpty()) {
                        CompoundTag s2 = new CompoundTag();
                        s2.putByte("slot", (byte) j);
                        s.save(provider, s2);
                        accList.add(s2);
                    }
                }
            }
            t.put("acc", accList);
            list.add(t);
        }
        nbt.put("backups", list);
        return nbt;
    }
}
