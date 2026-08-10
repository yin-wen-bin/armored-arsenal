package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.network.LaserBeamPayload;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PortableLaserBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PortableLaserBlock> CODEC = simpleCodec(PortableLaserBlock::new);
    private static final int MAX_RANGE = 64;
    private static final int UPDATE_INTERVAL = 5;
    private static final VoxelShape NORTH = Block.box(3, 3, 10, 13, 13, 16);
    private static final VoxelShape SOUTH = Block.box(3, 3, 0, 13, 13, 6);
    private static final VoxelShape EAST = Block.box(0, 3, 3, 6, 13, 13);
    private static final VoxelShape WEST = Block.box(10, 3, 3, 16, 13, 13);

    public PortableLaserBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, UPDATE_INTERVAL);
        Direction facing = state.getValue(FACING);
        BlockPos partner = findPartner(level, pos, facing);
        if (partner == null || !isPrimary(pos, partner)) {
            return;
        }

        Vec3 start = aperture(pos, facing);
        Vec3 end = aperture(partner, facing.getOpposite());
        damageEntities(level, start, end);
        Vec3 middle = start.add(end).scale(0.5D);
        PacketDistributor.sendToPlayersNear(
                level, null, middle.x, middle.y, middle.z, MAX_RANGE + 24.0D,
                LaserBeamPayload.between(pos.asLong(), start, end, 0xDFFF1818, 0.085F, UPDATE_INTERVAL + 3));
    }

    private static void damageEntities(ServerLevel level, Vec3 start, Vec3 end) {
        level.getEntitiesOfClass(
                        LivingEntity.class,
                        new net.minecraft.world.phys.AABB(start, end).inflate(0.3D),
                        LivingEntity::isAlive)
                .stream()
                .filter(entity -> entity.getBoundingBox().inflate(0.12D).clip(start, end).isPresent())
                .forEach(entity -> {
                    if (entity.hurtServer(level, level.damageSources().magic(), 3.0F)) {
                        entity.igniteForSeconds(1.0F);
                    }
                });
    }

    private BlockPos findPartner(ServerLevel level, BlockPos pos, Direction facing) {
        for (int distance = 1; distance <= MAX_RANGE; distance++) {
            BlockPos candidate = pos.relative(facing, distance);
            BlockState candidateState = level.getBlockState(candidate);
            if (candidateState.isAir()) {
                continue;
            }
            if (candidateState.is(this) && candidateState.getValue(FACING) == facing.getOpposite()) {
                return candidate;
            }
            return null;
        }
        return null;
    }

    private static Vec3 aperture(BlockPos pos, Direction facing) {
        return Vec3.atCenterOf(pos).add(
                facing.getStepX() * 0.42D,
                facing.getStepY() * 0.42D,
                facing.getStepZ() * 0.42D);
    }

    private static boolean isPrimary(BlockPos first, BlockPos second) {
        if (first.getX() != second.getX()) return first.getX() < second.getX();
        if (first.getY() != second.getY()) return first.getY() < second.getY();
        return first.getZ() < second.getZ();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
