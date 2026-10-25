package com.myname.mymodid.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/**
 * v1.1.15：D点攻势方块统一物品形式。
 * 按住 Shift（高级 tooltip）时显示该方块的中文介绍（lang key: shift.desc.<简单名>）。
 */
public class ItemObjectiveDBlock extends ItemBlock {

    public ItemObjectiveDBlock(Block block) {
        super(block);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        super.addInformation(stack, player, list, advanced);
        if (!advanced) return;
        try {
            String un = stack.getUnlocalizedName(); // 形如 tile.objective_d.capturePoint
            if (un == null) return;
            String simple = un.substring(un.lastIndexOf('.') + 1);
            String desc = StatCollector.translateToLocal("shift.desc." + simple);
            if (desc != null && !desc.startsWith("shift.desc.")) {
                list.add(EnumChatFormatting.GRAY + desc);
            }
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }
}
