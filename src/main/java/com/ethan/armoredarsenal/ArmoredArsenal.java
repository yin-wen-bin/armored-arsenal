package com.ethan.armoredarsenal;

import com.ethan.armoredarsenal.client.ClientLaserBeams;
import com.ethan.armoredarsenal.client.ClientTransformationState;
import com.ethan.armoredarsenal.network.LaserBeamPayload;
import com.ethan.armoredarsenal.network.TransformationPayload;
import com.ethan.armoredarsenal.registry.ModCreativeTabs;
import com.ethan.armoredarsenal.registry.ModBlocks;
import com.ethan.armoredarsenal.registry.ModItems;
import com.ethan.armoredarsenal.registry.ModMenus;
import com.ethan.armoredarsenal.server.ArmoredCommands;
import com.ethan.armoredarsenal.server.ArmoredEvents;
import com.ethan.armoredarsenal.server.ArsenalDimensionHandler;
import com.ethan.armoredarsenal.server.BuildableEndPortalHandler;
import com.ethan.armoredarsenal.server.CouchSittingHandler;
import com.ethan.armoredarsenal.server.DelayedMinecartHandler;
import com.ethan.armoredarsenal.server.MaterialGolemHandler;
import com.ethan.armoredarsenal.server.RocketLogic;
import com.ethan.armoredarsenal.server.VillagePopulationHandler;
import com.ethan.armoredarsenal.server.WorldEditTools;
import com.ethan.armoredarsenal.server.ThrowEnchantHandler;
import com.ethan.armoredarsenal.server.WaterFloodTntHandler;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(ArmoredArsenal.MOD_ID)
public final class ArmoredArsenal {
    public static final String MOD_ID = "armoredarsenal";

    public ArmoredArsenal(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener(ArmoredArsenal::registerPayloads);

        NeoForge.EVENT_BUS.addListener(ArmoredCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(ArmoredCommands::handlePlainChatCommands);
        NeoForge.EVENT_BUS.addListener(ArmoredEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(ArmoredEvents::livingFall);
        NeoForge.EVENT_BUS.addListener(MaterialGolemHandler::blockPlaced);
        NeoForge.EVENT_BUS.addListener(MaterialGolemHandler::incomingDamage);
        NeoForge.EVENT_BUS.addListener(MaterialGolemHandler::afterEntityTick);
        NeoForge.EVENT_BUS.addListener(BuildableEndPortalHandler::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(CouchSittingHandler::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(CouchSittingHandler::beforeEntityTick);
        NeoForge.EVENT_BUS.addListener(DelayedMinecartHandler::beforeEntityTick);
        NeoForge.EVENT_BUS.addListener(VillagePopulationHandler::beforeEntityTick);
        NeoForge.EVENT_BUS.addListener(WorldEditTools::command);
        NeoForge.EVENT_BUS.addListener(WorldEditTools::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(WorldEditTools::leftClickBlock);
        NeoForge.EVENT_BUS.addListener(WaterFloodTntHandler::beforeEntityTick);
        NeoForge.EVENT_BUS.addListener(RocketLogic::projectileImpact);
        NeoForge.EVENT_BUS.addListener(ThrowEnchantHandler::rightClickItem);
        NeoForge.EVENT_BUS.addListener(ThrowEnchantHandler::projectileImpact);
        NeoForge.EVENT_BUS.addListener(RocketLogic::beforeEntityTick);
        NeoForge.EVENT_BUS.addListener(ArsenalDimensionHandler::chunkLoaded);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                TransformationPayload.TYPE,
                TransformationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientTransformationState.accept(payload)));
        registrar.playToClient(
                LaserBeamPayload.TYPE,
                LaserBeamPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientLaserBeams.accept(payload)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
