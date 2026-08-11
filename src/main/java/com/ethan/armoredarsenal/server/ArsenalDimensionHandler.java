package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.registry.ModBlocks;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class ArsenalDimensionHandler {
    public static final ResourceKey<Level> ARSENAL_DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ArmoredArsenal.id("arsenal_dimension"));

    public static int toggleDimension(ServerPlayer player) {
        ServerLevel target = player.level().dimension().equals(ARSENAL_DIMENSION)
                ? player.level().getServer().overworld()
                : player.level().getServer().getLevel(ARSENAL_DIMENSION);
        if (target == null) {
            player.sendSystemMessage(Component.literal("The Arsenal Dimension is not available."), false);
            return 0;
        }

        BlockPos arrival = player.level().dimension().equals(ARSENAL_DIMENSION)
                ? target.getServer().getRespawnData().pos()
                : new BlockPos(0, 110, 0);
        if (target.dimension().equals(ARSENAL_DIMENSION)) {
            prepareArrivalPlatform(target, arrival);
        }
        player.teleportTo(
                target, arrival.getX() + 0.5D, arrival.getY() + 1.0D, arrival.getZ() + 0.5D,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.literal(target.dimension().equals(ARSENAL_DIMENSION)
                ? "Entered the Arsenal Dimension. Mine underground for Arcanite Ore."
                : "Returned to the Overworld."), false);
        return 1;
    }

    public static void chunkLoaded(ChunkEvent.Load event) {
        if (!event.isNewChunk()
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(ARSENAL_DIMENSION)) {
            return;
        }

        LevelChunk chunk = event.getChunk();
        long seed = level.getSeed() ^ ((long) chunk.getPos().x() * 341873128712L)
                ^ ((long) chunk.getPos().z() * 132897987541L);
        RandomSource random = RandomSource.create(seed);
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int vein = 0; vein < 12; vein++) {
            int x = startX + random.nextInt(16);
            int y = -48 + random.nextInt(96);
            int z = startZ + random.nextInt(16);
            for (int part = 0; part < 3 + random.nextInt(5); part++) {
                BlockPos pos = new BlockPos(
                        x + random.nextInt(3) - 1,
                        y + random.nextInt(3) - 1,
                        z + random.nextInt(3) - 1);
                if (chunk.getBlockState(pos).is(Blocks.STONE)
                        || chunk.getBlockState(pos).is(Blocks.DEEPSLATE)) {
                    chunk.setBlockState(pos, ModBlocks.ARCANITE_ORE.get().defaultBlockState(), 0);
                }
            }
        }
    }

    private static void prepareArrivalPlatform(ServerLevel level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.OBSIDIAN.defaultBlockState());
                for (int y = 1; y <= 3; y++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private ArsenalDimensionHandler() {}
}
