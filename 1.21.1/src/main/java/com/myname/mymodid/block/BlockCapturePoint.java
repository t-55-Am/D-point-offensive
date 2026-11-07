package com.myname.mymodid.block;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.client.gui.GuiCapturePoint;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.network.SPayloads.CapturePointSyncRemovePayload;
import com.myname.mymodid.score.CapturePointData;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class BlockCapturePoint extends Block implements EntityBlock {

    public BlockCapturePoint() {
        super(Block.Properties.of().mapColor(MapColor.METAL)
            .strength(3.0F, 10.0F).lightLevel(s -> 8).sound(SoundType.METAL));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCapturePoint(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() == this && newState.getBlock() != this) {
            CapturePointData.get(level).remove(pos.getX(), pos.getY(), pos.getZ());
            Network.sendToAllPlayers(new CapturePointSyncRemovePayload(pos.getX(), pos.getY(), pos.getZ()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == MyMod.CAPTURE_POINT_TE.get()
            ? ticker((BlockEntityTicker<TileEntityCapturePoint>) TileEntityCapturePoint::tick) : null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> ticker(BlockEntityTicker<? extends BlockEntity> t) {
        return (BlockEntityTicker<T>) t;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isCreative()) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new GuiCapturePoint(player, pos));
            return ItemInteractionResult.sidedSuccess(true);
        }
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileEntityCapturePoint cp) {
            ItemStack held = player.getMainHandItem();
            if (held.is(Items.EMERALD)) {
                cp.setMainBase(!cp.isMainBase());
                level.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.translatable("dp.msg.base").withStyle(ChatFormatting.GOLD)
                    .append(" ").append(Component.translatable(cp.isMainBase() ? "dp.gui.on" : "dp.gui.off")));
                return ItemInteractionResult.sidedSuccess(true);
            }
            if (held.is(Items.RED_DYE)) {
                boolean pve = ScoreData.get((ServerLevel) level).pveEnabled;
                String newOwner;
                if (pve) { newOwner = "PVEHUMAN".equals(cp.getOwnerTeam()) ? "PvE" : "PVEHUMAN"; }
                else { newOwner = "Red".equals(cp.getOwnerTeam()) ? "Blue" : "Red"; }
                cp.setOwnerTeam(newOwner);
                level.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.translatable("dp.msg.baseowner").withStyle(ChatFormatting.LIGHT_PURPLE).append(" " + newOwner));
                return ItemInteractionResult.sidedSuccess(true);
            }
            if (held.is(Items.REDSTONE)) {
                cp.setReviveMode(!cp.isReviveMode());
                level.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.translatable("dp.msg.revive").withStyle(ChatFormatting.AQUA)
                    .append(" ").append(Component.translatable(cp.isReviveMode() ? "dp.gui.on" : "dp.gui.off")));
                return ItemInteractionResult.sidedSuccess(true);
            }
        }
        return ItemInteractionResult.sidedSuccess(true);
    }
}
