package com.myname.mymodid.tileentity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.block.BlockCapturePoint;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.network.PacketCapturePointSync;
import com.myname.mymodid.network.PacketCapturePointSyncRemove;
import com.myname.mymodid.score.CapturePointData;
import com.myname.mymodid.score.PlayerStats;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.score.ScoreTickHandler;

public class TileEntityCapturePoint extends TileEntity {
    private int radius = 5;
    private int captureTime = 30;
    private String pointName = "Point A";
    private double captureProgress = 0;
    private String ownerTeam = null;
    private String capturingTeam = null;
    private int reportTimer = 0;
    private int pveSpawnTimer = 0;
    private boolean isMainBase = false;
    private boolean reviveMode = false;
    private boolean hasBeenZombieCaptured = false;

    private int pveSpawnInterval = 20;
    private int pveSpawnCount = 3;
    private int pveMinCount = 0;
    private int pveMaxCount = 20;

    @Override
    public void invalidate() {
        if (world != null && !world.isRemote) {
            net.minecraft.block.Block b = world.getBlockState(pos).getBlock();
            if (!(b instanceof BlockCapturePoint)) {
                CapturePointData.get(world).remove(pos.getX(), pos.getY(), pos.getZ());
                MyMod.NETWORK.sendToAll(new PacketCapturePointSyncRemove(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        super.invalidate();
    }

    public void resetToNeutral() {
        captureProgress = 0;
        ownerTeam = null;
        capturingTeam = null;
        if (world != null) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            CapturePointData data = CapturePointData.get(world);
            data.update(pos.getX(), pos.getY(), pos.getZ(), pointName, ownerTeam, capturingTeam, captureProgress, radius);
            MyMod.NETWORK.sendToAll(new PacketCapturePointSync(pos.getX(), pos.getY(), pos.getZ(), pointName, ownerTeam,
                capturingTeam, captureProgress, radius));
        }
    }

    public void update() {
        if (world.isRemote) return;

        ScoreData sd = ScoreData.get(world);
        if (!sd.gameStarted) return;

        if (isMainBase && ownerTeam == null) {
            ownerTeam = sd.pveEnabled ? "PVEHUMAN" : "Red";
        }

        boolean pve = sd.pveEnabled;

        if (MyModConfig.enableCapture) {
            AxisAlignedBB aabb = new AxisAlignedBB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
            List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, aabb);
            Map<String, Integer> teamCounts = new HashMap<String, Integer>();

            if (pve) {
                int cnt = 0;
                for (EntityPlayer p : players) {
                    if (getPlayerTeam(p) != null) cnt++;
                }
                if (cnt > 0) teamCounts.put("PVEHUMAN", cnt);
            } else {
                for (EntityPlayer p : players) {
                    Team team = getPlayerTeam(p);
                    if (team == null) continue;
                    String name = team.getName();
                    Integer c = teamCounts.get(name);
                    teamCounts.put(name, c == null ? 1 : c + 1);
                }
            }

            double ratePerTick = 100.0 / (captureTime * 20.0);

            if (teamCounts.isEmpty()) {
                boolean zombieHandled = false;
                if (pve) {
                    List<EntityZombie> zombies = world.getEntitiesWithinAABB(EntityZombie.class, aabb);
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
                                world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_ZOMBIE_INFECT, net.minecraft.util.SoundCategory.HOSTILE, 1.0F, 1.0F);
                                net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
                                    new TextComponentString(
                                        TextFormatting.DARK_RED + String.format(net.minecraft.client.resources.I18n.format("dp.msg.zombie_capture"), pointName)));
                                AxisAlignedBB aabbN = new AxisAlignedBB(
                                    pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                                    pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
                                List<EntityZombie> nearZ = world.getEntitiesWithinAABB(EntityZombie.class, aabbN);
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
                        world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        String showName = "PVEHUMAN".equals(teamName) ? net.minecraft.client.resources.I18n.format("dp.msg.humans") : teamName;
                        net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
                            new TextComponentString(
                                TextFormatting.GOLD + String.format(net.minecraft.client.resources.I18n.format("dp.msg.captured"), pointName, showName)));
                        for (EntityPlayer p2 : players) {
                            Team t2 = getPlayerTeam(p2);
                            boolean shouldAdd = false;
                            if (pve) {
                                shouldAdd = (t2 != null);
                            } else {
                                shouldAdd = (t2 != null && teamName.equals(t2.getName()));
                            }
                            if (shouldAdd) {
                                PlayerStats.addCap(p2.getName());
                            }
                        }
                        ScoreTickHandler.syncAllStats(world);

                        // ===== 修复：占点完成瞬间掉分（PvP）——占领成功立即扣对手分数，分数条马上可见 =====
                        if (!pve) {
                            ScoreData sdd = ScoreData.get(world);
                            String lower = teamName.toLowerCase();
                            boolean newOwnerRed = lower.contains("red") || teamName.contains("红")
                                || lower.contains("attack") || lower.equals("1");
                            if (newOwnerRed) {
                                sdd.blueScore = Math.max(0, sdd.blueScore - sdd.scorePerSecond * 10);
                            } else {
                                sdd.redScore = Math.max(0, sdd.redScore - sdd.scorePerSecond * 10);
                            }
                            sdd.markDirty();
                            MyMod.NETWORK.sendToAll(new com.myname.mymodid.network.PacketScoreUpdate(sdd.redScore, sdd.blueScore));
                        }

                        if (isMainBase) {
                            ScoreData sdd = ScoreData.get(world);
                            if (pve) {
                                if ("PvE".equals(teamName)) {
                                    sdd.redScore = 0;
                                    sdd.blueScore = 0;
                                    net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
                                        new TextComponentString(
                                            TextFormatting.DARK_RED + net.minecraft.client.resources.I18n.format("dp.msg.base_zombie")));
                                } else {
                                    net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
                                        new TextComponentString(
                                            TextFormatting.GREEN + net.minecraft.client.resources.I18n.format("dp.msg.base_human")));
                                }
                            } else {
                                String lower = teamName.toLowerCase();
                                if (lower.contains("red") || teamName.contains("红")) {
                                    sdd.blueScore = 0;
                                    net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
                                        new TextComponentString(
                                            TextFormatting.RED + net.minecraft.client.resources.I18n.format("dp.msg.base_red")));
                                } else {
                                    sdd.redScore = 0;
                                    net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
                                        new TextComponentString(
                                            TextFormatting.BLUE + net.minecraft.client.resources.I18n.format("dp.msg.base_blue")));
                                }
                            }
                            sdd.markDirty();
                            MyMod.NETWORK.sendToAll(new com.myname.mymodid.network.PacketScoreUpdate(sdd.redScore, sdd.blueScore));
                        }
                    }
                }
            }
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }

        reportTimer++;
        if (reportTimer >= 10) {
            reportTimer = 0;
            CapturePointData data = CapturePointData.get(world);
            data.update(pos.getX(), pos.getY(), pos.getZ(), pointName, ownerTeam, capturingTeam, captureProgress, radius);
            MyMod.NETWORK.sendToAll(new PacketCapturePointSync(pos.getX(), pos.getY(), pos.getZ(), pointName, ownerTeam,
                capturingTeam, captureProgress, radius));

            if (pve) {
                boolean alreadyZombie = "PvE".equals(ownerTeam);
                int viewDist = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getViewDistance();
                int guideRange = isMainBase ? Math.min(viewDist * 16, 256) : (radius + 16);
                AxisAlignedBB aabbZ = new AxisAlignedBB(
                    pos.getX() - guideRange, pos.getY() - guideRange, pos.getZ() - guideRange,
                    pos.getX() + guideRange + 1, pos.getY() + guideRange + 1, pos.getZ() + guideRange + 1);
                List<EntityZombie> zombies = world.getEntitiesWithinAABB(EntityZombie.class, aabbZ);
                EntityPlayer nearestPlayer = null;
                double nearestDist = Double.MAX_VALUE;
                AxisAlignedBB aabbP = new AxisAlignedBB(
                    pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                    pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
                List<EntityPlayer> nearPlayers = world.getEntitiesWithinAABB(EntityPlayer.class, aabbP);
                for (EntityPlayer pp : nearPlayers) {
                    double d = pp.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (d < nearestDist) {
                        nearestDist = d;
                        nearestPlayer = pp;
                    }
                }
                for (EntityZombie z : zombies) {
                    if (z.getAttackTarget() instanceof EntityPlayer) continue;
                    if (nearestPlayer != null) {
                        z.setAttackTarget(nearestPlayer);
                    } else if (!alreadyZombie) {
                        z.getNavigator().tryMoveToXYZ(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
                    }
                }
            }

            if (reviveMode && ownerTeam != null) {
                for (Object o : net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
                    if (o instanceof EntityPlayerMP) {
                        EntityPlayerMP mp = (EntityPlayerMP) o;
                        Team tt = getPlayerTeam(mp);
                        if (tt != null) {
                            String tname = tt.getName();
                            boolean match = "PVEHUMAN".equals(ownerTeam) || ownerTeam.equals(tname);
                            if (match) {
                                mp.setSpawnPoint(pos, true);
                            }
                        }
                    }
                }
            }
        }

        if (pve && "PvE".equals(ownerTeam)) {
            pveSpawnTimer++;
            if (pveSpawnTimer >= this.pveSpawnInterval * 20) {
                pveSpawnTimer = 0;
                AxisAlignedBB aabb2 = new AxisAlignedBB(
                    pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                    pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
                List<EntityZombie> near = world.getEntitiesWithinAABB(EntityZombie.class, aabb2);
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
        ScoreData sd = ScoreData.get(world);
        for (int i = 0; i < count; i++) {
            EntityZombie z = new EntityZombie(world);
            double ang = world.rand.nextDouble() * Math.PI * 2;
            double dist = 2 + world.rand.nextDouble() * radius;
            z.setLocationAndAngles(pos.getX() + 0.5 + Math.cos(ang) * dist, pos.getY() + 1.0,
                pos.getZ() + 0.5 + Math.sin(ang) * dist, world.rand.nextFloat() * 360.0F, 0.0F);
            try {
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(sd.zombieAttack);
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(sd.zombieSpeed);
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MAX_HEALTH).setBaseValue(sd.zombieHealth);
                z.setHealth((float) sd.zombieHealth);
            } catch (Exception e) {}
            world.spawnEntity(z);
        }
    }

    private Team getPlayerTeam(EntityPlayer p) {
        String name = p.getName();
        Team t = world.getScoreboard().getPlayersTeam(name);
        if (t != null) return t;
        for (Object o : world.getScoreboard().getTeams()) {
            if (o instanceof ScorePlayerTeam) {
                if (((ScorePlayerTeam) o).getMembershipCollection().contains(name)) {
                    return (Team) o;
                }
            }
        }
        return null;
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
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("radius", radius);
        nbt.setInteger("captureTime", captureTime);
        nbt.setString("pointName", pointName);
        nbt.setDouble("captureProgress", captureProgress);
        nbt.setBoolean("isMainBase", isMainBase);
        nbt.setBoolean("reviveMode", reviveMode);
        nbt.setBoolean("hasBeenZombieCaptured", hasBeenZombieCaptured);
        nbt.setInteger("pveSpawnInterval", pveSpawnInterval);
        nbt.setInteger("pveSpawnCount", pveSpawnCount);
        nbt.setInteger("pveMinCount", pveMinCount);
        nbt.setInteger("pveMaxCount", pveMaxCount);
        if (ownerTeam != null) nbt.setString("ownerTeam", ownerTeam);
        if (capturingTeam != null) nbt.setString("capturingTeam", capturingTeam);
        return nbt;
    
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        radius = nbt.getInteger("radius");
        captureTime = nbt.getInteger("captureTime");
        pointName = nbt.hasKey("pointName") ? nbt.getString("pointName") : "Point A";
        captureProgress = nbt.getDouble("captureProgress");
        isMainBase = nbt.getBoolean("isMainBase");
        reviveMode = nbt.getBoolean("reviveMode");
        hasBeenZombieCaptured = nbt.getBoolean("hasBeenZombieCaptured");
        pveSpawnInterval = nbt.hasKey("pveSpawnInterval") ? nbt.getInteger("pveSpawnInterval") : 20;
        pveSpawnCount = nbt.hasKey("pveSpawnCount") ? nbt.getInteger("pveSpawnCount") : 3;
        pveMinCount = nbt.hasKey("pveMinCount") ? nbt.getInteger("pveMinCount") : 0;
        pveMaxCount = nbt.hasKey("pveMaxCount") ? nbt.getInteger("pveMaxCount") : 20;
        ownerTeam = nbt.hasKey("ownerTeam") ? nbt.getString("ownerTeam") : null;
        capturingTeam = nbt.hasKey("capturingTeam") ? nbt.getString("capturingTeam") : null;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
