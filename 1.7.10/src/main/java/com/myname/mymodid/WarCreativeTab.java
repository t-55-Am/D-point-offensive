package com.myname.mymodid;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import cpw.mods.fml.common.registry.GameRegistry;

public class WarCreativeTab extends CreativeTabs {

    public static final WarCreativeTab INSTANCE = new WarCreativeTab();

    public WarCreativeTab() {
        super("warthunder");
    }

    @Override
    public Item getTabIconItem() {
        Item item = GameRegistry.findItem("mymodid", "capturePoint");
        return item != null ? item : Item.getItemFromBlock(Blocks.stone);
    }
}
