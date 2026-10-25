package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public class CapturePointData extends WorldSavedData {
    public static final String NAME = "mymodid_capturepoints";

    public static class Entry {
        public String name = "Point A";
        public int x, y, z;
        public String ownerTeam = null;
        public String capturingTeam = null;
        public double progress = 0;
        public int radius = 5;
    }

    private final Map<Long, Entry> points = new HashMap<Long, Entry>();

    public CapturePointData() { super(NAME); }
    public CapturePointData(String name) { super(name); }

    public static CapturePointData get(World world) {
        CapturePointData data = (CapturePointData) world.getMapStorage().getOrLoadData(CapturePointData.class, NAME);
        if (data == null) {
            data = new CapturePointData();
            world.getMapStorage().setData(NAME, data);
        }
        return data;
    }

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
            markDirty();
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
        if (changed) markDirty();
    }

    public void remove(int x, int y, int z) {
        points.remove(key(x, y, z));
        markDirty();
    }

    public Map<Long, Entry> getAll() {
        return points;
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Entry e : points.values()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("x", e.x);
            t.setInteger("y", e.y);
            t.setInteger("z", e.z);
            t.setString("name", e.name == null ? "" : e.name);
            if (e.ownerTeam != null) t.setString("owner", e.ownerTeam);
            if (e.capturingTeam != null) t.setString("capturing", e.capturingTeam);
            t.setDouble("progress", e.progress);
            t.setInteger("radius", e.radius);
            list.appendTag(t);
        }
        nbt.setTag("points", list);
        return nbt;
    
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        points.clear();
        NBTTagList list = nbt.getTagList("points", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            Entry e = new Entry();
            e.x = t.getInteger("x");
            e.y = t.getInteger("y");
            e.z = t.getInteger("z");
            e.name = t.hasKey("name") ? t.getString("name") : "Point A";
            e.ownerTeam = t.hasKey("owner") ? t.getString("owner") : null;
            e.capturingTeam = t.hasKey("capturing") ? t.getString("capturing") : null;
            e.progress = t.getDouble("progress");
            e.radius = t.getInteger("radius");
            points.put(key(e.x, e.y, e.z), e);
        }
    }
}
