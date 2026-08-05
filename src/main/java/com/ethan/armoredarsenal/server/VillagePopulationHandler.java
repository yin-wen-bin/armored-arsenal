package com.ethan.armoredarsenal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class VillagePopulationHandler {
    private static final int CHECK_INTERVAL = 600;
    private static final int MAX_NEARBY_VILLAGERS = 8;
    private static final int SEARCH_RADIUS = 40;
    private static final int LIME_BED_RESERVATION_RADIUS = 7;
    private static final String AUTO_VILLAGER = "ArmoredArsenalAutoVillager";

    public static void tick(ServerPlayer player) {
        if (player.tickCount % CHECK_INTERVAL != 0 || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel level = player.level();
        AABB populationArea = player.getBoundingBox().inflate(SEARCH_RADIUS, 16, SEARCH_RADIUS);
        if (level.getEntitiesOfClass(Villager.class, populationArea).size() >= MAX_NEARBY_VILLAGERS) {
            return;
        }

        BlockPos origin = player.blockPosition();
        List<BlockPos> limeBeds = level.getPoiManager().findAll(
                        type -> type.is(PoiTypes.HOME),
                        pos -> level.getBlockState(pos).is(Blocks.LIME_BED),
                        origin, SEARCH_RADIUS + LIME_BED_RESERVATION_RADIUS, PoiManager.Occupancy.ANY)
                .toList();
        level.getPoiManager().findAll(
                        type -> type.is(PoiTypes.HOME), pos -> true,
                        origin, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .filter(pos -> Math.abs(pos.getY() - origin.getY()) <= 12)
                .filter(pos -> isAvailableSpawnBed(level, pos, limeBeds))
                .findFirst()
                .ifPresent(bedPos -> EntityType.VILLAGER.spawn(
                        level, spawned -> {
                            spawned.setPersistenceRequired();
                            spawned.getPersistentData().putBoolean(AUTO_VILLAGER, true);
                        }, bedPos.above(), EntitySpawnReason.NATURAL, false, false));
    }

    public static void beforeEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)
                || !villager.getPersistentData().getBooleanOr(AUTO_VILLAGER, false)
                || !(villager.level() instanceof ServerLevel level)) {
            return;
        }

        villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(home -> {
            if (home.dimension().equals(level.dimension()) && level.getBlockState(home.pos()).is(Blocks.LIME_BED)) {
                villager.getBrain().eraseMemory(MemoryModuleType.HOME);
                if (villager.isSleeping()) villager.stopSleeping();
            }
        });
    }

    private static boolean isAvailableSpawnBed(ServerLevel level, BlockPos bedPos, List<BlockPos> limeBeds) {
        BlockState state = level.getBlockState(bedPos);
        if (!(state.getBlock() instanceof BedBlock)
                || state.getValue(BedBlock.PART) != BedPart.HEAD
                || state.is(Blocks.LIME_BED)
                || !level.getBlockState(bedPos.above()).isAir()
                || !level.getEntitiesOfClass(Villager.class, new AABB(bedPos).inflate(7, 4, 7)).isEmpty()) {
            return false;
        }
        return limeBeds.stream().noneMatch(lime ->
                Math.abs(lime.getX() - bedPos.getX()) <= LIME_BED_RESERVATION_RADIUS
                        && Math.abs(lime.getY() - bedPos.getY()) <= 4
                        && Math.abs(lime.getZ() - bedPos.getZ()) <= LIME_BED_RESERVATION_RADIUS);
    }

    private VillagePopulationHandler() {}
}