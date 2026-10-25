package com.myname.mymodid.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

import com.myname.mymodid.config.MyModConfig;

public class MyModGuiConfig extends GuiConfig {
    public MyModGuiConfig(GuiScreen parent) {
        super(parent, getConfigElements(), "mymodid", false, false, "D-Point Offensive Config");
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<IConfigElement>();
        list.add(new ConfigElement(MyModConfig.config.getCategory("general")));
        list.add(new ConfigElement(MyModConfig.config.getCategory("display")));
        return list;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        MyModConfig.load();
    }
}
