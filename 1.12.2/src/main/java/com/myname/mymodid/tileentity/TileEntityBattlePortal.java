package com.myname.mymodid.tileentity;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import com.myname.mymodid.config.MyModConfig;

public class TileEntityBattlePortal extends TileEntity {
    public String redTeamName = "Red";
    public int redMaxPlayers = 10;
    public int redSpawnX, redSpawnY, redSpawnZ;
    public boolean redHasSpawn = false;
    public ItemStack[] redBackpack = null;
    public String blueTeamName = "Blue";
    public int blueMaxPlayers = 10;
    public int blueSpawnX, blueSpawnY, blueSpawnZ;
    public boolean blueHasSpawn = false;
    public ItemStack[] blueBackpack = null;
    public int mode = 0;
    private boolean wasRedPowered = false;
    private boolean wasBluePowered = false;
    private int redCooldown = 0;
    private int blueCooldown = 0;

    public void onNeighborChanged() {
        if (world != null && !world.isRemote) checkSignals();
    }

    public void update() {
        if (world == null || world.isRemote) return;
        checkSignals();
    }

    // 修复：红蓝独立边沿触发，两端接同一信号源时两队都能激活（不再红优先吞掉蓝队）
    private void checkSignals() {
        boolean red = isSidePowered(-1, 0, 0);
        boolean blue = isSidePowered(1, 0, 0);
        if (redCooldown > 0) redCooldown--;
        if (blueCooldown > 0) blueCooldown--;
        if (red && !wasRedPowered && redCooldown <= 0) {
            handleTrigger(0);
            redCooldown = 5;
        }
        if (blue && !wasBluePowered && blueCooldown <= 0) {
            handleTrigger(1);
            blueCooldown = 5;
        }
        wasRedPowered = red;
        wasBluePowered = blue;
    }

    private boolean isSidePowered(int dx, int dy, int dz) {
        World w = world;
        int x = pos.getX() + dx, y = pos.getY() + dy, z = pos.getZ() + dz;
        if (w.isBlockPowered(new net.minecraft.util.math.BlockPos(x, y, z))) return true;
        return false;
    }

    private void handleTrigger(int teamIndex) {
        if (!MyModConfig.enableTeam) return;
        try {
            Scoreboard sb = world.getScoreboard();
            String teamName = teamIndex == 0 ? redTeamName : blueTeamName;
            int maxP = teamIndex == 0 ? redMaxPlayers : blueMaxPlayers;
            ItemStack[] bp = teamIndex == 0 ? redBackpack : blueBackpack;
            boolean hasSpawn = teamIndex == 0 ? redHasSpawn : blueHasSpawn;
            int sx = teamIndex == 0 ? redSpawnX : blueSpawnX;
            int sy = teamIndex == 0 ? redSpawnY : blueSpawnY;
            int sz = teamIndex == 0 ? redSpawnZ : blueSpawnZ;

            AxisAlignedBB aabb = new AxisAlignedBB(
                pos.getX() - 3, pos.getY() - 3, pos.getZ() - 3,
                pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
            List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, aabb);
            for (EntityPlayer p : players) {
                if (!isInTeam(sb, p.getName(), teamName)) {
                    addPlayerToTeamSafe(sb, p.getName(), teamName);
                }
                if (countTeam(sb, teamName) > maxP) {
                    p.sendMessage(new TextComponentTranslation("msg.team.full", teamName));
                    continue;
                }
                if (mode == 1 && bp != null) {
                    copyBackpack(p, bp);
                }
                if (hasSpawn) {
                    p.setPositionAndUpdate(sx + 0.5, sy + 0.5, sz + 0.5);
                    p.sendMessage(new TextComponentTranslation("msg.enter"));
                }
            }
        } catch (Exception e) {
        }
    }

