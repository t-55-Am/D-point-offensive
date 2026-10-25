package com.myname.mymodid.tileentity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

/**
 * 队伍配置器方块 TE（原战场入口，v1.1.8 改为全局队伍管理入口）。
 * 队伍数据全部存在世界级 TeamData 里，本 TE 不再存红/蓝字段、不再红石激活加队伍。
 * 旧存档（redTeamName/blueTeamName）首次加载时迁移进 TeamData。
 */
public class TileEntityBattlePortal extends TileEntity {

    @Override
    public void updateEntity() {
        // 空实现：配置器不再有任何红石/队伍逻辑
    }

    /** 旧版本 NBT 迁移到 TeamData（服务端），红/蓝队伍及出生点/背包保留 */
    public void migrateLegacy(NBTTagCompound nbt) {
        try {
            World w = worldObj;
            if (w == null || w.isRemote) return;
            if (!nbt.hasKey("redTeamName")) return;
            TeamData data = TeamData.get(w);
            if (data == null) return;

            TeamConfig r = data.find("Red");
            if (r == null) data.addTeam("Red", 0);
            r = data.find("Red");
            if (r != null) {
                r.name = nbt.hasKey("redTeamName") ? nbt.getString("redTeamName") : "Red";
                r.maxPlayers = Math.max(1, nbt.getInteger("redMaxPlayers"));
                r.hasSpawn = nbt.getBoolean("redHasSpawn");
                r.spawnX = nbt.getInteger("redSpawnX");
                r.spawnY = nbt.getInteger("redSpawnY");
                r.spawnZ = nbt.getInteger("redSpawnZ");
                ItemStack[] bp = readBackpack(nbt, "redBackpack");
                if (bp != null) r.backpack = bp;
            }
            TeamConfig b = data.find("Blue");
            if (b == null) data.addTeam("Blue", 1);
            b = data.find("Blue");
            if (b != null) {
                b.name = nbt.hasKey("blueTeamName") ? nbt.getString("blueTeamName") : "Blue";
                b.maxPlayers = Math.max(1, nbt.getInteger("blueMaxPlayers"));
                b.hasSpawn = nbt.getBoolean("blueHasSpawn");
                b.spawnX = nbt.getInteger("blueSpawnX");
                b.spawnY = nbt.getInteger("blueSpawnY");
                b.spawnZ = nbt.getInteger("blueSpawnZ");
                ItemStack[] bp = readBackpack(nbt, "blueBackpack");
                if (bp != null) b.backpack = bp;
            }
            data.markDirty();
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }

    private static ItemStack[] readBackpack(NBTTagCompound nbt, String key) {
        NBTTagList list = nbt.getTagList(key, 10);
        if (list.tagCount() == 0) return null;
        ItemStack[] arr = new ItemStack[40];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("slot");
            if (slot >= 0 && slot < 40) {
                arr[slot] = ItemStack.loadItemStackFromNBT(tag);
            }
        }
        return arr;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        // 旧存档迁移（服务端首加载），迁移后旧 key 不再写入 NBT
        migrateLegacy(nbt);
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
