package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.registry.ModItems;
import com.ethan.armoredarsenal.server.LaserLogic;
import com.ethan.armoredarsenal.server.RocketLogic;
import com.mojang.serialization.MapCodec;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AutoTurretBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AutoTurretBlock> CODEC = simpleCodec(AutoTurretBlock::new);
    public static final IntegerProperty GUN = IntegerProperty.create("gun", 0, 7);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14),
            Block.box(5, 4, 5, 11, 10, 11),
            Block.box(3, 9, 3, 13, 15, 13));

    public AutoTurretBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GUN, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        int gun = gunIndex(stack.getItem());
        if (gun == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(GUN) != 0) {
            player.sendSystemMessage(Component.literal("Turret already loaded. Crouch and right-click it with an empty hand to unload."));
            return InteractionResult.SUCCESS_SERVER;
        }

        stack.shrink(1);
        level.setBlock(pos, state.setValue(GUN, gun), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, 1);
        player.sendSystemMessage(Component.literal("Turret loaded with ").append(displayNameFor(gun)));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int gun = state.getValue(GUN);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (gun == 0) {
            player.sendSystemMessage(Component.literal("Right-click the turret with an Armored Arsenal gun to load it."));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!player.isShiftKeyDown()) {
            player.sendSystemMessage(Component.literal("Loaded: ").append(displayNameFor(gun)));
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack returned = new ItemStack(itemFor(gun));
        if (!player.getInventory().add(returned)) {
            player.drop(returned, false);
        }
        level.setBlock(pos, state.setValue(GUN, 0), Block.UPDATE_ALL);
        player.sendSystemMessage(Component.literal("Turret unloaded."));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int gun = state.getValue(GUN);
        if (gun == 0) {
            level.scheduleTick(pos, this, 20);
            return;
        }

        if (gun <= 4) {
            fireLaser(level, pos, state, gun);
        } else {
            fireRocket(level, pos, state, gun);
        }
    }

    private void fireLaser(ServerLevel level, BlockPos pos, BlockState state, int gun) {
        WeaponProfile profile = profileFor(gun);
        level.scheduleTick(pos, this, Math.max(4, profile.cooldownTicks()));
        findTarget(level, pos, profile.range(), 0.0D).ifPresent(target -> {
            Direction facing = facingToward(pos, target.position());
            if (state.getValue(FACING) != facing) {
                level.setBlock(pos, state.setValue(FACING, facing), Block.UPDATE_CLIENTS);
            }
            Vec3 start = Vec3.atCenterOf(pos).add(0.0D, 0.56D, 0.0D);
            LaserLogic.fireTurretBeam(level, pos, start, target, profile);
        });
    }

    private void fireRocket(ServerLevel level, BlockPos pos, BlockState state, int gun) {
        RocketProfile profile = rocketProfileFor(gun);
        level.scheduleTick(pos, this, Math.max(8, profile.cooldownTicks()));
        findTarget(level, pos, profile.range(), 5.0D).ifPresent(target -> {
            Direction facing = facingToward(pos, target.position());
            if (state.getValue(FACING) != facing) {
                level.setBlock(pos, state.setValue(FACING, facing), Block.UPDATE_CLIENTS);
            }
            Vec3 start = Vec3.atCenterOf(pos).add(0.0D, 0.56D, 0.0D);
            RocketLogic.launchFromTurret(level, pos, start, target, profile, itemFor(gun));
        });
    }

    private static Optional<LivingEntity> findTarget(
            ServerLevel level, BlockPos pos, double requestedRange, double minimumRange) {
        Vec3 start = Vec3.atCenterOf(pos).add(0.0D, 0.56D, 0.0D);
        double range = Math.min(requestedRange, 64.0D);
        double minimumDistanceSqr = minimumRange * minimumRange;
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos).inflate(range),
                        entity -> entity.isAlive() && entity instanceof Enemy)
                .stream()
                .filter(entity -> start.distanceToSqr(entity.getEyePosition()) <= range * range)
                .filter(entity -> start.distanceToSqr(entity.getEyePosition()) >= minimumDistanceSqr)
                .filter(entity -> hasLineOfSight(level, start, entity.getEyePosition()))
                .min(Comparator.comparingDouble(entity -> start.distanceToSqr(entity.getEyePosition())));
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 start, Vec3 end) {
        return level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType()
                == HitResult.Type.MISS;
    }

    private static Direction facingToward(BlockPos pos, Vec3 target) {
        double x = target.x - (pos.getX() + 0.5D);
        double z = target.z - (pos.getZ() + 0.5D);
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0.0D ? Direction.EAST : Direction.WEST;
        }
        return z > 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static int gunIndex(Item item) {
        if (item == ModItems.LASER_RIFLE.get()) return 1;
        if (item == ModItems.PULSE_PISTOL.get()) return 2;
        if (item == ModItems.BEAM_CANNON.get()) return 3;
        if (item == ModItems.CHARGED_SNIPER_LASER.get()) return 4;
        if (item == ModItems.STINGER_ROCKET.get()) return 5;
        if (item == ModItems.SIEGEBREAKER_ROCKET.get()) return 6;
        if (item == ModItems.TITAN_ROCKET.get()) return 7;
        return 0;
    }

    private static Item itemFor(int gun) {
        return switch (gun) {
            case 1 -> ModItems.LASER_RIFLE.get();
            case 2 -> ModItems.PULSE_PISTOL.get();
            case 3 -> ModItems.BEAM_CANNON.get();
            case 4 -> ModItems.CHARGED_SNIPER_LASER.get();
            case 5 -> ModItems.STINGER_ROCKET.get();
            case 6 -> ModItems.SIEGEBREAKER_ROCKET.get();
            case 7 -> ModItems.TITAN_ROCKET.get();
            default -> throw new IllegalArgumentException("Unknown turret gun " + gun);
        };
    }

    private static WeaponProfile profileFor(int gun) {
        return switch (gun) {
            case 1 -> WeaponProfile.LASER_RIFLE;
            case 2 -> WeaponProfile.PULSE_PISTOL;
            case 3 -> WeaponProfile.BEAM_CANNON;
            case 4 -> WeaponProfile.CHARGED_SNIPER;
            default -> throw new IllegalArgumentException("Unknown turret gun " + gun);
        };
    }

    private static RocketProfile rocketProfileFor(int gun) {
        return switch (gun) {
            case 5 -> RocketProfile.STINGER;
            case 6 -> RocketProfile.SIEGEBREAKER;
            case 7 -> RocketProfile.TITAN;
            default -> throw new IllegalArgumentException("Unknown turret rocket " + gun);
        };
    }

    private static Component displayNameFor(int gun) {
        return gun <= 4 ? profileFor(gun).displayName() : rocketProfileFor(gun).displayName();
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        int gun = state.getValue(GUN);
        if (gun != 0) {
            Block.popResource(level, pos, new ItemStack(itemFor(gun)));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GUN);
    }
}
