package com.myname.mymodid;

import net.minecraftforge.common.MinecraftForge;

import com.myname.mymodid.client.ClientEventHandler;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientEventHandler handler = new ClientEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(handler);
    }


    @Override
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(com.myname.mymodid.client.ClientEventHandler.keyQuickComm);
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(com.myname.mymodid.client.ClientEventHandler.keyQuickCommConfig);        
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(com.myname.mymodid.client.ClientEventHandler.keyChatGui);
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(com.myname.mymodid.client.ClientEventHandler.keyMark);


    }
}
