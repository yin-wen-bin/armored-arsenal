package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public final class StormBeaconHandler {
    private static final String BEACON_POS = "ArmoredArsenalWitheredBeaconPos";
    private static final String BEACON_DIM = "ArmoredArsenalWitheredBeaconDimension";

    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getPlacedBlock().is(ModBlocks.WITHERED_BEACON.get())) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.putLong(BEACON_POS, event.getPos().asLong());
        data.putString(BEACON_DIM, level.dimension().identifier().toString());
        TransformationHandler.applyBeaconForm(player);
        player.sendSystemMessage(Component.literal("The Withered Beacon made you a mini Wither Storm."), false);
    }

    public static void blockBroken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(ModBlocks.WITHERED_BEACON.get())) {
            return;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            CompoundTag data = player.getPersistentData();
            if (data.getLongOr(BEACON_POS, Long.MIN_VALUE) == event.getPos().asLong()
                    && data.getStringOr(BEACON_DIM, "").equals(level.dimension().identifier().toString())) {
                clear(player);
            }
        }
    }

    public static void playerTick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(BEACON_POS)) {
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        Identifier id = Identifier.tryParse(data.getStringOr(BEACON_DIM, ""));
        ServerLevel level = id == null ? null : player.level().getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, id));
        BlockPos pos = BlockPos.of(data.getLongOr(BEACON_POS, 0L));
        if (level == null || !level.getBlockState(pos).is(ModBlocks.WITHERED_BEACON.get())) {
            clear(player);
            return;
        }
        TransformationHandler.applyBeaconForm(player);
        player.level().sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.7, 0.9, 0.7, 0.0);
    }

    private static void clear(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(BEACON_POS);
        data.remove(BEACON_DIM);
        TransformationHandler.clearBeaconForm(player);
        player.sendSystemMessage(Component.literal("The Withered Beacon broke. You are a player again."), false);
    }

    private StormBeaconHandler() {}
}
