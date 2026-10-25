package com.myname.mymodid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.util.DelayedTask;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockBattlePortal extends BlockContainer {
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;
    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;

    public BlockBattlePortal() {
        super(Material.iron);
        setBlockName("battlePortal");
        setCreativeTab(MyMod.WAR_TAB);
        setHardness(2.0f);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBattlePortal();
    }

    @Override
    public boolean onBlockActivated(World world, final int x, final int y, final int z, final EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!player.capabilities.isCreativeMode) return true;
        if (player.openContainer != player.inventoryContainer) return true;
        if (!world.isRemote) {
            DelayedTask.run(new Runnable() {
                public void run() {
                    if (player.openContainer == player.inventoryContainer && !player.isDead) {
                        FMLNetworkHandler.openGui(player, MyMod.instance, 2, player.worldObj, x, y, z);
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
        this.sideIcon = reg.registerIcon("objective_d:battle_portal_side");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int metadata) {
        return (side == 0 || side == 1) ? this.topIcon : this.sideIcon;
    }
}
