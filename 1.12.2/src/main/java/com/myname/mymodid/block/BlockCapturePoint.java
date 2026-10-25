package com.myname.mymodid.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.util.DelayedTask;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockCapturePoint extends Block {

    public BlockCapturePoint() {
        super(Material.IRON);
        setUnlocalizedName("capturepoint");
        setCreativeTab(MyMod.WAR_TAB);
        setHardness(3.0F);
        setResistance(10.0F);
        setLightLevel(0.5F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityCapturePoint();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, final EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.capabilities.isCreativeMode) return true;
        if (player.openContainer != player.inventoryContainer) return true;
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityCapturePoint) {
                TileEntityCapturePoint cp = (TileEntityCapturePoint) te;
                ItemStack held = player.getHeldItem(hand);
                if (!held.isEmpty() && held.getItem() == net.minecraft.init.Items.EMERALD) {
                    cp.setMainBase(!cp.isMainBase());
                    world.notifyBlockUpdate(pos, state, state, 3);
                    player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + net.minecraft.client.resources.I18n.format("dp.msg.base") + " " +
                        (cp.isMainBase() ? net.minecraft.client.resources.I18n.format("dp.gui.on") : net.minecraft.client.resources.I18n.format("dp.gui.off"))));
                    return true;
                }
                if (!held.isEmpty() && held.getItem() == net.minecraft.init.Items.DYE) {
                    boolean pve = com.myname.mymodid.score.ScoreData.get(world).pveEnabled;
                    String newOwner;
                    if (pve) {
                        newOwner = "PVEHUMAN".equals(cp.getOwnerTeam()) ? "PvE" : "PVEHUMAN";
                    } else {
                        newOwner = "Red".equals(cp.getOwnerTeam()) ? "Blue" : "Red";
                    }
                    cp.setOwnerTeam(newOwner);
                    world.notifyBlockUpdate(pos, state, state, 3);
                    player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + net.minecraft.client.resources.I18n.format("dp.msg.baseowner") + " " + newOwner));
                    return true;
                }
                if (!held.isEmpty() && held.getItem() == net.minecraft.init.Items.REDSTONE) {
                    cp.setReviveMode(!cp.isReviveMode());
                    world.notifyBlockUpdate(pos, state, state, 3);
                    player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + net.minecraft.client.resources.I18n.format("dp.msg.revive") + " " +
                        (cp.isReviveMode() ? net.minecraft.client.resources.I18n.format("dp.gui.on") : net.minecraft.client.resources.I18n.format("dp.gui.off"))));
                    return true;
                }
            }
            final BlockPos finalPos = pos;
            DelayedTask.run(new Runnable() {
                public void run() {
                    if (player.openContainer == player.inventoryContainer && !player.isDead) {
                        player.openGui(MyMod.instance, 0, player.world, finalPos.getX(), finalPos.getY(), finalPos.getZ());
                    }
                }
            });
        }
        return true;
    }
}
