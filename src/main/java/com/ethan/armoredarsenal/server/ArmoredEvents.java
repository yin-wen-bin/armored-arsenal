package com.ethan.armoredarsenal.server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ArmoredEvents {
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.seenCredits = true;
            SuitPowerHandler.tick(player);
            TransformationHandler.tick(player);
            VillagePopulationHandler.tick(player);
            WaterSlideTools.tickSliding(player);
        }
    }

    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SuitPowerHandler.hasFullSuit(player)) {
            event.setDistance(0);
            event.setCanceled(true);
        }
    }

    private ArmoredEvents() {}
}
