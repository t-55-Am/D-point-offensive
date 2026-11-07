package com.myname.mymodid.tileentity;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.network.SPayloads.CapturePointSyncPayload;
import com.myname.mymodid.network.SPayloads.ScoreUpdatePayload;
import com.myname.mymodid.score.CapturePointData;
import com.myname.mymodid.score.PlayerStats;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.score.ScoreTickHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class TileEntityCapturePoint extends BlockEntity {
    public static final java.util.List<TileEntityCapturePoint> REGISTRY = new java.util.concurrent.CopyOnWriteArrayList<>();

    private int radius = 5;
    private int captureTime = 30;
    private String pointName = "Point A";
    private double captureProgress = 0;
    private String ownerTeam = null;
    private String capturingTeam = null;
    private int reportTimer = 0;
    private int pveSpawnTimer = 0;
    private int syncTimer = 0;
    private boolean isMainBase = false;
    private boolean reviveMode = false;
    private boolean hasBeenZombieCaptured = false;

    private int pveSpawnInterval = 20;
    private int pveSpawnCount = 3;
    private int pveMinCount = 0;
    private int pveMaxCount = 20;

    public TileEntityCapturePoint(BlockPos pos, BlockState state) {
        super(MyMod.CAPTURE_POINT_TE.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        REGISTRY.remove(this);
        super.setRemoved();
    }

    public void resetToNeutral() {
        captureProgress = 0;
        ownerTeam = null;
        capturingTeam = null;
        Level w = level;
        if (w != null) {
            w.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            CapturePointData data = CapturePointData.get(w);
            data.update(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                pointName, ownerTeam, capturingTeam, captureProgress, radius);
            Network.sendToAllPlayers(new CapturePointSyncPayload(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                pointName, ownerTeam, capturingTeam, captureProgress, radius));
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityCapturePoint te) {
        te.tickInternal();
    }

    public void tickInternal() {
        if (!REGISTRY.contains(this)) REGISTRY.add(this);

        if (level == null || level.isClientSide) return;

        ScoreData sd = ScoreData.get((ServerLevel) level);

        syncTimer++;
        if (syncTimer >= 10) {
            syncTimer = 0;
            CapturePointData data = CapturePointData.get(level);
            data.update(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                pointName, ownerTeam, capturingTeam, captureProgress, radius);
            Network.sendToAllPlayers(new CapturePointSyncPayload(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                pointName, ownerTeam, capturingTeam, captureProgress, radius));
        }

        if (!sd.gameStarted) return;

        if (isMainBase && ownerTeam == null) {
            ownerTeam = sd.pveEnabled ? "PVEHUMAN" : "Red";
        }

        boolean pve = sd.pveEnabled;

        if (MyModConfig.ENABLE_CAPTURE.get()) {
            AABB aabb = new AABB(
                worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1);
            List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
            Map<String, Integer> teamCounts = new HashMap<>();

            if (pve) {
                int cnt = 0;
                for (Player p : players) {
                    if (getPlayerTeam(p) != null) cnt++;
                }
                if (cnt > 0) teamCounts.put("PVEHUMAN", cnt);
            } else {
                for (Player p : players) {
                    net.minecraft.world.scores.PlayerTeam team = getPlayerTeam(p);
                    if (team == null) continue;
                    String name = team.getName();
                    teamCounts.merge(name, 1, Integer::sum);
                }
            }

            double ratePerTick = 100.0 / (captureTime * 20.0);

            if (teamCounts.isEmpty()) {
                boolean zombieHandled = false;
                if (pve) {
                    List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, aabb);
                    if (!zombies.isEmpty()) {
                        zombieHandled = true;
                        if ("PvE".equals(ownerTeam)) {
                            captureProgress = 0;
                            capturingTeam = null;
                        } else {
                            if (!"PvE".equals(capturingTeam)) {
                                capturingTeam = "PvE";
                                if (!"PvE".equals(ownerTeam)) captureProgress = 0;
                            }
                            captureProgress = Math.min(100, captureProgress + ratePerTick);
                            if (captureProgress >= 100) {
                                ownerTeam = "PvE";
                                captureProgress = 0;
                                capturingTeam = null;
                                level.playSound(null, worldPosition, SoundEvents.ZOMBIE_INFECT, SoundSource.BLOCKS, 1.0F, 1.0F);
                                broadcastServerMessage(Component.translatable("dp.msg.zombie_capture", pointName).withStyle(ChatFormatting.DARK_RED));
                                AABB aabbN = new AABB(
                                    worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                                    worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1);
                                List<Zombie> nearZ = level.getEntitiesOfClass(Zombie.class, aabbN);
                                int cur = nearZ.size();
                                int count = this.pveSpawnCount;
                                if (!hasBeenZombieCaptured) {
                                    count *= 3;
                                    hasBeenZombieCaptured = true;
                                }
                                if (cur >= this.pveMaxCount) {
                                } else if (cur < this.pveMinCount) {
                                    spawnZombieReinforcements(this.pveMinCount - cur);
                                } else {
                                    spawnZombieReinforcements(count);
                                }
                            }
                        }
                    }
                }
                if (!zombieHandled) {
                    if (captureProgress > 0) {
                        captureProgress = Math.max(0, captureProgress - ratePerTick);
                        if (captureProgress == 0) capturingTeam = null;
                    }
                }
            } else if (teamCounts.size() == 1) {
                String teamName = teamCounts.keySet().iterator().next();
                if (teamName.equals(ownerTeam)) {
                    captureProgress = 0;
                    capturingTeam = null;
                } else {
                    if (!teamName.equals(capturingTeam)) {
                        capturingTeam = teamName;
                        captureProgress = 0;
                    }
                    captureProgress = Math.min(100, captureProgress + ratePerTick);
                    if (captureProgress >= 100) {
                        ownerTeam = teamName;

                        captureProgress = 0;
                        capturingTeam = null;
                        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                        String showName = "PVEHUMAN".equals(teamName)
                            ? Component.translatable("dp.msg.humans").getString() : teamName;
                        broadcastServerMessage(Component.translatable("dp.msg.captured", pointName, showName)
                            .withStyle(ChatFormatting.GOLD));
                        for (Player p2 : players) {
                            net.minecraft.world.scores.PlayerTeam t2 = getPlayerTeam(p2);
                            boolean shouldAdd = false;
                            if (pve) {
                                shouldAdd = (t2 != null);
                            } else {
                                shouldAdd = (t2 != null && teamName.equals(t2.getName()));
                            }
                            if (shouldAdd) {
                                PlayerStats.addCap(p2.getScoreboardName());
                            }
                        }
                        ScoreTickHandler.syncAllStats((ServerLevel) level);

                        if (!pve) {
                            ScoreData sdd = ScoreData.get((ServerLevel) level);
                            boolean newOwnerRed = ScoreTickHandler.isRedOwner((ServerLevel) level, teamName);
                            if (newOwnerRed) {
                                sdd.blueScore = Math.max(0, sdd.blueScore - sdd.scorePerSecond * 10);
                            } else {
                                sdd.redScore = Math.max(0, sdd.redScore - sdd.scorePerSecond * 10);
                            }
                            sdd.setDirty();
                            Network.sendToAllPlayers(new ScoreUpdatePayload(sdd.redScore, sdd.blueScore));
                        }

                        if (isMainBase) {
                            ScoreData sdd = ScoreData.get((ServerLevel) level);
                            if (pve) {
                                if ("PvE".equals(teamName)) {
                                    sdd.redScore = 0;
                                    sdd.blueScore = 0;
                                    broadcastServerMessage(Component.translatable("dp.msg.base_zombie").withStyle(ChatFormatting.DARK_RED));
                                } else {
                                    broadcastServerMessage(Component.translatable("dp.msg.base_human").withStyle(ChatFormatting.GREEN));
                                }
                            } else {
                                String lower = teamName.toLowerCase();
                                if (lower.contains("red") || teamName.contains("红")) {
                                    sdd.blueScore = 0;
                                    broadcastServerMessage(Component.translatable("dp.msg.base_red").withStyle(ChatFormatting.RED));
                                } else {
                                    sdd.redScore = 0;
                                    broadcastServerMessage(Component.translatable("dp.msg.base_blue").withStyle(ChatFormatting.BLUE));
                                }
                            }
                            sdd.setDirty();
                            Network.sendToAllPlayers(new ScoreUpdatePayload(sdd.redScore, sdd.blueScore));
                        }
                    }
                }
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        reportTimer++;
        if (reportTimer >= 10) {
            reportTimer = 0;
            if (pve) {
                boolean alreadyZombie = "PvE".equals(ownerTeam);
                int viewDist = level.getServer().getPlayerList().getViewDistance();
                int guideRange = isMainBase ? Math.min(viewDist * 16, 256) : (radius + 16);
                AABB aabbZ = new AABB(
                    worldPosition.getX() - guideRange, worldPosition.getY() - guideRange, worldPosition.getZ() - guideRange,
                    worldPosition.getX() + guideRange + 1, worldPosition.getY() + guideRange + 1, worldPosition.getZ() + guideRange + 1);
                List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, aabbZ);
                Player nearestPlayer = null;
                double nearestDist = Double.MAX_VALUE;
                AABB aabbP = new AABB(
                    worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                    worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1);
                List<Player> nearPlayers = level.getEntitiesOfClass(Player.class, aabbP);
                for (Player pp : nearPlayers) {
                    double d = pp.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
                    if (d < nearestDist) {
                        nearestDist = d;
                        nearestPlayer = pp;
                    }
                }
                for (Zombie z : zombies) {
                    if (z.getTarget() instanceof Player) continue;
                    if (nearestPlayer != null) {
                        z.setTarget(nearestPlayer);
                    } else if (!alreadyZombie) {
                        z.getNavigation().moveTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, 1.0);
                    }
                }
            }

            if (reviveMode && ownerTeam != null) {
                for (ServerPlayer mp : level.getServer().getPlayerList().getPlayers()) {
                    net.minecraft.world.scores.PlayerTeam tt = getPlayerTeam(mp);
                    if (tt != null) {
                        String tname = tt.getName();
                        boolean match = "PVEHUMAN".equals(ownerTeam) || ownerTeam.equals(tname);
                        if (match) {
                            mp.setRespawnPosition(level.dimension(),
                                new BlockPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()),
                                0.0F, true, false);
                        }
                    }
                }
            }
        }

        if (pve && "PvE".equals(ownerTeam)) {
            pveSpawnTimer++;
            if (pveSpawnTimer >= this.pveSpawnInterval * 20) {
                pveSpawnTimer = 0;
                AABB aabb2 = new AABB(
                    worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                    worldPosition.getX() + radius + 1, worldPosition.getY() + radius + 1, worldPosition.getZ() + radius + 1);
                List<Zombie> near = level.getEntitiesOfClass(Zombie.class, aabb2);
                int cur = near.size();
                int count = this.pveSpawnCount;
                boolean beingRecaptured = capturingTeam != null && !"PvE".equals(capturingTeam);
                if (beingRecaptured) count *= 2;
                if (cur >= this.pveMaxCount) {
                } else if (cur < this.pveMinCount) {
                    spawnZombieReinforcements(this.pveMinCount - cur);
                } else {
                    spawnZombieReinforcements(count);
                }
            }
        } else {
            pveSpawnTimer = 0;
        }
    }

    private void spawnZombieReinforcements(int count) {
        ScoreData sd = ScoreData.get((ServerLevel) level);
        for (int i = 0; i < count; i++) {
            Zombie z = new Zombie(level);
            double ang = level.random.nextDouble() * Math.PI * 2;
            double dist = 2 + level.random.nextDouble() * radius;
            z.moveTo(worldPosition.getX() + 0.5 + Math.cos(ang) * dist, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5 + Math.sin(ang) * dist,
                level.random.nextFloat() * 360.0F, 0.0F);
            try {
                z.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(sd.zombieAttack);
                z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(sd.zombieSpeed);
                z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(sd.zombieHealth);
                z.setHealth((float) sd.zombieHealth);
            } catch (Exception e) {}
            level.addFreshEntity(z);
        }
    }

    private net.minecraft.world.scores.PlayerTeam getPlayerTeam(Player p) {
        String name = p.getScoreboardName();
        net.minecraft.world.scores.Scoreboard sb = level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam t = sb.getPlayersTeam(name);
        if (t != null) return t;
        for (net.minecraft.world.scores.PlayerTeam team : sb.getPlayerTeams()) {
            if (team.getPlayers().contains(name)) {
                return team;
            }
        }
        return null;
    }

    private void broadcastServerMessage(Component msg) {
        if (level instanceof ServerLevel sl) {
            sl.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    public void setRadius(int r) { this.radius = Math.max(1, Math.min(64, r)); }
    public void setCaptureTime(int t) { this.captureTime = Math.max(1, Math.min(3600, t)); }
    public void setPointName(String n) { this.pointName = n; }
    public int getRadius() { return radius; }
    public int getCaptureTime() { return captureTime; }
    public String getPointName() { return pointName; }
    public double getCaptureProgress() { return captureProgress; }
    public String getOwnerTeam() { return ownerTeam; }
    public void setOwnerTeam(String t) { this.ownerTeam = t; }
    public String getCapturingTeam() { return capturingTeam; }
    public boolean isMainBase() { return isMainBase; }
    public void setMainBase(boolean v) { this.isMainBase = v; }
    public boolean isReviveMode() { return reviveMode; }
    public void setReviveMode(boolean v) { this.reviveMode = v; }

    public int getPveSpawnInterval() { return pveSpawnInterval; }
    public void setPveSpawnInterval(int v) { this.pveSpawnInterval = Math.max(1, Math.min(600, v)); }
    public int getPveSpawnCount() { return pveSpawnCount; }
    public void setPveSpawnCount(int v) { this.pveSpawnCount = Math.max(0, Math.min(100, v)); }
    public int getPveMinCount() { return pveMinCount; }
    public void setPveMinCount(int v) { this.pveMinCount = Math.max(0, Math.min(100, v)); }
    public int getPveMaxCount() { return pveMaxCount; }
    public void setPveMaxCount(int v) { this.pveMaxCount = Math.max(0, Math.min(200, v)); }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putInt("radius", radius);
        nbt.putInt("captureTime", captureTime);
        nbt.putString("pointName", pointName);
        nbt.putDouble("captureProgress", captureProgress);
        nbt.putBoolean("isMainBase", isMainBase);
        nbt.putBoolean("reviveMode", reviveMode);
        nbt.putBoolean("hasBeenZombieCaptured", hasBeenZombieCaptured);
        nbt.putInt("pveSpawnInterval", pveSpawnInterval);
        nbt.putInt("pveSpawnCount", pveSpawnCount);
        nbt.putInt("pveMinCount", pveMinCount);
        nbt.putInt("pveMaxCount", pveMaxCount);
        if (ownerTeam != null) nbt.putString("ownerTeam", ownerTeam);
        if (capturingTeam != null) nbt.putString("capturingTeam", capturingTeam);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        radius = nbt.getInt("radius");
        captureTime = nbt.getInt("captureTime");
        pointName = nbt.contains("pointName") ? nbt.getString("pointName") : "Point A";
        captureProgress = nbt.getDouble("captureProgress");
        isMainBase = nbt.getBoolean("isMainBase");
        reviveMode = nbt.getBoolean("reviveMode");
        hasBeenZombieCaptured = nbt.getBoolean("hasBeenZombieCaptured");
        pveSpawnInterval = nbt.contains("pveSpawnInterval") ? nbt.getInt("pveSpawnInterval") : 20;
        pveSpawnCount = nbt.contains("pveSpawnCount") ? nbt.getInt("pveSpawnCount") : 3;
        pveMinCount = nbt.contains("pveMinCount") ? nbt.getInt("pveMinCount") : 0;
        pveMaxCount = nbt.contains("pveMaxCount") ? nbt.getInt("pveMaxCount") : 20;
        ownerTeam = nbt.contains("ownerTeam") ? nbt.getString("ownerTeam") : null;
        capturingTeam = nbt.contains("capturingTeam") ? nbt.getString("capturingTeam") : null;
    }
}
