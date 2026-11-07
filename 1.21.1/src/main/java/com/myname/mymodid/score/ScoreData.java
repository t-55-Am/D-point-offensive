package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class ScoreData extends SavedData {
    public static final String NAME = "CapturePointScoreData";

    public int redScore = 10000;
    public int blueScore = 10000;
    public int initialScore = 10000;
    public int scorePerSecond = 10;
    public boolean pveEnabled = false;
    public boolean gameStarted = false;
    public int mode = 0;
    public Map<String, Integer> teamScores = new HashMap<>();
    public double zombieAttack = 4.0;
    public double zombieSpeed = 0.23;
    public double zombieHealth = 20.0;
    public int pveSpawnInterval = 20;
    public int pveSpawnCount = 1;
    public int pveMinCount = 0;
    public int pveMaxCount = 20;

    public ScoreData() {}
    public ScoreData(CompoundTag nbt) { load(nbt); }

    public static ScoreData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<ScoreData>(ScoreData::new, (tag, reg) -> new ScoreData(tag),
                net.minecraft.util.datafix.DataFixTypes.LEVEL), NAME);
    }

    private static final ScoreData CLIENT = new ScoreData();
    public static ScoreData getClient() { return CLIENT; }

    public void load(CompoundTag nbt) {
        redScore = nbt.getInt("redScore");
        blueScore = nbt.getInt("blueScore");
        initialScore = nbt.getInt("initialScore");
        scorePerSecond = nbt.getInt("scorePerSecond");
        pveEnabled = nbt.getBoolean("pveEnabled");
        gameStarted = nbt.getBoolean("gameStarted");
        zombieAttack = nbt.contains("zombieAttack") ? nbt.getDouble("zombieAttack") : 4.0;
        zombieSpeed = nbt.contains("zombieSpeed") ? nbt.getDouble("zombieSpeed") : 0.23;
        zombieHealth = nbt.contains("zombieHealth") ? nbt.getDouble("zombieHealth") : 20.0;
        pveSpawnInterval = nbt.getInt("pveSpawnInterval");
        pveSpawnCount = nbt.getInt("pveSpawnCount");
        pveMinCount = nbt.getInt("pveMinCount");
        pveMaxCount = nbt.getInt("pveMaxCount");
        mode = nbt.getInt("mode");
        teamScores.clear();
        if (nbt.contains("teamScores")) {
            ListTag list = nbt.getList("teamScores", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                teamScores.put(t.getString("n"), t.getInt("s"));
            }
        }
        if (nbt.contains("playerStats")) {
            ListTag l = nbt.getList("playerStats", CompoundTag.TAG_COMPOUND);
            PlayerStats.stats.clear();
            for (int i = 0; i < l.size(); i++) {
                CompoundTag t = l.getCompound(i);
                PlayerStats.stats.put(t.getString("n"),
                    new int[]{t.getInt("k"), t.getInt("a"), t.getInt("c"), t.getInt("d")});
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.putInt("redScore", redScore);
        nbt.putInt("blueScore", blueScore);
        nbt.putInt("initialScore", initialScore);
        nbt.putInt("scorePerSecond", scorePerSecond);
        nbt.putBoolean("pveEnabled", pveEnabled);
        nbt.putBoolean("gameStarted", gameStarted);
        nbt.putDouble("zombieAttack", zombieAttack);
        nbt.putDouble("zombieSpeed", zombieSpeed);
        nbt.putDouble("zombieHealth", zombieHealth);
        nbt.putInt("pveSpawnInterval", pveSpawnInterval);
        nbt.putInt("pveSpawnCount", pveSpawnCount);
        nbt.putInt("pveMinCount", pveMinCount);
        nbt.putInt("pveMaxCount", pveMaxCount);
        nbt.putInt("mode", mode);
        ListTag scoreList = new ListTag();
        for (Map.Entry<String, Integer> e : teamScores.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("n", e.getKey());
            t.putInt("s", e.getValue() == null ? 0 : e.getValue());
            scoreList.add(t);
        }
        nbt.put("teamScores", scoreList);
        ListTag statsList = new ListTag();
        for (java.util.Map.Entry<String, int[]> e : PlayerStats.stats.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("n", e.getKey());
            int[] v = e.getValue();
            t.putInt("k", v[0]); t.putInt("a", v[1]); t.putInt("c", v[2]); t.putInt("d", v[3]);
            statsList.add(t);
        }
        nbt.put("playerStats", statsList);
        return nbt;
    }

    public void reset() {
        redScore = initialScore;
        blueScore = initialScore;
        teamScores.clear();
        PlayerStats.clear();
        setDirty();
    }
}
