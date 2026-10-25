package com.myname.mymodid.tileentity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

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
        if (worldObj != null && !worldObj.isRemote) {
            net.minecraft.block.Block b = worldObj.getBlock(xCoord, yCoord, zCoord);
            if (!(b instanceof BlockCapturePoint)) {
                CapturePointData.get(worldObj).remove(xCoord, yCoord, zCoord);
                MyMod.NETWORK.sendToAll(new PacketCapturePointSyncRemove(xCoord, yCoord, zCoord));
            }
        }
        super.invalidate();
    }

    public void resetToNeutral() {
        captureProgress = 0;
        ownerTeam = null;
        capturingTeam = null;
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            // 立即同步到客户端，不依赖gameStarted状态
            CapturePointData data = CapturePointData.get(worldObj);
            data.update(xCoord, yCoord, zCoord, pointName, ownerTeam, capturingTeam, captureProgress, radius);
            MyMod.NETWORK.sendToAll(new PacketCapturePointSync(xCoord, yCoord, zCoord, pointName, ownerTeam,
                capturingTeam, captureProgress, radius));
        }
    }


    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        ScoreData sd = ScoreData.get(worldObj);

        // 没开始游戏时：占点、刷丧尸、数据同步全暂停
        if (!sd.gameStarted) return;

        if (isMainBase && ownerTeam == null) {
            ownerTeam = sd.pveEnabled ? "PVEHUMAN" : "Red";
        }

        boolean pve = sd.pveEnabled;

        if (MyModConfig.enableCapture) {
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                xCoord - radius, yCoord - radius, zCoord - radius,
                xCoord + radius + 1, yCoord + radius + 1, zCoord + radius + 1);
            @SuppressWarnings("unchecked")
            List<EntityPlayer> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class, aabb);
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
                    String name = team.getRegisteredName();
                    Integer c = teamCounts.get(name);
                    teamCounts.put(name, c == null ? 1 : c + 1);
                }
            }

            double ratePerTick = 100.0 / (captureTime * 20.0);

            if (teamCounts.isEmpty()) {
                boolean zombieHandled = false;
                if (pve) {
                    @SuppressWarnings("unchecked")
                    List<EntityZombie> zombies = worldObj.getEntitiesWithinAABB(EntityZombie.class, aabb);
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
                                worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "mob.zombie.infect", 1.0F, 1.0F);
                                net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().sendChatMsg(
                                    new net.minecraft.util.ChatComponentText(
                                        net.minecraft.util.EnumChatFormatting.DARK_RED + String.format(net.minecraft.util.StatCollector.translateToLocal("dp.msg.zombie_capture"), pointName)));
                                AxisAlignedBB aabbN = AxisAlignedBB.getBoundingBox(
                                    xCoord - radius, yCoord - radius, zCoord - radius,
                                    xCoord + radius + 1, yCoord + radius + 1, zCoord + radius + 1);
                                @SuppressWarnings("unchecked")
                                List<EntityZombie> nearZ = worldObj.getEntitiesWithinAABB(EntityZombie.class, aabbN);
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
                        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.levelup", 1.0F, 1.0F);
                        String showName = "PVEHUMAN".equals(teamName) ? net.minecraft.util.StatCollector.translateToLocal("dp.msg.humans") : teamName;
                        net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().sendChatMsg(
                            new net.minecraft.util.ChatComponentText(
                                net.minecraft.util.EnumChatFormatting.GOLD + String.format(net.minecraft.util.StatCollector.translateToLocal("dp.msg.captured"), pointName, showName)));
                        for (EntityPlayer p2 : players) {
                            Team t2 = getPlayerTeam(p2);
                            boolean shouldAdd = false;
                            if (pve) {
                                shouldAdd = (t2 != null);
                            } else {
                                shouldAdd = (t2 != null && teamName.equals(t2.getRegisteredName()));
                            }
                            if (shouldAdd) {
                                PlayerStats.addCap(p2.getCommandSenderName());
                            }
                        }
                        ScoreTickHandler.syncAllStats(worldObj);

                        // ===== 修复：占点完成瞬间掉分（PvP）——占领成功立即扣对手分数，分数条马上可见 =====
                        if (!pve) {
                            ScoreData sdd = ScoreData.get(worldObj);
                            boolean newOwnerRed = ScoreTickHandler.isRedOwner(worldObj, teamName);
                            if (newOwnerRed) {
                                sdd.blueScore = Math.max(0, sdd.blueScore - sdd.scorePerSecond * 10);
                            } else {
                                sdd.redScore = Math.max(0, sdd.redScore - sdd.scorePerSecond * 10);
                            }
                            sdd.markDirty();
                            MyMod.NETWORK.sendToAll(new com.myname.mymodid.network.PacketScoreUpdate(sdd.redScore, sdd.blueScore));
                        }

                        if (isMainBase) {
                            ScoreData sdd = ScoreData.get(worldObj);
                            if (pve) {
                                if ("PvE".equals(teamName)) {
                                    sdd.redScore = 0;
                                    sdd.blueScore = 0;
                                    net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().sendChatMsg(
                                        new net.minecraft.util.ChatComponentText(
                                            net.minecraft.util.EnumChatFormatting.DARK_RED + net.minecraft.util.StatCollector.translateToLocal("dp.msg.base_zombie")));
                                } else {
                                    net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().sendChatMsg(
                                        new net.minecraft.util.ChatComponentText(
                                            net.minecraft.util.EnumChatFormatting.GREEN + net.minecraft.util.StatCollector.translateToLocal("dp.msg.base_human")));
                                }
                            } else {
                                String lower = teamName.toLowerCase();
                                if (lower.contains("red") || teamName.contains("红")) {
                                    sdd.blueScore = 0;
                                    net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().sendChatMsg(
                                        new net.minecraft.util.ChatComponentText(
                                            net.minecraft.util.EnumChatFormatting.RED + net.minecraft.util.StatCollector.translateToLocal("dp.msg.base_red")));
                                } else {
                                    sdd.redScore = 0;
                                    net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().sendChatMsg(
                                        new net.minecraft.util.ChatComponentText(
                                            net.minecraft.util.EnumChatFormatting.BLUE + net.minecraft.util.StatCollector.translateToLocal("dp.msg.base_blue")));
                                }
                            }
                            sdd.markDirty();
                            MyMod.NETWORK.sendToAll(new com.myname.mymodid.network.PacketScoreUpdate(sdd.redScore, sdd.blueScore));
                        }
                    }
                }
            }
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        reportTimer++;
        if (reportTimer >= 10) {
            reportTimer = 0;
            CapturePointData data = CapturePointData.get(worldObj);
            data.update(xCoord, yCoord, zCoord, pointName, ownerTeam, capturingTeam, captureProgress, radius);
            MyMod.NETWORK.sendToAll(new PacketCapturePointSync(xCoord, yCoord, zCoord, pointName, ownerTeam,
                capturingTeam, captureProgress, radius));

            if (pve) {
                boolean alreadyZombie = "PvE".equals(ownerTeam);
                int viewDist = net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().getViewDistance();
                int guideRange = isMainBase ? Math.min(viewDist * 16, 256) : (radius + 16);
                AxisAlignedBB aabbZ = AxisAlignedBB.getBoundingBox(
                    xCoord - guideRange, yCoord - guideRange, zCoord - guideRange,
                    xCoord + guideRange + 1, yCoord + guideRange + 1, zCoord + guideRange + 1);
                @SuppressWarnings("unchecked")
                List<EntityZombie> zombies = worldObj.getEntitiesWithinAABB(EntityZombie.class, aabbZ);
                EntityPlayer nearestPlayer = null;
                double nearestDist = Double.MAX_VALUE;
                AxisAlignedBB aabbP = AxisAlignedBB.getBoundingBox(
                    xCoord - radius, yCoord - radius, zCoord - radius,
                    xCoord + radius + 1, yCoord + radius + 1, zCoord + radius + 1);
                @SuppressWarnings("unchecked")
                List<EntityPlayer> nearPlayers = worldObj.getEntitiesWithinAABB(EntityPlayer.class, aabbP);
                for (EntityPlayer pp : nearPlayers) {
                    double d = pp.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
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
                        z.getNavigator().tryMoveToXYZ(xCoord + 0.5, yCoord, zCoord + 0.5, 1.0);
                    }
                }
            }

            if (reviveMode && ownerTeam != null) {
                for (Object o : net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                    if (o instanceof net.minecraft.entity.player.EntityPlayerMP) {
                        net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) o;
                        Team tt = getPlayerTeam(mp);
                        if (tt != null) {
                            String tname = tt.getRegisteredName();
                            boolean match = "PVEHUMAN".equals(ownerTeam) || ownerTeam.equals(tname);
                            if (match) {
                                mp.setSpawnChunk(new net.minecraft.util.ChunkCoordinates(xCoord, yCoord, zCoord), true);
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
                AxisAlignedBB aabb2 = AxisAlignedBB.getBoundingBox(
                    xCoord - radius, yCoord - radius, zCoord - radius,
                    xCoord + radius + 1, yCoord + radius + 1, zCoord + radius + 1);
                @SuppressWarnings("unchecked")
                List<EntityZombie> near = worldObj.getEntitiesWithinAABB(EntityZombie.class, aabb2);
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
        ScoreData sd = ScoreData.get(worldObj);
        for (int i = 0; i < count; i++) {
            EntityZombie z = new EntityZombie(worldObj);
            double ang = worldObj.rand.nextDouble() * Math.PI * 2;
            double dist = 2 + worldObj.rand.nextDouble() * radius;
            z.setLocationAndAngles(xCoord + 0.5 + Math.cos(ang) * dist, yCoord + 1.0,
                zCoord + 0.5 + Math.sin(ang) * dist, worldObj.rand.nextFloat() * 360.0F, 0.0F);
            try {
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage).setBaseValue(sd.zombieAttack);
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).setBaseValue(sd.zombieSpeed);
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth).setBaseValue(sd.zombieHealth);
                z.setHealth((float) sd.zombieHealth);
            } catch (Exception e) {}
            worldObj.spawnEntityInWorld(z);
        }
    }

    private Team getPlayerTeam(EntityPlayer p) {
        String name = p.getCommandSenderName();
        Team t = worldObj.getScoreboard().getPlayersTeam(name);
        if (t != null) return t;
        for (Object o : worldObj.getScoreboard().getTeams()) {
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
    public void writeToNBT(NBTTagCompound nbt) {
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
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
