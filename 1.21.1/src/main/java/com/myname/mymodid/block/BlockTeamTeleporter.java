package com.myname.mymodid.block;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.client.gui.GuiTeamTeleporter;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;

public class BlockTeamTeleporter extends Block implements EntityBlock {
    public BlockTeamTeleporter() {
        super(Block.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 10.0F).sound(SoundType.METAL));
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityTeamTeleporter(pos, state); }
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == MyMod.TEAM_TELEPORTER_TE.get()
            ? (BlockEntityTicker<T>) (BlockEntityTicker<TileEntityTeamTeleporter>) TileEntityTeamTeleporter::tick : null;
    }
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileEntityTeamTeleporter te) te.onNeighborChanged();
    }
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new GuiTeamTeleporter(player, pos));
            return ItemInteractionResult.sidedSuccess(true);
        }
        return ItemInteractionResult.sidedSuccess(true);
    }
}
