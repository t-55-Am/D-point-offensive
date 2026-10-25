package com.myname.mymodid.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerBattlePortal extends Container {

    public ContainerBattlePortal() {
        // 纯配置 GUI，不需要槽位
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
