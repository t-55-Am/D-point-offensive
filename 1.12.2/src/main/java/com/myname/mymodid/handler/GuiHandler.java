package com.myname.mymodid.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.myname.mymodid.client.gui.GuiBattlePortal;
import com.myname.mymodid.client.gui.GuiCapturePoint;
import com.myname.mymodid.client.gui.GuiScoreController;
import com.myname.mymodid.client.gui.GuiTeamTeleporter;
import com.myname.mymodid.inventory.ContainerBattlePortal;
import com.myname.mymodid.inventory.ContainerCapturePoint;
import com.myname.mymodid.inventory.ContainerScoreController;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.tileentity.TileEntityScoreController;

import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == 0) return new ContainerCapturePoint();
        if (ID == 1) return new ContainerScoreController(player.inventory);
        if (ID == 2) return new ContainerBattlePortal();
        if (ID == 3) return new ContainerBattlePortal();
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
        if (ID == 0) return new GuiCapturePoint(player.inventory, (TileEntityCapturePoint) tile);
        if (ID == 1) return new GuiScoreController((TileEntityScoreController) tile);
        if (ID == 2) return new GuiBattlePortal(x, y, z);
        if (ID == 3) return new GuiTeamTeleporter(x, y, z);
        return null;
    }
}
