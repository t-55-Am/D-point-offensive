package com.myname.mymodid;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.myname.mymodid.client.ClientEventHandler;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientEventHandler handler = new ClientEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(ClientEventHandler.keyQuickComm);
        ClientRegistry.registerKeyBinding(ClientEventHandler.keyQuickCommConfig);
        ClientRegistry.registerKeyBinding(ClientEventHandler.keyChatGui);
        ClientRegistry.registerKeyBinding(ClientEventHandler.keyMark);
    }
}
