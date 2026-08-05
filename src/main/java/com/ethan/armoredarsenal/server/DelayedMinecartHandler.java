package com.ethan.armoredarsenal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class DelayedMinecartHandler {
    private static final String LAUNCH_AT = "ArmoredArsenalMinecartLaunchAt";
    private static final String LAUNCHED = "ArmoredArsenalMinecartLaunched";
    private static final long DELAY_TICKS = 80L;
    private static final double LAUNCH_SPEED = 0.35;

    public static void beforeEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractMinecart minecart)
                || !(minecart.level() instanceof ServerLevel level)
                || minecart.getPersistentData().getBooleanOr("ArmoredArsenalCouchSeat", false)
                || minecart.getPersistentData().getBooleanOr(LAUNCHED, false)) {
            return;
        }

        Rail rail = railUnder(level, minecart);
        if (rail == null) {
            minecart.getPersistentData().remove(LAUNCH_AT);
            return;
        }

        if (minecart.getPersistentData().getBooleanOr(LAUNCHED, false)) {
            maintainSpeed(minecart, rail.shape());
            return;
        }

        long launchAt = minecart.getPersistentData().getLongOr(LAUNCH_AT, -1L);
        if (launchAt < 0L) {
            minecart.getPersistentData().putLong(LAUNCH_AT, level.getGameTime() + DELAY_TICKS);
            return;
        }
        if (level.getGameTime() < launchAt) {
            return;
        }

        minecart.getPersistentData().putBoolean(LAUNCHED, true);
        if (minecart.getDeltaMovement().horizontalDistanceSqr() < 0.0025) {
            minecart.setDeltaMovement(launchVelocity(rail.shape()));
            minecart.hurtMarked = true;
        }
    }

    private static void maintainSpeed(AbstractMinecart minecart, RailShape shape) {
        if (isCurve(shape)) {
            return;
        }

        Vec3 movement = minecart.getDeltaMovement();
        double horizontalSpeed = movement.horizontalDistance();
        if (horizontalSpeed >= LAUNCH_SPEED) {
            return;
        }

        Vec3 assisted;
        if (horizontalSpeed > 0.01) {
            double scale = LAUNCH_SPEED / horizontalSpeed;
            assisted = new Vec3(movement.x * scale, movement.y, movement.z * scale);
        } else {
            assisted = launchVelocity(shape);
        }
        minecart.setDeltaMovement(assisted);
        minecart.hurtMarked = true;
    }

    private static Rail railUnder(ServerLevel level, AbstractMinecart minecart) {
        BlockPos pos = minecart.blockPosition();
        BlockState state = level.getBlockState(pos);
        BaseRailBlock railBlock;
        if (state.getBlock() instanceof BaseRailBlock currentRail) {
            railBlock = currentRail;
        } else {
            pos = pos.below();
            state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BaseRailBlock belowRail)) {
                return null;
            }
            railBlock = belowRail;
        }
        return new Rail(railBlock.getRailDirection(state, level, pos, minecart));
    }

    private static Vec3 launchVelocity(RailShape shape) {
        return switch (shape) {
            case ASCENDING_WEST -> new Vec3(-LAUNCH_SPEED, 0.0, 0.0);
            case EAST_WEST, ASCENDING_EAST -> new Vec3(LAUNCH_SPEED, 0.0, 0.0);
            case ASCENDING_NORTH -> new Vec3(0.0, 0.0, -LAUNCH_SPEED);
            case SOUTH_EAST, NORTH_EAST -> new Vec3(LAUNCH_SPEED, 0.0, 0.0);
            case SOUTH_WEST, NORTH_WEST -> new Vec3(-LAUNCH_SPEED, 0.0, 0.0);
            default -> new Vec3(0.0, 0.0, LAUNCH_SPEED);
        };
    }

    private static boolean isCurve(RailShape shape) {
        return switch (shape) {
            case SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST -> true;
            default -> false;
        };
    }

    private record Rail(RailShape shape) {}

    private DelayedMinecartHandler() {}
}