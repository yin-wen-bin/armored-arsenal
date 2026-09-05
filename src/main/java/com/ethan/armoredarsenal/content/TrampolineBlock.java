package com.ethan.armoredarsenal.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class TrampolineBlock extends Block {
    public static final MapCodec<TrampolineBlock> CODEC = simpleCodec(TrampolineBlock::new);

    public TrampolineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(Level level, BlockState state, net.minecraft.core.BlockPos pos, Entity entity, double distance) {
        entity.resetFallDistance();
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        var movement = entity.getDeltaMovement();
        int connectedSize = connectedSize(level, entity.blockPosition().below());
        double sizeBounce = 0.8D + Math.sqrt(connectedSize) * 0.38D;
        double bounce = Math.max(sizeBounce, Math.min(5.5D, -movement.y * 1.05D));
        entity.setDeltaMovement(movement.x, bounce, movement.z);
    }

    private static int connectedSize(BlockGetter level, net.minecraft.core.BlockPos start) {
        if (!(level.getBlockState(start).getBlock() instanceof TrampolineBlock)) {
            return 1;
        }
        ArrayDeque<net.minecraft.core.BlockPos> open = new ArrayDeque<>();
        Set<net.minecraft.core.BlockPos> visited = new HashSet<>();
        open.add(start.immutable());
        while (!open.isEmpty() && visited.size() < 256) {
            net.minecraft.core.BlockPos pos = open.removeFirst();
            if (visited.contains(pos) || !(level.getBlockState(pos).getBlock() instanceof TrampolineBlock)) {
                continue;
            }
            visited.add(pos);
            open.add(pos.north());
            open.add(pos.south());
            open.add(pos.east());
            open.add(pos.west());
        }
        return Math.max(1, visited.size());
    }
}
