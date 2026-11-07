package com.myname.mymodid;

import com.myname.mymodid.block.BlockBattlePortal;
import com.myname.mymodid.block.BlockCapturePoint;
import com.myname.mymodid.block.BlockScoreController;
import com.myname.mymodid.client.ClientEventHandler;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.score.ScoreTickHandler;
import com.myname.mymodid.tileentity.TileEntityBattlePortal;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import com.myname.mymodid.tileentity.TileEntityScoreController;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;
import com.myname.mymodid.util.DelayedTask;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(MyMod.MODID)
public class MyMod {
    public static final String MODID = "objective_d";
    public static final String VERSION = "1.0.1";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MyMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MyMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MyMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MyMod.MODID);

    public static final Supplier<Block> CAPTURE_POINT = BLOCKS.register("capture_point", BlockCapturePoint::new);
    public static final Supplier<Block> SCORE_CONTROLLER = BLOCKS.register("score_controller", BlockScoreController::new);
    public static final Supplier<Block> BATTLE_PORTAL = BLOCKS.register("battle_portal", BlockBattlePortal::new);
    public static final Supplier<Block> TEAM_TELEPORTER = BLOCKS.register("team_teleporter", BlockTeamTeleporter::new);

    public static final Supplier<BlockEntityType<TileEntityCapturePoint>> CAPTURE_POINT_TE = BLOCK_ENTITIES.register("capture_point_te", () -> BlockEntityType.Builder.of(TileEntityCapturePoint::new, CAPTURE_POINT.get()).build(null));
    public static final Supplier<BlockEntityType<TileEntityScoreController>> SCORE_CONTROLLER_TE = BLOCK_ENTITIES.register("score_controller_te", () -> BlockEntityType.Builder.of(TileEntityScoreController::new, SCORE_CONTROLLER.get()).build(null));
    public static final Supplier<BlockEntityType<TileEntityBattlePortal>> BATTLE_PORTAL_TE = BLOCK_ENTITIES.register("battle_portal_te", () -> BlockEntityType.Builder.of(TileEntityBattlePortal::new, BATTLE_PORTAL.get()).build(null));
    public static final Supplier<BlockEntityType<TileEntityTeamTeleporter>> TEAM_TELEPORTER_TE = BLOCK_ENTITIES.register("team_teleporter_te", () -> BlockEntityType.Builder.of(TileEntityTeamTeleporter::new, TEAM_TELEPORTER.get()).build(null));

    public static final Supplier<BlockItem> CAPTURE_POINT_ITEM = ITEMS.register("capture_point", () -> new BlockItem(CAPTURE_POINT.get(), new Item.Properties()));
    public static final Supplier<BlockItem> SCORE_CONTROLLER_ITEM = ITEMS.register("score_controller", () -> new BlockItem(SCORE_CONTROLLER.get(), new Item.Properties()));
    public static final Supplier<BlockItem> BATTLE_PORTAL_ITEM = ITEMS.register("battle_portal", () -> new BlockItem(BATTLE_PORTAL.get(), new Item.Properties()));
    public static final Supplier<BlockItem> TEAM_TELEPORTER_ITEM = ITEMS.register("team_teleporter", () -> new BlockItem(TEAM_TELEPORTER.get(), new Item.Properties()));

    public static final Supplier<CreativeModeTab> WAR_TAB = TABS.register("war_tab", () -> CreativeModeTab.builder()
        .title(net.minecraft.network.chat.Component.translatable("itemGroup.objective_d"))
        .icon(() -> new ItemStack(CAPTURE_POINT_ITEM.get()))
        .displayItems((params, output) -> {
            output.accept(new ItemStack(CAPTURE_POINT_ITEM.get()));
            output.accept(new ItemStack(SCORE_CONTROLLER_ITEM.get()));
            output.accept(new ItemStack(BATTLE_PORTAL_ITEM.get()));
            output.accept(new ItemStack(TEAM_TELEPORTER_ITEM.get()));
        })
        .build());

    public MyMod(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(Network::register);
        container.registerConfig(ModConfig.Type.SERVER, MyModConfig.SPEC);
        NeoForge.EVENT_BUS.register(new ScoreTickHandler());
        NeoForge.EVENT_BUS.register(new DelayedTask());
        if (FMLEnvironment.dist.isClient()) {
            MyModConfig.loadHud();
            NeoForge.EVENT_BUS.register(new ClientEventHandler());
        }
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientEventHandler.keyQuickComm);
        event.register(ClientEventHandler.keyQuickCommConfig);
        event.register(ClientEventHandler.keyChatGui);
        event.register(ClientEventHandler.keyMark);
    }
}
