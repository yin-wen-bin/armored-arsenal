package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class GiantStructureTools {
    private static final int SCALE = 3;
    private static final int TARGET_RANGE = 96;
    private static final int SCAN_RADIUS = 18;
    private static final int SURROUNDING_MARGIN = 6;
    private static final int MAX_STRUCTURE_BLOCKS = 8_192;
    private static final int MAX_EDIT_BLOCKS = 262_144;
    private static final int FLOORS = 3;
    private static final int FLOOR_HEIGHT = 5;

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("giant")
                .executes(context -> enlarge(context.getSource().getPlayerOrException()));
    }

    private static int enlarge(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos target = lookedAtBlock(player);
        if (target == null) {
            message(player, "Look directly at a structure within " + TARGET_RANGE + " blocks, then type //giant.");
            return 0;
        }

        int groundY = estimateGroundY(level, target);
        BlockPos seed = findStructureSeed(level, target, groundY);
        if (seed == null) {
            message(player, "Aim at a wall above the ground and try //giant again.");
            return 0;
        }

        List<BlockPos> structure = discoverStructure(level, seed, groundY);
        if (structure.size() < 4) {
            message(player, "That does not look like a connected structure. Aim at one of its walls.");
            return 0;
        }
        if (structure.size() >= MAX_STRUCTURE_BLOCKS) {
            message(player, "That structure is too large to enlarge safely. Maximum detected size is "
                    + MAX_STRUCTURE_BLOCKS + " blocks.");
            return 0;
        }

        StructureBounds bounds = StructureBounds.around(structure);
        GiantRegion region = createRegion(level, bounds, groundY);
        if (region.editVolume() > MAX_EDIT_BLOCKS) {
            message(player, "The mansion and enlarged surroundings would use " + region.editVolume()
                    + " blocks. The safety limit is " + MAX_EDIT_BLOCKS + ".");
            return 0;
        }

        int mansionTop = groundY + FLOORS * FLOOR_HEIGHT + 3;
        if (Math.max(region.destinationMaxY(), mansionTop) >= level.getMaxY()) {
            message(player, "There is not enough space below the world height limit.");
            return 0;
        }

        message(player, "Transforming the structure and its surroundings into a giant mansion...");
        BlockState[] snapshot = takeSnapshot(level, region);
        int changed = scaleSurroundings(level, region, snapshot);
        Direction front = frontTowardPlayer(player, bounds);
        changed += buildMansion(level, bounds, groundY, front);
        movePlayerToSafety(player, bounds, groundY, front);
        message(player, "Giant mansion complete: " + changed + " blocks changed. Nearby paths, fences, gardens, "
                + "trees, and terrain were enlarged with it.");
        return Math.max(1, changed);
    }

    private static BlockPos lookedAtBlock(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(TARGET_RANGE));
        BlockHitResult hit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.of(player)));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().immutable() : null;
    }

    private static int estimateGroundY(ServerLevel level, BlockPos target) {
        int radius = 8;
        List<Integer> heights = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                    continue;
                }
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        target.getX() + dx, target.getZ() + dz) - 1;
                if (height >= level.getMinY()) {
                    heights.add(height);
                }
            }
        }
        heights.sort(Comparator.naturalOrder());
        return heights.isEmpty() ? target.getY() - 1 : heights.get(heights.size() / 2);
    }

    private static BlockPos findStructureSeed(ServerLevel level, BlockPos target, int groundY) {
        if (target.getY() > groundY && isStructureBlock(level.getBlockState(target))) {
            return target;
        }

        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = groundY + 1; y <= Math.min(groundY + 16, level.getMaxY() - 1); y++) {
                    BlockPos candidate = new BlockPos(target.getX() + dx, y, target.getZ() + dz);
                    if (!isStructureBlock(level.getBlockState(candidate))) {
                        continue;
                    }
                    int distance = candidate.distManhattan(target);
                    if (distance < bestDistance) {
                        best = candidate.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private static List<BlockPos> discoverStructure(ServerLevel level, BlockPos seed, int groundY) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        List<BlockPos> structure = new ArrayList<>();
        pending.add(seed);

        int maximumY = Math.min(level.getMaxY() - 1, groundY + 32);
        while (!pending.isEmpty() && structure.size() < MAX_STRUCTURE_BLOCKS) {
            BlockPos pos = pending.removeFirst();
            if (!visited.add(pos.asLong()) || pos.getY() <= groundY || pos.getY() > maximumY
                    || Math.abs(pos.getX() - seed.getX()) > SCAN_RADIUS
                    || Math.abs(pos.getZ() - seed.getZ()) > SCAN_RADIUS
                    || !isStructureBlock(level.getBlockState(pos))) {
                continue;
            }

            structure.add(pos.immutable());
            for (Direction direction : Direction.values()) {
                pending.addLast(pos.relative(direction));
            }
        }
        return structure;
    }

    private static boolean isStructureBlock(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static GiantRegion createRegion(ServerLevel level, StructureBounds bounds, int groundY) {
        int minX = bounds.minX() - SURROUNDING_MARGIN;
        int maxX = bounds.maxX() + SURROUNDING_MARGIN;
        int minZ = bounds.minZ() - SURROUNDING_MARGIN;
        int maxZ = bounds.maxZ() + SURROUNDING_MARGIN;
        int minY = groundY;
        int maximumSurface = bounds.maxY() + 8;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                maximumSurface = Math.max(maximumSurface,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
            }
        }
        int maxY = Math.min(level.getMaxY() - 1, Math.min(groundY + 32, maximumSurface));

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int destinationWidth = width * SCALE;
        int destinationDepth = depth * SCALE;
        int centerXTwice = minX + maxX + 1;
        int centerZTwice = minZ + maxZ + 1;
        int destinationMinX = Math.floorDiv(centerXTwice - destinationWidth, 2);
        int destinationMinZ = Math.floorDiv(centerZTwice - destinationDepth, 2);
        return new GiantRegion(new BlockPos(minX, minY, minZ), width, maxY - minY + 1, depth,
                new BlockPos(destinationMinX, minY, destinationMinZ));
    }

    private static BlockState[] takeSnapshot(ServerLevel level, GiantRegion region) {
        BlockState[] snapshot = new BlockState[region.sourceVolume()];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < region.height(); y++) {
            for (int z = 0; z < region.depth(); z++) {
                for (int x = 0; x < region.width(); x++) {
                    cursor.set(region.sourceMin().getX() + x, region.sourceMin().getY() + y,
                            region.sourceMin().getZ() + z);
                    snapshot[region.sourceIndex(x, y, z)] = level.getBlockState(cursor);
                }
            }
        }
        return snapshot;
    }

    private static int scaleSurroundings(ServerLevel level, GiantRegion region, BlockState[] snapshot) {
        int changed = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < region.destinationHeight(); y++) {
            int sourceY = y == 0 ? 0 : 1 + (y - 1) / SCALE;
            for (int z = 0; z < region.destinationDepth(); z++) {
                for (int x = 0; x < region.destinationWidth(); x++) {
                    BlockState state = snapshot[region.sourceIndex(x / SCALE, sourceY, z / SCALE)];
                    cursor.set(region.destinationMin().getX() + x, region.destinationMin().getY() + y,
                            region.destinationMin().getZ() + z);
                    changed += place(level, cursor, state);
                }
            }
        }
        return changed;
    }

    private static int buildMansion(ServerLevel level, StructureBounds original, int groundY, Direction front) {
        int width = oddAtLeast(original.width() * SCALE, 21);
        int depth = oddAtLeast(original.depth() * SCALE, 17);
        int centerX = (original.minX() + original.maxX()) / 2;
        int centerZ = (original.minZ() + original.maxZ()) / 2;
        int minX = centerX - width / 2;
        int maxX = centerX + width / 2;
        int minZ = centerZ - depth / 2;
        int maxZ = centerZ + depth / 2;
        int wallTop = groundY + FLOORS * FLOOR_HEIGHT;
        int changed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                changed += place(level, pos.set(x, groundY, z), Blocks.STONE_BRICKS.defaultBlockState());
                for (int y = groundY + 1; y <= wallTop; y++) {
                    boolean edgeX = x == minX || x == maxX;
                    boolean edgeZ = z == minZ || z == maxZ;
                    boolean edge = edgeX || edgeZ;
                    int relativeY = y - (groundY + 1);
                    boolean floor = relativeY % FLOOR_HEIGHT == 0;
                    BlockState state;

                    if (floor) {
                        state = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    } else if (!edge) {
                        state = Blocks.AIR.defaultBlockState();
                    } else {
                        int along = edgeZ ? x - minX : z - minZ;
                        boolean pillar = (edgeX && edgeZ) || along % 5 == 0;
                        boolean windowHeight = relativeY % FLOOR_HEIGHT == 2
                                || relativeY % FLOOR_HEIGHT == 3;
                        boolean windowBay = along > 1 && along < (edgeZ ? width : depth) - 2
                                && (along % 5 == 2 || along % 5 == 3);
                        if (isEntrance(x, z, y, centerX, centerZ, groundY, front, minX, maxX, minZ, maxZ)) {
                            state = Blocks.AIR.defaultBlockState();
                        } else if (pillar) {
                            state = Blocks.DARK_OAK_LOG.defaultBlockState();
                        } else if (windowHeight && windowBay) {
                            state = Blocks.GLASS.defaultBlockState();
                        } else {
                            state = Blocks.QUARTZ_BLOCK.defaultBlockState();
                        }
                    }
                    changed += place(level, pos.set(x, y, z), state);
                }
            }
        }

        for (int tier = 0; tier < 3; tier++) {
            int roofY = wallTop + 1 + tier;
            int roofMinX = minX - 1 + tier * 2;
            int roofMaxX = maxX + 1 - tier * 2;
            int roofMinZ = minZ - 1 + tier * 2;
            int roofMaxZ = maxZ + 1 - tier * 2;
            for (int x = roofMinX; x <= roofMaxX; x++) {
                for (int z = roofMinZ; z <= roofMaxZ; z++) {
                    changed += place(level, pos.set(x, roofY, z), Blocks.DEEPSLATE_TILES.defaultBlockState());
                }
            }
        }

        changed += addEntranceAndPath(level, centerX, centerZ, groundY, front, minX, maxX, minZ, maxZ);
        return changed;
    }

    private static boolean isEntrance(int x, int z, int y, int centerX, int centerZ, int groundY,
                                      Direction front, int minX, int maxX, int minZ, int maxZ) {
        if (y < groundY + 2 || y > groundY + 5) {
            return false;
        }
        return switch (front) {
            case NORTH -> z == minZ && Math.abs(x - centerX) <= 2;
            case SOUTH -> z == maxZ && Math.abs(x - centerX) <= 2;
            case WEST -> x == minX && Math.abs(z - centerZ) <= 2;
            case EAST -> x == maxX && Math.abs(z - centerZ) <= 2;
            default -> false;
        };
    }

    private static int addEntranceAndPath(ServerLevel level, int centerX, int centerZ, int groundY,
                                           Direction front, int minX, int maxX, int minZ, int maxZ) {
        int changed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int edgeX = front == Direction.EAST ? maxX : front == Direction.WEST ? minX : centerX;
        int edgeZ = front == Direction.SOUTH ? maxZ : front == Direction.NORTH ? minZ : centerZ;
        for (int distance = 0; distance <= 10; distance++) {
            int pathX = edgeX + front.getStepX() * distance;
            int pathZ = edgeZ + front.getStepZ() * distance;
            for (int sideways = -2; sideways <= 2; sideways++) {
                int x = pathX + (front.getAxis() == Direction.Axis.Z ? sideways : 0);
                int z = pathZ + (front.getAxis() == Direction.Axis.X ? sideways : 0);
                changed += place(level, pos.set(x, groundY, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                changed += place(level, pos.set(x, groundY + 1, z), Blocks.AIR.defaultBlockState());
            }
        }

        for (int side : new int[] {-3, 3}) {
            int x = edgeX + (front.getAxis() == Direction.Axis.Z ? side : 0);
            int z = edgeZ + (front.getAxis() == Direction.Axis.X ? side : 0);
            for (int y = groundY + 1; y <= groundY + 5; y++) {
                changed += place(level, pos.set(x, y, z), Blocks.DARK_OAK_LOG.defaultBlockState());
            }
            changed += place(level, pos.set(x, groundY + 6, z), Blocks.SEA_LANTERN.defaultBlockState());
        }
        return changed;
    }

    private static Direction frontTowardPlayer(ServerPlayer player, StructureBounds bounds) {
        int dx = player.blockPosition().getX() - (bounds.minX() + bounds.maxX()) / 2;
        int dz = player.blockPosition().getZ() - (bounds.minZ() + bounds.maxZ()) / 2;
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static void movePlayerToSafety(ServerPlayer player, StructureBounds bounds, int groundY, Direction front) {
        int centerX = (bounds.minX() + bounds.maxX()) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ()) / 2;
        int width = oddAtLeast(bounds.width() * SCALE, 21);
        int depth = oddAtLeast(bounds.depth() * SCALE, 17);
        int distance = (front.getAxis() == Direction.Axis.X ? width : depth) / 2 + 13;
        int x = centerX + front.getStepX() * distance;
        int z = centerZ + front.getStepZ() * distance;
        int y = Math.max(groundY + 1,
                player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1);
        player.teleportTo(x + 0.5, y, z + 0.5);
    }

    private static int oddAtLeast(int value, int minimum) {
        int result = Math.max(value, minimum);
        return result % 2 == 0 ? result + 1 : result;
    }

    private static int place(ServerLevel level, BlockPos pos, BlockState state) {
        return !level.getBlockState(pos).equals(state) && level.setBlock(pos, state, 2) ? 1 : 0;
    }

    private static void message(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text), false);
    }

    private record StructureBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        private static StructureBounds around(List<BlockPos> blocks) {
            return new StructureBounds(
                    blocks.stream().mapToInt(BlockPos::getX).min().orElseThrow(),
                    blocks.stream().mapToInt(BlockPos::getX).max().orElseThrow(),
                    blocks.stream().mapToInt(BlockPos::getY).min().orElseThrow(),
                    blocks.stream().mapToInt(BlockPos::getY).max().orElseThrow(),
                    blocks.stream().mapToInt(BlockPos::getZ).min().orElseThrow(),
                    blocks.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int depth() {
            return maxZ - minZ + 1;
        }
    }

    private record GiantRegion(BlockPos sourceMin, int width, int height, int depth, BlockPos destinationMin) {
        private int sourceVolume() {
            return width * height * depth;
        }

        private int sourceIndex(int x, int y, int z) {
            return (y * depth + z) * width + x;
        }

        private int destinationWidth() {
            return width * SCALE;
        }

        private int destinationHeight() {
            return height == 1 ? 1 : 1 + (height - 1) * SCALE;
        }

        private int destinationDepth() {
            return depth * SCALE;
        }

        private int destinationMaxY() {
            return destinationMin.getY() + destinationHeight() - 1;
        }

        private long editVolume() {
            return (long)destinationWidth() * destinationHeight() * destinationDepth();
        }
    }

    private GiantStructureTools() {}
}
