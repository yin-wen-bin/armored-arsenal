package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.client.screen.CreativeSupplyScreen;
import com.ethan.armoredarsenal.client.screen.GunSelectorScreen;
import com.ethan.armoredarsenal.client.screen.SuitSelectorScreen;
import com.ethan.armoredarsenal.registry.ModMenus;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ArmoredArsenal.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean slideKeyWasDown;
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SUIT_SELECTOR.get(), SuitSelectorScreen::new);
        event.register(ModMenus.GUN_SELECTOR.get(), GunSelectorScreen::new);
        event.register(ModMenus.CREATIVE_SUPPLY.get(), CreativeSupplyScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, ArmoredArsenal.id("suit_hud"), new SuitHudLayer());
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ArmoredArsenal.id("warden_megaboss_hud"),
                new WardenMegabossHudLayer());
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, ArmoredArsenal.id("storm_death"),
                new StormDeathLayer());
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fill")
                .then(Commands.argument("block", StringArgumentType.word())
                        .executes(context -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft.player == null) {
                                return 0;
                            }

                            String block = StringArgumentType.getString(context, "block");
                            minecraft.player.connection.sendCommand("we fill " + block);
                            return 1;
                        })));
    }
    @SubscribeEvent
    public static void renderTransformedPlayer(RenderPlayerEvent.Pre<?> event) {
        ClientTransformationState.render(event);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        ClientLaserBeams.clientTick(event);
        ClientStormGeometry.clientTick(event);
        StormDeathLayer.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            slideKeyWasDown = false;
            return;
        }
        boolean slideKeyDown = minecraft.player.isShiftKeyDown()
                && InputConstants.isKeyDown(minecraft.getWindow(), 85);
        if (slideKeyDown && !slideKeyWasDown) {
            minecraft.player.connection.sendCommand("slideposition");
        }
        slideKeyWasDown = slideKeyDown;
    }

    @SubscribeEvent
    public static void extractLaserBeams(ExtractLevelRenderStateEvent event) {
        ClientLaserBeams.extract(event);
        ClientStormGeometry.extract(event);
    }

    @SubscribeEvent
    public static void submitLaserBeams(SubmitCustomGeometryEvent event) {
        ClientLaserBeams.submit(event);
        ClientStormGeometry.submit(event);
    }

    private ClientEvents() {}
}
