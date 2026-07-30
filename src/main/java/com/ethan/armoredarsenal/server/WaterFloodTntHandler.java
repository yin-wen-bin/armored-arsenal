package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.WaterFloodTntItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class WaterFloodTntHandler {
    private static final int RADIUS = 5;
    private static final int DEPTH = 3;

    public static void beforeEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof PrimedTnt tnt)
                || !(tnt.level() instanceof ServerLevel level)
                || !tnt.getPersistentData().getBooleanOr(WaterFloodTntItem.ENTITY_MARKER, false)
                || tnt.getFuse() > 1) return;
        event.setCanceled(true);
        tnt.discard();
        flood(level, tnt.blockPosition());
        level.sendParticles(ParticleTypes.SPLASH, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(), 180, 4.0, 1.5, 4.0, 0.35);
        level.playSound(null, tnt.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0F, 0.85F);
    }

    private static void flood(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int yOffset = -(DEPTH - 1); yOffset <= 0; yOffset++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (x * x + z * z > RADIUS * RADIUS) continue;
                    cursor.set(center.getX() + x, center.getY() + yOffset, center.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()) level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 3);
                }
            }
        }
    }

    private WaterFloodTntHandler() {}
}