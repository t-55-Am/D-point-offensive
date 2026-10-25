package com.myname.mymodid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.myname.mymodid.block.BlockBattlePortal;
import com.myname.mymodid.block.BlockCapturePoint;
import com.myname.mymodid.block.BlockScoreController;
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
import com.myname.mymodid.score.ScoreTickHandler;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.tileentity.TileEntityScoreController;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

// 修改点1：Tags.VERSION 替换为 "1.1.8"，其他注解参数原样保留
@Mod(modid = MyMod.MODID, version = "1.1.16", name = "MyMod", acceptedMinecraftVersions = "[1.7.10]")
public class MyMod {
    public static final String MODID = "objective_d";
    public static final Logger LOG = LogManager.getLogger(MODID);
    @Mod.Instance(MODID)
    public static MyMod instance;
    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    // 创意标签页「D点攻势」：图标用占点方块，本地化 key = itemGroup.war_tab
    public static final CreativeTabs WAR_TAB = new CreativeTabs("war_tab") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(GameRegistry.findBlock(MODID, "capturePoint"));
        }
    };
    @SidedProxy(clientSide = "com.myname.mymodid.ClientProxy", serverSide = "com.myname.mymodid.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 修复：加载配置（此前从未调用，enableScore 等配置项从未写入配置文件，全靠代码默认值）
        com.myname.mymodid.config.MyModConfig.init(event.getSuggestedConfigurationFile());
        proxy.preInit(event);
        GameRegistry.registerBlock(new BlockCapturePoint(), com.myname.mymodid.item.ItemObjectiveDBlock.class, "capturePoint");
        GameRegistry.registerTileEntity(TileEntityCapturePoint.class, MODID + "_capturePoint");
        GameRegistry.registerBlock(new BlockScoreController(), com.myname.mymodid.item.ItemObjectiveDBlock.class, "scoreController");
        GameRegistry.registerTileEntity(TileEntityScoreController.class, MODID + "_scoreController");
        GameRegistry.registerBlock(new BlockBattlePortal(), com.myname.mymodid.item.ItemObjectiveDBlock.class, "battlePortal");
        GameRegistry.registerTileEntity(TileEntityBattlePortal.class, MODID + "_battlePortal");
        GameRegistry.registerBlock(new com.myname.mymodid.block.BlockTeamTeleporter(), com.myname.mymodid.item.ItemObjectiveDBlock.class, "teamTeleporter");
        GameRegistry.registerTileEntity(TileEntityTeamTeleporter.class, MODID + "_teamTeleporter");
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        ScoreTickHandler scoreHandler = new ScoreTickHandler();
        MinecraftForge.EVENT_BUS.register(scoreHandler);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(scoreHandler);
        com.myname.mymodid.handler.PlayerInteractHandler interactHandler = new com.myname.mymodid.handler.PlayerInteractHandler();
        MinecraftForge.EVENT_BUS.register(interactHandler);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(interactHandler);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new com.myname.mymodid.util.DelayedTask());

        NETWORK.registerMessage(PacketCapturePointConfig.Handler.class, PacketCapturePointConfig.class, 0, Side.SERVER);
        NETWORK.registerMessage(PacketScoreUpdate.Handler.class, PacketScoreUpdate.class, 1, Side.CLIENT);
        NETWORK.registerMessage(PacketScoreConfig.Handler.class, PacketScoreConfig.class, 2, Side.SERVER);
        NETWORK.registerMessage(PacketBattleConfig.Handler.class, PacketBattleConfig.class, 3, Side.SERVER);
        NETWORK.registerMessage(PacketSaveBackpack.Handler.class, PacketSaveBackpack.class, 4, Side.SERVER);
        NETWORK.registerMessage(PacketCapturePointSync.Handler.class, PacketCapturePointSync.class, 6, Side.CLIENT);
        NETWORK.registerMessage(PacketPlayerStats.Handler.class, PacketPlayerStats.class, 7, Side.CLIENT);
        NETWORK.registerMessage(PacketCapturePointSyncRemove.Handler.class, PacketCapturePointSyncRemove.class, 8, Side.CLIENT);
        NETWORK.registerMessage(PacketCapturePointFlags.Handler.class, PacketCapturePointFlags.class, 9, Side.SERVER);
        NETWORK.registerMessage(PacketChatMessage.Handler.class, PacketChatMessage.class, 10, Side.SERVER);
        NETWORK.registerMessage(PacketChatReceive.Handler.class, PacketChatReceive.class, 11, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoiceRequest.Handler.class, com.myname.mymodid.network.PacketVoiceRequest.class, 13, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoiceNeedUpload.Handler.class, com.myname.mymodid.network.PacketVoiceNeedUpload.class, 14, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoiceUpload.Handler.class, com.myname.mymodid.network.PacketVoiceUpload.class, 15, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketVoicePlay.Handler.class, com.myname.mymodid.network.PacketVoicePlay.class, 16, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketMarkRequest.Handler.class, com.myname.mymodid.network.PacketMarkRequest.class, 17, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketMarkSync.Handler.class, com.myname.mymodid.network.PacketMarkSync.class, 18, Side.CLIENT);



        // 修改点2：新增注册 PacketScoreSync 数据包 id=12
        NETWORK.registerMessage(com.myname.mymodid.network.PacketScoreSync.Handler.class, com.myname.mymodid.network.PacketScoreSync.class,12, Side.CLIENT);

        // v1.1.8：自由队伍系统网络包
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamRequest.Handler.class, com.myname.mymodid.network.PacketTeamRequest.class, 19, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamSync.Handler.class, com.myname.mymodid.network.PacketTeamSync.class, 20, Side.CLIENT);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamSelect.Handler.class, com.myname.mymodid.network.PacketTeamSelect.class, 21, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeleporterConfig.Handler.class, com.myname.mymodid.network.PacketTeleporterConfig.class, 22, Side.SERVER);
        NETWORK.registerMessage(com.myname.mymodid.network.PacketTeamScores.Handler.class, com.myname.mymodid.network.PacketTeamScores.class, 23, Side.CLIENT);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        // 修改点3：仅客户端注册HUD事件处理器
        if(cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient()){
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new com.myname.mymodid.client.ClientEventHandler());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.myname.mymodid.client.ClientEventHandler());
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
