package com.myname.mymodid.block;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.tileentity.TileEntityScoreController;
import com.myname.mymodid.util.DelayedTask;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockScoreController extends BlockContainer {

    public BlockScoreController() {
        super(Material.IRON);
        setUnlocalizedName("scorecontroller");
        setCreativeTab(MyMod.WAR_TAB);
        setHardness(2.0F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityScoreController();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, final EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (player.openContainer != player.inventoryContainer) return true;
        if (!player.isSneaking()) {
            final BlockPos finalPos = pos;
            DelayedTask.run(new Runnable() {
                public void run() {
                    if (player.openContainer == player.inventoryContainer && !player.isDead) {
                        player.openGui(MyMod.instance, 1, player.world, finalPos.getX(), finalPos.getY(), finalPos.getZ());
                    }
                }
            });
        }
        return true;
    }
}