    private void addPlayerToTeamSafe(Scoreboard sb, String playerName, String teamName) {
        if (sb.getTeam(teamName) == null) {
            try {
                sb.createTeam(teamName);
            } catch (Exception e) {
                return;
            }
        }
        String[] names = {"addPlayerToTeam", "func_151392_a"};
        for (String m : names) {
            try {
                Method method = Scoreboard.class.getMethod(m, String.class, String.class);
                method.invoke(sb, playerName, teamName);
                return;
            } catch (Exception ignored) {
            }
        }
        try {
            Team t = sb.getTeam(teamName);
            if (t != null) {
                ((ScorePlayerTeam) t).getMembershipCollection().add(playerName);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isInTeam(Scoreboard sb, String playerName, String teamName) {
        Team t = sb.getTeam(teamName);
        if (t == null) return false;
        if (sb.getPlayersTeam(playerName) != null && sb.getPlayersTeam(playerName).getName().equals(teamName)) {
            return true;
        }
        try {
            return ((ScorePlayerTeam) t).getMembershipCollection().contains(playerName);
        } catch (Exception e) {
            return false;
        }
    }

    private int countTeam(Scoreboard sb, String name) {
        Team t = sb.getTeam(name);
        if (t == null) return 0;
        try {
            return ((ScorePlayerTeam) t).getMembershipCollection().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void copyBackpack(EntityPlayer p, ItemStack[] template) {
        for (int i = 0; i < 36; i++) {
            p.inventory.setInventorySlotContents(i, template[i] == null ? ItemStack.EMPTY : template[i].copy());
        }
        for (int i = 0; i < 4; i++) {
            p.inventory.setInventorySlotContents(36 + i, template[36 + i] == null ? ItemStack.EMPTY : template[36 + i].copy());
        }
        p.inventoryContainer.detectAndSendChanges();
    }

    private void writeBackpack(NBTTagCompound nbt, String key, ItemStack[] arr) {
        NBTTagList list = new NBTTagList();
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null && !arr[i].isEmpty()) {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setByte("slot", (byte) i);
                    arr[i].writeToNBT(tag);
                    list.appendTag(tag);
                }
            }
        }
        nbt.setTag(key, list);
    }

    private ItemStack[] readBackpack(NBTTagCompound nbt, String key) {
        NBTTagList list = nbt.getTagList(key, 10);
        if (list.tagCount() == 0) return null;
        ItemStack[] arr = new ItemStack[40];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("slot");
            if (slot >= 0 && slot < 40) {
                arr[slot] = new ItemStack(tag);
            }
        }
        return arr;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("redTeamName", redTeamName);
        nbt.setInteger("redMaxPlayers", redMaxPlayers);
        nbt.setBoolean("redHasSpawn", redHasSpawn);
        nbt.setInteger("redSpawnX", redSpawnX);
        nbt.setInteger("redSpawnY", redSpawnY);
        nbt.setInteger("redSpawnZ", redSpawnZ);
        writeBackpack(nbt, "redBackpack", redBackpack);
        nbt.setString("blueTeamName", blueTeamName);
        nbt.setInteger("blueMaxPlayers", blueMaxPlayers);
        nbt.setBoolean("blueHasSpawn", blueHasSpawn);
        nbt.setInteger("blueSpawnX", blueSpawnX);
        nbt.setInteger("blueSpawnY", blueSpawnY);
        nbt.setInteger("blueSpawnZ", blueSpawnZ);
        writeBackpack(nbt, "blueBackpack", blueBackpack);
        nbt.setInteger("mode", mode);
        return nbt;
    
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        redTeamName = nbt.hasKey("redTeamName") ? nbt.getString("redTeamName") : "Red";
        redMaxPlayers = nbt.hasKey("redMaxPlayers") ? nbt.getInteger("redMaxPlayers") : 10;
        if (redMaxPlayers <= 0) redMaxPlayers = 10;
        redHasSpawn = nbt.getBoolean("redHasSpawn");
        redSpawnX = nbt.getInteger("redSpawnX");
        redSpawnY = nbt.getInteger("redSpawnY");
        redSpawnZ = nbt.getInteger("redSpawnZ");
        redBackpack = readBackpack(nbt, "redBackpack");
        blueTeamName = nbt.hasKey("blueTeamName") ? nbt.getString("blueTeamName") : "Blue";
        blueMaxPlayers = nbt.hasKey("blueMaxPlayers") ? nbt.getInteger("blueMaxPlayers") : 10;
        if (blueMaxPlayers <= 0) blueMaxPlayers = 10;
        blueHasSpawn = nbt.getBoolean("blueHasSpawn");
        blueSpawnX = nbt.getInteger("blueSpawnX");
        blueSpawnY = nbt.getInteger("blueSpawnY");
        blueSpawnZ = nbt.getInteger("blueSpawnZ");
        blueBackpack = readBackpack(nbt, "blueBackpack");
        mode = nbt.getInteger("mode");
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
