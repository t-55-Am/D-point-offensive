package com.myname.mymodid.block;

import com.myname.mymodid.client.gui.GuiBattlePortal;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class BlockBattlePortal extends Block implements EntityBlock {
    public BlockBattlePortal() {
        super(Block.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 10.0F).sound(SoundType.METAL));
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityBattlePortal(pos, state); }
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new GuiBattlePortal(player, pos));
            return ItemInteractionResult.sidedSuccess(true);
        }
        return ItemInteractionResult.sidedSuccess(true);
    }
}
