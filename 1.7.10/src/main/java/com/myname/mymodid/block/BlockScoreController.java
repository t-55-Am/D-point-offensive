package com.myname.mymodid.block;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.tileentity.TileEntityScoreController;
import com.myname.mymodid.util.DelayedTask;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockScoreController extends BlockContainer {
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;
    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;

    public BlockScoreController() {
        super(Material.iron);
        setBlockName("scoreController");
        setCreativeTab(MyMod.WAR_TAB);
        setHardness(2.0F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityScoreController();
    }

    @Override
    public boolean onBlockActivated(World world, final int x, final int y, final int z, final EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (player.openContainer != player.inventoryContainer) return true;
        if (!player.isSneaking()) {
            DelayedTask.run(new Runnable() {
                public void run() {
                    if (player.openContainer == player.inventoryContainer && !player.isDead) {
                        player.openGui(MyMod.instance, 1, player.worldObj, x, y, z);
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
        this.sideIcon = reg.registerIcon("objective_d:score_controller_side");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int metadata) {
        return (side == 0 || side == 1) ? this.topIcon : this.sideIcon;
    }
}
