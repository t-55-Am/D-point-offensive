package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

public class ScoreData extends WorldSavedData {
    public static final String NAME = "CapturePointScoreData";
    public int redScore = 10000;
    public int blueScore = 10000;
    public int initialScore = 10000;
    public int scorePerSecond = 10;
    public boolean pveEnabled = false;
    public boolean gameStarted = false;

    // v1.1.11：对抗模式。0=双队对抗（红蓝），1=多队伍（每队独立分数）
    public int mode = 0;
    public Map<String, Integer> teamScores = new HashMap<String, Integer>();

    public double zombieAttack = 4.0;
    public double zombieSpeed = 0.23;
    public double zombieHealth = 20.0;

    public int pveSpawnInterval = 20;
    public int pveSpawnCount = 1;
    public int pveMinCount = 0;
    public int pveMaxCount = 20;

    public ScoreData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        redScore = nbt.getInteger("redScore");
        blueScore = nbt.getInteger("blueScore");
        initialScore = nbt.getInteger("initialScore");
        scorePerSecond = nbt.getInteger("scorePerSecond");
        pveEnabled = nbt.getBoolean("pveEnabled");
        gameStarted = nbt.getBoolean("gameStarted");
        zombieAttack = nbt.hasKey("zombieAttack") ? nbt.getDouble("zombieAttack") : 4.0;
        zombieSpeed = nbt.hasKey("zombieSpeed") ? nbt.getDouble("zombieSpeed") : 0.23;
        zombieHealth = nbt.hasKey("zombieHealth") ? nbt.getDouble("zombieHealth") : 20.0;
        pveSpawnInterval = nbt.getInteger("pveSpawnInterval");
        pveSpawnCount = nbt.getInteger("pveSpawnCount");
        pveMinCount = nbt.getInteger("pveMinCount");
        pveMaxCount = nbt.getInteger("pveMaxCount");
        mode = nbt.getInteger("mode");
        teamScores.clear();
        NBTTagList list = nbt.getTagList("teamScores", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            teamScores.put(t.getString("n"), t.getInteger("s"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("redScore", redScore);
        nbt.setInteger("blueScore", blueScore);
        nbt.setInteger("initialScore", initialScore);
        nbt.setInteger("scorePerSecond", scorePerSecond);
        nbt.setBoolean("pveEnabled", pveEnabled);
        nbt.setBoolean("gameStarted", gameStarted);
        nbt.setDouble("zombieAttack", zombieAttack);
        nbt.setDouble("zombieSpeed", zombieSpeed);
        nbt.setDouble("zombieHealth", zombieHealth);
        nbt.setInteger("pveSpawnInterval", pveSpawnInterval);
        nbt.setInteger("pveSpawnCount", pveSpawnCount);
        nbt.setInteger("pveMinCount", pveMinCount);
        nbt.setInteger("pveMaxCount", pveMaxCount);
        nbt.setInteger("mode", mode);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Integer> e : teamScores.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setString("n", e.getKey());
            t.setInteger("s", e.getValue() == null ? 0 : e.getValue());
            list.appendTag(t);
        }
        nbt.setTag("teamScores", list);
    }

    public static ScoreData get(World world) {
        ScoreData data = (ScoreData) world.mapStorage.loadData(ScoreData.class, NAME);
        if (data == null) {
            data = new ScoreData(NAME);
            world.mapStorage.setData(NAME, data);
        }
        return data;
    }

    public void reset() {
        redScore = initialScore;
        blueScore = initialScore;
        teamScores.clear();
        PlayerStats.clear();
        markDirty();
    }
}
