package com.myname.mymodid.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * v1.2.2：D点攻势方块统一物品形式。
 * 按住 Shift（高级 tooltip）时显示该方块的中文介绍（lang key: shift.desc.<简单名>）。
 */
public class ItemObjectiveDBlock extends ItemBlock {

    public ItemObjectiveDBlock(Block block) {
        super(block);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (flagIn.isAdvanced()) {
            try {
                String un = stack.getUnlocalizedName(); // 形如 tile.capturepoint
                if (un == null) return;
                String simple = un.substring(un.lastIndexOf('.') + 1);
                String desc = I18n.format("shift.desc." + simple);
                if (desc != null && !desc.startsWith("shift.desc.")) {
                    tooltip.add(TextFormatting.GRAY + desc);
                }
            } catch (Exception e) {
                // 绝不崩游戏
            }
        }
    }
}
