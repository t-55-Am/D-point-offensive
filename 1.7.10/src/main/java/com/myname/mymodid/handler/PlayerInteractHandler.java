package com.myname.mymodid.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class PlayerInteractHandler {
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world.isRemote) return;
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
            && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        EntityPlayer player = event.entityPlayer;
        if (player == null) return;
        // GUI打开中拦截右键，关闭后立即恢复
        if (player.openContainer != player.inventoryContainer) {
            event.setCanceled(true);
        }
    }
}
