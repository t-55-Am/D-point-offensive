package com.myname.mymodid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.util.DelayedTask;

import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

public class BlockBattlePortal extends BlockContainer {

    public BlockBattlePortal() {
        super(Material.IRON);
        setUnlocalizedName("battleportal");
        setCreativeTab(MyMod.WAR_TAB);
        setHardness(2.0f);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBattlePortal();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntityBattlePortal) {
            ((TileEntityBattlePortal) world.getTileEntity(pos)).onNeighborChanged();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, final EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.capabilities.isCreativeMode) return true;
        if (player.openContainer != player.inventoryContainer) return true;
        if (!world.isRemote) {
            final BlockPos finalPos = pos;
            DelayedTask.run(new Runnable() {
                public void run() {
                    if (player.openContainer == player.inventoryContainer && !player.isDead) {
                        FMLNetworkHandler.openGui(player, MyMod.instance, 2, player.world, finalPos.getX(), finalPos.getY(), finalPos.getZ());
                    }
                }
            });
        }
        return true;
    }
}
