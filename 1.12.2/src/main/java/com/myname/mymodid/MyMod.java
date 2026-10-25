package com.myname.mymodid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.myname.mymodid.block.BlockBattlePortal;
import com.myname.mymodid.block.BlockCapturePoint;
import com.myname.mymodid.block.BlockScoreController;
import com.myname.mymodid.block.BlockTeamTeleporter;
import com.myname.mymodid.handler.GuiHandler;
import com.myname.mymodid.network.PacketBattleConfig;
import com.myname.mymodid.network.PacketCapturePointConfig;
import com.myname.mymodid.network.PacketCapturePointFlags;
import com.myname.mymodid.network.PacketCapturePointSync;
import com.myname.mymodid.network.PacketCapturePointSyncRemove;
import com.myname.mymodid.network.PacketPlayerStats;
import com.myname.mymodid.network.PacketSaveBackpack;
import com.myname.mymodid.network.PacketScoreConfig;
import com.myname.mymodid.network.PacketChatMessage;
import com.myname.mymodid.network.PacketChatReceive;
import com.myname.mymodid.network.PacketScoreUpdate;
import com.myname.mymodid.network.PacketSetSpawn;
import com.myname.mymodid.score.ScoreTickHandler;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.tileentity.TileEntityScoreController;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = MyMod.MODID, version = "1.2.2", name = "D点攻势", acceptedMinecraftVersions = "[1.12.2]")
public class MyMod {
    public static final String MODID = "objective_d";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static MyMod instance;

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    // 创意标签页
    public static final CreativeTabs WAR_TAB = new CreativeTabs("war_tab") {
        @Override
        public ItemStack getTabIconItem() {
            net.minecraft.block.Block block = net.minecraft.block.Block.REGISTRY.getObject(new ResourceLocation(MODID, "capturepoint"));
            if (block != null) {
                return new ItemStack(Item.getItemFromBlock(block));
            }
            return ItemStack.EMPTY;
        }
    };

    @SidedProxy(clientSide = "com.myname.mymodid.ClientProxy", serverSide = "com.myname.mymodid.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);

        // 注册方块
        BlockCapturePoint capturePoint = new BlockCapturePoint();
        capturePoint.setRegistryName(new ResourceLocation(MODID, "capturepoint"));
        net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.register(capturePoint);
        // ItemBlock：1.12.2 创造页显示必须有物品注册（统一 ItemObjectiveDBlock 支持 Shift 介绍）
        net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(
            new com.myname.mymodid.item.ItemObjectiveDBlock(capturePoint).setRegistryName(capturePoint.getRegistryName()));
        GameRegistry.registerTileEntity(TileEntityCapturePoint.class, new ResourceLocation(MODID, "capturepoint"));

        BlockScoreController scoreController = new BlockScoreController();
        scoreController.setRegistryName(new ResourceLocation(MODID, "scorecontroller"));
        net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.register(scoreController);
        net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(
            new com.myname.mymodid.item.ItemObjectiveDBlock(scoreController).setRegistryName(scoreController.getRegistryName()));
        GameRegistry.registerTileEntity(TileEntityScoreController.class, new ResourceLocation(MODID, "scorecontroller"));

        BlockBattlePortal battlePortal = new BlockBattlePortal();
        battlePortal.setRegistryName(new ResourceLocation(MODID, "battleportal"));
        net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.register(battlePortal);
        net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(
            new com.myname.mymodid.item.ItemObjectiveDBlock(battlePortal).setRegistryName(battlePortal.getRegistryName()));
        GameRegistry.registerTileEntity(TileEntityBattlePortal.class, new ResourceLocation(MODID, "battleportal"));

        BlockTeamTeleporter teamTeleporter = new BlockTeamTeleporter();
        teamTeleporter.setRegistryName(new ResourceLocation(MODID, "teamteleporter"));
        net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.register(teamTeleporter);
        net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(
            new com.myname.mymodid.item.ItemObjectiveDBlock(teamTeleporter).setRegistryName(teamTeleporter.getRegistryName()));
        GameRegistry.registerTileEntity(TileEntityTeamTeleporter.class, new ResourceLocation(MODID, "teamteleporter"));

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        // 注册事件
        ScoreTickHandler scoreHandler = new ScoreTickHandler();
        MinecraftForge.EVENT_BUS.register(scoreHandler);

        com.myname.mymodid.handler.PlayerInteractHandler interactHandler = new com.myname.mymodid.handler.PlayerInteractHandler();
        MinecraftForge.EVENT_BUS.register(interactHandler);

        MinecraftForge.EVENT_BUS.register(new com.myname.mymodid.util.DelayedTask());

        // 注册网络包
        NETWORK.registerMessage(PacketCapturePointConfig.Handler.class, PacketCapturePointConfig.class, 0, Side.SERVER);
        NETWORK.registerMessage(PacketScoreUpdate.Handler.class, PacketScoreUpdate.class, 1, Side.CLIENT);
        NETWORK.registerMessage(PacketScoreConfig.Handler.class, PacketScoreConfig.class, 2, Side.SERVER);
        NETWORK.registerMessage(PacketBattleConfig.Handler.class, PacketBattleConfig.class, 3, Side.SERVER);
        NETWORK.registerMessage(PacketSaveBackpack.Handler.class, PacketSaveBackpack.class, 4, Side.SERVER);
        NETWORK.registerMessage(PacketSetSpawn.Handler.class, PacketSetSpawn.class, 5, Side.SERVER);
        NETWORK.registerMessage(PacketCapturePointSync.Handler.class, PacketCapturePointSync.class, 6, Side.CLIENT);
        NETWORK.registerMessage(PacketPlayerStats.Handler.class, PacketPlayerStats.class, 7, Side.CLIENT);
        NETWORK.registerMessage(PacketCapturePointSyncRemove.Handler.class, PacketCapturePointSyncRemove.class, 8, Side.CLIENT);
        NETWORK.registerMessage(PacketCapturePointFlags.Handler.class, PacketCapturePointFlags.class, 9, Side.SERVER);
        NETWORK.registerMessage(PacketChatMessage.Handler.class, PacketChatMessage.class, 10, Side.SERVER);
        NETWORK.registerMessage(PacketChatReceive.Handler.class, PacketChatReceive.class, 11, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketScoreSync.Handler.class, com.myname.mymodid.network.PacketScoreSync.class, 12, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoiceRequest.Handler.class, com.myname.mymodid.network.PacketVoiceRequest.class, 13, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoiceNeedUpload.Handler.class, com.myname.mymodid.network.PacketVoiceNeedUpload.class, 14, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoiceUpload.Handler.class, com.myname.mymodid.network.PacketVoiceUpload.class, 15, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoicePlay.Handler.class, com.myname.mymodid.network.PacketVoicePlay.class, 16, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketMarkRequest.Handler.class, com.myname.mymodid.network.PacketMarkRequest.class, 17, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketMarkSync.Handler.class, com.myname.mymodid.network.PacketMarkSync.class, 18, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamRequest.Handler.class, com.myname.mymodid.network.PacketTeamRequest.class, 19, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamSync.Handler.class, com.myname.mymodid.network.PacketTeamSync.class, 20, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamSelect.Handler.class, com.myname.mymodid.network.PacketTeamSelect.class, 21, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeleporterConfig.Handler.class, com.myname.mymodid.network.PacketTeleporterConfig.class, 22, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamScores.Handler.class, com.myname.mymodid.network.PacketTeamScores.class, 23, Side.CLIENT);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new com.myname.mymodid.client.ClientEventHandler());
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
