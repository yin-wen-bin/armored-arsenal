package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.ethan.armoredarsenal.registry.ModBlocks;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WaterSlideTools {
    private static final Set<UUID> SLIDING_PLAYERS = new HashSet<>();

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("waterslide")
                .then(Commands.literal("tornado").executes(context -> build(context.getSource().getPlayerOrException(), "tornado")))
                .then(Commands.literal("kidder").executes(context -> build(context.getSource().getPlayerOrException(), "kidder")))
                .then(Commands.literal("crazymode").executes(context -> build(context.getSource().getPlayerOrException(), "crazymode")))
                .then(Commands.literal("straightdown").executes(context -> build(context.getSource().getPlayerOrException(), "straightdown"))));
        dispatcher.register(Commands.literal("slideposition")
                .executes(context -> toggleSliding(context.getSource().getPlayerOrException())));
    }

    public static void tickSliding(ServerPlayer player) {
        if (!SLIDING_PLAYERS.contains(player.getUUID())) {
            return;
        }
        player.setPose(Pose.SWIMMING);
        if (!player.isInWater()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 current = player.getDeltaMovement();
        double horizontal = 1.65D;
        double y = Math.min(current.y, Math.max(-1.1D, look.y * 0.75D - 0.12D));
        player.setDeltaMovement(look.x * horizontal, y, look.z * horizontal);
        player.resetFallDistance();
    }

    private static int toggleSliding(ServerPlayer player) {
        boolean enabled;
        if (SLIDING_PLAYERS.remove(player.getUUID())) {
            player.setPose(Pose.STANDING);
            enabled = false;
        } else {
            SLIDING_PLAYERS.add(player.getUUID());
            enabled = true;
        }
        player.sendSystemMessage(Component.literal("Fast water-slide position " + (enabled ? "enabled" : "disabled") + "."));
        return 1;
    }

    private static int build(ServerPlayer player, String type) {
        ServerLevel level = player.level();
        Direction forward = player.getDirection();
        int height = type.equals("kidder") ? 18 : type.equals("straightdown") ? 48 : 28;
        BlockPos origin = player.blockPosition().relative(forward, 8).above(height);
        int blocks = switch (type) {
            case "tornado" -> buildTornado(level, origin, forward);
            case "kidder" -> buildKidder(level, origin, forward);
            case "crazymode" -> buildCrazy(level, origin, forward);
            default -> buildStraightDown(level, origin, forward);
        };
        BlockPos entrance = switch (type) {
            case "tornado" -> local(origin, forward, 10, 0, 0);
            case "kidder" -> local(origin, forward, 7, 0, 0);
            default -> origin;
        };
        SLIDING_PLAYERS.add(player.getUUID());
        player.teleportTo(entrance.getX() + 0.5D, entrance.getY() + 0.05D, entrance.getZ() + 0.5D);
        player.setPose(Pose.SWIMMING);
        player.setDeltaMovement(0.0D, type.equals("straightdown") ? -1.2D : -0.15D, 0.0D);
        player.sendSystemMessage(Component.literal("Built the " + type + " water slide using " + blocks + " blocks."));
        return blocks;
    }

    private static int buildStraightDown(ServerLevel level, BlockPos origin, Direction forward) {
        int count = 0;
        for (int y = 0; y >= -48; y--) {
            count += channel(level, origin.above(y));
        }
        count += pool(level, local(origin, forward, 0, -49, 4), forward, 8);
        return count;
    }

    private static int buildTornado(ServerLevel level, BlockPos origin, Direction forward) {
        int count = 0;
        int samples = 14 * 32;
        for (int i = 0; i < samples; i++) {
            double angle = i * Math.PI * 2.0D / 32.0D;
            int x = (int)Math.round(Math.cos(angle) * 10.0D);
            int z = (int)Math.round(Math.sin(angle) * 10.0D);
            int y = -(i * 24 / samples);
            count += channel(level, local(origin, forward, x, y, z));
        }
        BlockPos exit = local(origin, forward, 11, -24, 0);
        count += verticalLoop(level, exit, forward, 6);
        BlockPos secondLoop = local(exit, forward, 0, 0, 4);
        count += verticalLoop(level, secondLoop, forward, 5);
        count += pool(level, local(secondLoop, forward, 0, -1, 18), forward, 7);
        return count;
    }

    private static int buildKidder(ServerLevel level, BlockPos origin, Direction forward) {
        int count = 0;
        int samples = 64;
        for (int i = 0; i < samples; i++) {
            double angle = i * Math.PI * 2.0D / 32.0D;
            int x = (int)Math.round(Math.cos(angle) * 7.0D);
            int z = (int)Math.round(Math.sin(angle) * 7.0D);
            int y = -(i * 16 / samples);
            count += channel(level, local(origin, forward, x, y, z));
        }
        count += pool(level, local(origin, forward, 8, -17, 5), forward, 6);
        return count;
    }

    private static int buildCrazy(ServerLevel level, BlockPos origin, Direction forward) {
        int count = 0;
        BlockPos cursor = origin;
        for (int loop = 0; loop < 3; loop++) {
            count += verticalLoop(level, cursor, forward, 7);
            cursor = local(cursor, forward, 0, 0, 4);
        }
        for (int ring = 10; ring >= 3; ring--) {
            for (int i = 0; i < 48; i++) {
                double angle = i * Math.PI * 2.0D / 48.0D;
                int x = (int)Math.round(Math.cos(angle) * ring);
                int z = (int)Math.round(Math.sin(angle) * ring);
                count += channel(level, local(cursor, forward, x, -(10 - ring), z));
            }
        }
        BlockPos hole = local(cursor, forward, 0, -38, 0);
        for (int y = -8; y >= -38; y--) {
            count += channel(level, local(cursor, forward, 0, y, 0));
        }
        for (int i = 0; i < 24; i++) {
            count += channel(level, local(hole, forward, 0, i / 3, i));
        }
        count += pool(level, local(hole, forward, 0, 7, 27), forward, 8);
        return count;
    }

    private static int verticalLoop(ServerLevel level, BlockPos center, Direction forward, int radius) {
        int count = 0;
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2.0D / 48.0D;
            int y = (int)Math.round(Math.sin(angle) * radius);
            int z = (int)Math.round((1.0D - Math.cos(angle)) * radius + i * 4.0D / 47.0D);
            count += channel(level, local(center, forward, 0, y, z));
        }
        return count;
    }

    private static int channel(ServerLevel level, BlockPos center) {
        int count = 0;
        count += setFloor(level, center.below());
        count += setWall(level, center.east());
        count += setWall(level, center.west());
        count += setWall(level, center.north());
        count += setWall(level, center.south());
        count += setWall(level, center.above());
        count += set(level, center, Blocks.WATER.defaultBlockState());
        return count;
    }

    private static int setFloor(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.WATER)) return 0;
        return set(level, pos, ModBlocks.WATER_SLIDE_SURFACE.get().defaultBlockState());
    }

    private static int setWall(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.WATER)) return 0;
        return set(level, pos, ModBlocks.WATER_SLIDE_SURFACE.get().defaultBlockState());
    }

    private static int pool(ServerLevel level, BlockPos center, Direction forward, int radius) {
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                BlockPos pos = local(center, forward, x, 0, z);
                count += set(level, pos.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState());
                count += set(level, pos, Blocks.WATER.defaultBlockState());
            }
        }
        return count;
    }

    private static int set(ServerLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) return 0;
        level.setBlock(pos, state, 3);
        return 1;
    }

    private static BlockPos local(BlockPos origin, Direction forward, int right, int up, int ahead) {
        Direction rightDirection = forward.getClockWise();
        return origin.relative(rightDirection, right).relative(forward, ahead).above(up);
    }

    private WaterSlideTools() {}
}
