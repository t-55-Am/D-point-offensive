package com.myname.mymodid.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.util.DelayedTask;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockCapturePoint extends Block {
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;
    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;

    public BlockCapturePoint() {
        super(Material.iron);
        setBlockName("capturePoint");
        setCreativeTab(MyMod.WAR_TAB);
        setHardness(3.0F);
        setResistance(10.0F);
        setLightLevel(0.5F);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityCapturePoint();
    }

    @Override
    public boolean onBlockActivated(World world, final int x, final int y, final int z, final EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!player.capabilities.isCreativeMode) return true;
        if (player.openContainer != player.inventoryContainer) return true;
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityCapturePoint) {
                TileEntityCapturePoint cp = (TileEntityCapturePoint) te;
                net.minecraft.item.ItemStack held = player.getHeldItem();
                if (held != null && held.getItem() == net.minecraft.init.Items.emerald) {
                    cp.setMainBase(!cp.isMainBase());
                    world.markBlockForUpdate(x, y, z);
                    player.addChatMessage(new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.GOLD + net.minecraft.util.StatCollector.translateToLocal("dp.msg.base") + " " + (cp.isMainBase() ? net.minecraft.util.StatCollector.translateToLocal("dp.gui.on") : net.minecraft.util.StatCollector.translateToLocal("dp.gui.off"))));
                    return true;
                }
                if (held != null && held.getItem() == net.minecraft.init.Items.dye) {
                    boolean pve = com.myname.mymodid.score.ScoreData.get(world).pveEnabled;
                    String newOwner;
                    if (pve) {
                        newOwner = "PVEHUMAN".equals(cp.getOwnerTeam()) ? "PvE" : "PVEHUMAN";
                    } else {
                        newOwner = "Red".equals(cp.getOwnerTeam()) ? "Blue" : "Red";
                    }
                    cp.setOwnerTeam(newOwner);
                    world.markBlockForUpdate(x, y, z);
                    player.addChatMessage(new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.LIGHT_PURPLE + net.minecraft.util.StatCollector.translateToLocal("dp.msg.baseowner") + " " + newOwner));
                    return true;
                }
                if (held != null && held.getItem() == net.minecraft.init.Items.redstone) {
                    cp.setReviveMode(!cp.isReviveMode());
                    world.markBlockForUpdate(x, y, z);
                    player.addChatMessage(new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.AQUA + net.minecraft.util.StatCollector.translateToLocal("dp.msg.revive") + " " + (cp.isReviveMode() ? net.minecraft.util.StatCollector.translateToLocal("dp.gui.on") : net.minecraft.util.StatCollector.translateToLocal("dp.gui.off"))));
                    return true;
                }
            }
            DelayedTask.run(new Runnable() {
                public void run() {
                    if (player.openContainer == player.inventoryContainer && !player.isDead) {
                        player.openGui(MyMod.instance, 0, player.worldObj, x, y, z);
                    }
                }
            });
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {
        this.topIcon = reg.registerIcon("objective_d:common_top");
        this.sideIcon = reg.registerIcon("objective_d:capture_point_side");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int metadata) {
        return (side == 0 || side == 1) ? this.topIcon : this.sideIcon;
    }
}
