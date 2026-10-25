package com.myname.mymodid.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerInteractHandler {
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) return;
        // GUI打开中拦截右键，关闭后立即恢复
        if (player.openContainer != player.inventoryContainer) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) return;
        // GUI打开中拦截右键，关闭后立即恢复
        if (player.openContainer != player.inventoryContainer) {
            event.setCanceled(true);
        }
    }
}
