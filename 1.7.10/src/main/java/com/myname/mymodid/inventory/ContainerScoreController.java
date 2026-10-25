package com.myname.mymodid.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class ContainerScoreController extends Container {
    public ContainerScoreController(InventoryPlayer playerInv) {
        // slot坐标全部设为-1000，移到屏幕外：不渲染、不可点击，但Container同步机制正常工作
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(playerInv, i, -1000, -1000));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, -1000, -1000));
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
