package com.myname.mymodid.tileentity;

import com.myname.mymodid.MyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityScoreController extends BlockEntity {
    public TileEntityScoreController(BlockPos pos, BlockState state) {
        super(MyMod.SCORE_CONTROLLER_TE.get(), pos, state);
    }
}
