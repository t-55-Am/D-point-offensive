package com.myname.mymodid;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class WarCreativeTab extends CreativeTabs {

    public static final WarCreativeTab INSTANCE = new WarCreativeTab();

    public WarCreativeTab() {
        super("warthunder");
    }

    @Override
    public ItemStack getTabIconItem() {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(MyMod.MODID, "capturepoint"));
        return item != null ? new ItemStack(item) : new ItemStack(Blocks.STONE);
    }
}
