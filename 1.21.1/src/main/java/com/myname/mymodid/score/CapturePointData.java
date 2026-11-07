package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public class CapturePointData extends SavedData {
    public static final String NAME = "mymodid_capturepoints";

    public static class Entry {
        public String name = "Point A";
        public int x, y, z;
        public String ownerTeam = null;
        public String capturingTeam = null;
        public double progress = 0;
        public int radius = 5;
    }

    private final Map<Long, Entry> points = new HashMap<>();

    public CapturePointData() { }
    public CapturePointData(CompoundTag nbt) { load(nbt); }

    public static CapturePointData get(net.minecraft.world.level.LevelAccessor level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new Factory<CapturePointData>(CapturePointData::new, (tag, reg) -> new CapturePointData(tag),
                net.minecraft.util.datafix.DataFixTypes.LEVEL), NAME);
    }

    private static final CapturePointData CLIENT = new CapturePointData();
    public static CapturePointData getClient() { return CLIENT; }

    public static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF);
    }

    public Entry getOrCreate(int x, int y, int z) {
        long k = key(x, y, z);
        Entry e = points.get(k);
        if (e == null) {
            e = new Entry();
            e.x = x; e.y = y; e.z = z;
            points.put(k, e);
            setDirty();
        }
        return e;
    }

    public void update(int x, int y, int z, String name, String owner, String capturing, double progress, int radius) {
        Entry e = getOrCreate(x, y, z);
        boolean changed = false;
        if (!eq(e.name, name)) { e.name = name; changed = true; }
        if (!eq(e.ownerTeam, owner)) { e.ownerTeam = owner; changed = true; }
        if (!eq(e.capturingTeam, capturing)) { e.capturingTeam = capturing; changed = true; }
        if (e.progress != progress) { e.progress = progress; changed = true; }
        if (e.radius != radius) { e.radius = radius; changed = true; }
        if (changed) setDirty();
    }

    public void remove(int x, int y, int z) { points.remove(key(x, y, z)); setDirty(); }
    public Map<Long, Entry> getAll() { return points; }
    public void clearAll() { points.clear(); }

    private boolean eq(String a, String b) { return a == null ? b == null : a.equals(b); }

    public void load(CompoundTag nbt) {
        points.clear();
        ListTag list = nbt.getList("points", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry e = new Entry();
            e.x = t.getInt("x"); e.y = t.getInt("y"); e.z = t.getInt("z");
            e.name = t.contains("name") ? t.getString("name") : "Point A";
            e.ownerTeam = t.contains("owner") ? t.getString("owner") : null;
            e.capturingTeam = t.contains("capturing") ? t.getString("capturing") : null;
            e.progress = t.getDouble("progress");
            e.radius = t.getInt("radius");
            points.put(key(e.x, e.y, e.z), e);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry e : points.values()) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", e.x); t.putInt("y", e.y); t.putInt("z", e.z);
            t.putString("name", e.name == null ? "" : e.name);
            if (e.ownerTeam != null) t.putString("owner", e.ownerTeam);
            if (e.capturingTeam != null) t.putString("capturing", e.capturingTeam);
            t.putDouble("progress", e.progress);
            t.putInt("radius", e.radius);
            list.add(t);
        }
        nbt.put("points", list);
        return nbt;
    }
}
