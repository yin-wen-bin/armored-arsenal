package com.ethan.armoredarsenal.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
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
            WitherStormHandler.playerTick(player);
            StormBeaconHandler.playerTick(player);
        }
    }

    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Warden warden
                && warden.level() instanceof ServerLevel level
                && event.getDistance() >= level.getHeight() - 32) {
            warden.kill(level);
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player && SuitPowerHandler.hasFullSuit(player)) {
            event.setDistance(0);
            event.setCanceled(true);
        }
    }

    private ArmoredEvents() {}
}
