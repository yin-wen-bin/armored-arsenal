package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class ColossusGolemHandler {
    private static final String COLOSSUS_KEY = "ArmoredArsenalColossus";
    private static final String SHAPE_KEY = "ArmoredArsenalColossusShape";
    private static final String PART_KEY = "ArmoredArsenalColossusPart";
    private static final String DISPLAY_KEY = "ArmoredArsenalGolemDisplay";
    private static final int SEARCH_RADIUS = 22;
    private static final int SEARCH_HEIGHT = 50;
    private static final int MAX_BLOCKS = 512;
    private static final Map<UUID, Preview> PREVIEWS = new HashMap<>();

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("golem")
                .then(Commands.literal("inspect")
                        .executes(context -> inspect(context.getSource().getPlayerOrException())))
                .then(Commands.literal("awaken")
                        .executes(context -> awaken(context.getSource().getPlayerOrException()))));
    }

    public static void afterEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof IronGolem golem) || golem.level().isClientSide()
                || !golem.getPersistentData().getBooleanOr(COLOSSUS_KEY, false)) {
            return;
        }
        if (golem.tickCount % 10 == 0 && golem.getPassengers().stream()
                .noneMatch(passenger -> passenger.getPersistentData().getBooleanOr(PART_KEY, false))) {
            if (addBody(golem) == 0) {
                golem.setInvisible(false);
            }
        }
        if (golem.tickCount % 2 == 0) {
            for (Entity passenger : golem.getPassengers()) {
                if (passenger.getPersistentData().getBooleanOr(PART_KEY, false)) {
                    passenger.setYRot(golem.getYRot());
                }
            }
        }
    }

    private static int inspect(ServerPlayer player) {
        Structure structure = findStructure(player);
        if (structure == null) {
            message(player, "Stand within 22 blocks of the mixed diamond-and-netherite structure, then try again.");
            return 0;
        }
        if (structure.blocks().size() > MAX_BLOCKS) {
            message(player, "That structure has too many blocks to animate safely (limit " + MAX_BLOCKS + ").");
            return 0;
        }
        PREVIEWS.put(player.getUUID(), new Preview(player.level().dimension().identifier().toString(),
                player.level().getGameTime() + 2400, structure.blocks()));
        message(player, "Mixed golem ready: " + structure.blocks().size() + " blocks, "
                + structure.pumpkins() + " pumpkins, " + structure.heads() + " heads. Bounds "
                + structure.minX() + ".." + structure.maxX() + ", "
                + structure.minY() + ".." + structure.maxY() + ", "
                + structure.minZ() + ".." + structure.maxZ()
                + ". Type /golem awaken within two minutes to animate these blocks.");
        return 1;
    }

    private static int awaken(ServerPlayer player) {
        Preview preview = PREVIEWS.get(player.getUUID());
        if (preview == null || preview.expiresAt() < player.level().getGameTime()
                || !preview.dimension().equals(player.level().dimension().identifier().toString())) {
            message(player, "Run /golem inspect first to confirm the blocks that will become the golem.");
            return 0;
        }
        Structure structure = findStructure(player);
        if (structure == null || !structure.blocks().equals(preview.blocks())) {
            PREVIEWS.remove(player.getUUID());
            message(player, "The structure changed or is out of range. Run /golem inspect again.");
            return 0;
        }
        PREVIEWS.remove(player.getUUID());
        ServerLevel level = player.level();
        BlockPos anchor = new BlockPos((structure.minX() + structure.maxX()) / 2,
                structure.minY(), (structure.minZ() + structure.maxZ()) / 2);
        for (CapturedBlock block : structure.blocks()) {
            level.setBlock(block.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
        IronGolem golem = EntityType.IRON_GOLEM.spawn(level,
                entity -> configure(entity, structure, anchor), anchor,
                EntitySpawnReason.EVENT, false, false);
        if (golem == null) {
            restore(level, structure.blocks());
            message(player, "The golem could not spawn. Your structure was restored.");
            return 0;
        }
        golem.setYRot(0.0F);
        golem.setYHeadRot(0.0F);
        int displayed = addBody(golem);
        if (displayed == 0) {
            golem.discard();
            restore(level, structure.blocks());
            message(player, "The golem body could not render. Your structure was restored.");
            return 0;
        }
        level.playSound(null, anchor, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 3.0F, 0.45F);
        message(player, "Your Netherite-Diamond Colossus is alive. " + displayed + " blocks form its moving body.");
        return 1;
    }

    private static Structure findStructure(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        int minX = center.getX() - SEARCH_RADIUS;
        int maxX = center.getX() + SEARCH_RADIUS;
        int minZ = center.getZ() - SEARCH_RADIUS;
        int maxZ = center.getZ() + SEARCH_RADIUS;
        int minY = Math.max(level.getMinY(), center.getY() - SEARCH_HEIGHT);
        int maxY = Math.min(level.getMaxY() - 1, center.getY() + SEARCH_HEIGHT);
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> largest = List.of();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    if (!isPart(level.getBlockState(cursor)) || visited.contains(cursor)) {
                        continue;
                    }
                    List<BlockPos> component = connectedComponent(level, cursor.immutable(), visited,
                            minX, maxX, minY, maxY, minZ, maxZ);
                    if (component.size() > largest.size() && qualifies(level, component)) {
                        largest = component;
                    }
                }
            }
        }
        if (largest.isEmpty()) {
            return null;
        }
        int bodyMinX = largest.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int bodyMaxX = largest.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int bodyMinY = largest.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        int bodyMaxY = largest.stream().mapToInt(BlockPos::getY).max().orElseThrow();
        int bodyMinZ = largest.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        int bodyMaxZ = largest.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        List<CapturedBlock> blocks = new ArrayList<>();
        int pumpkins = 0;
        int heads = 0;
        for (int y = bodyMinY; y <= Math.min(level.getMaxY() - 1, bodyMaxY + 5); y++) {
            for (int x = bodyMinX - 6; x <= bodyMaxX + 6; x++) {
                for (int z = bodyMinZ - 3; z <= bodyMaxZ + 3; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isPart(state)) {
                        continue;
                    }
                    blocks.add(new CapturedBlock(cursor.immutable(), state));
                    if (isPumpkin(state)) {
                        pumpkins++;
                    } else if (state.getBlock() instanceof AbstractSkullBlock) {
                        heads++;
                    }
                    if (blocks.size() > MAX_BLOCKS) {
                        return new Structure(blocks, pumpkins, heads, 0, 0, 0, 0, 0, 0);
                    }
                }
            }
        }
        int capturedMinX = blocks.stream().mapToInt(block -> block.pos().getX()).min().orElseThrow();
        int capturedMaxX = blocks.stream().mapToInt(block -> block.pos().getX()).max().orElseThrow();
        int capturedMinY = blocks.stream().mapToInt(block -> block.pos().getY()).min().orElseThrow();
        int capturedMaxY = blocks.stream().mapToInt(block -> block.pos().getY()).max().orElseThrow();
        int capturedMinZ = blocks.stream().mapToInt(block -> block.pos().getZ()).min().orElseThrow();
        int capturedMaxZ = blocks.stream().mapToInt(block -> block.pos().getZ()).max().orElseThrow();
        return new Structure(List.copyOf(blocks), pumpkins, heads,
                capturedMinX, capturedMaxX, capturedMinY, capturedMaxY, capturedMinZ, capturedMaxZ);
    }

    private static List<BlockPos> connectedComponent(ServerLevel level, BlockPos seed, Set<BlockPos> visited,
                                                      int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> component = new ArrayList<>();
        queue.add(seed);
        visited.add(seed);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            component.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (next.getX() < minX || next.getX() > maxX
                        || next.getY() < minY || next.getY() > maxY
                        || next.getZ() < minZ || next.getZ() > maxZ
                        || visited.contains(next) || !isPart(level.getBlockState(next))) {
                    continue;
                }
                visited.add(next);
                queue.addLast(next);
            }
        }
        return component;
    }

    private static boolean qualifies(ServerLevel level, List<BlockPos> component) {
        if (component.size() < 40) {
            return false;
        }
        boolean diamond = false;
        boolean netherite = false;
        boolean head = false;
        for (BlockPos pos : component) {
            BlockState state = level.getBlockState(pos);
            diamond |= state.is(Blocks.DIAMOND_BLOCK);
            netherite |= state.is(Blocks.NETHERITE_BLOCK);
            head |= isPumpkin(state) || state.getBlock() instanceof AbstractSkullBlock;
        }
        return diamond && netherite && head;
    }

    private static boolean isPart(BlockState state) {
        return state.is(Blocks.DIAMOND_BLOCK) || state.is(Blocks.NETHERITE_BLOCK)
                || isPumpkin(state) || state.getBlock() instanceof AbstractSkullBlock;
    }

    private static boolean isPumpkin(BlockState state) {
        return state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN)
                || state.is(Blocks.JACK_O_LANTERN);
    }

    private static void configure(IronGolem golem, Structure structure, BlockPos anchor) {
        setAttribute(golem, Attributes.MAX_HEALTH, 2500.0);
        setAttribute(golem, Attributes.ATTACK_DAMAGE, 65.0);
        setAttribute(golem, Attributes.MOVEMENT_SPEED, 0.22);
        setAttribute(golem, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        setAttribute(golem, Attributes.SCALE,
                Math.min(16.0, (structure.maxY() - structure.minY() + 1) / 2.7));
        setAttribute(golem, Attributes.STEP_HEIGHT, 3.0);
        golem.setHealth(2500.0F);
        golem.setPlayerCreated(true);
        golem.setPersistenceRequired();
        golem.setCustomName(Component.literal("Netherite-Diamond Colossus"));
        golem.setCustomNameVisible(true);
        golem.setInvisible(true);
        CompoundTag data = golem.getPersistentData();
        data.putBoolean(COLOSSUS_KEY, true);
        ListTag shape = new ListTag();
        for (CapturedBlock block : structure.blocks()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", block.pos().getX() - anchor.getX());
            entry.putInt("y", block.pos().getY() - anchor.getY());
            entry.putInt("z", block.pos().getZ() - anchor.getZ());
            entry.put("state", NbtUtils.writeBlockState(block.state()));
            shape.add(entry);
        }
        data.put(SHAPE_KEY, shape);
    }

    private static int addBody(IronGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return 0;
        }
        double ridingOffset = golem.getPassengerRidingPosition(golem).y - golem.getY();
        int added = 0;
        for (Tag tag : golem.getPersistentData().getListOrEmpty(SHAPE_KEY)) {
            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }
            CompoundTag nbt = new CompoundTag();
            nbt.put("block_state", entry.getCompoundOrEmpty("state").copy());
            nbt.putFloat("view_range", 4.0F);
            CompoundTag transformation = new CompoundTag();
            transformation.put("translation", vector(entry.getIntOr("x", 0) - 0.5F,
                    entry.getIntOr("y", 0) - (float) ridingOffset,
                    entry.getIntOr("z", 0) - 0.5F));
            transformation.put("scale", vector(1.0F, 1.0F, 1.0F));
            transformation.put("left_rotation", quaternionIdentity());
            transformation.put("right_rotation", quaternionIdentity());
            nbt.put("transformation", transformation);
            try {
                Entity display = SummonCommand.createEntity(level.getServer().createCommandSourceStack(),
                        level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).getOrThrow(EntityType.BLOCK_DISPLAY.builtInRegistryHolder().key()),
                        golem.position(), nbt, false);
                display.getPersistentData().putBoolean(DISPLAY_KEY, true);
                display.getPersistentData().putBoolean(PART_KEY, true);
                display.startRiding(golem, true, true);
                display.setYRot(golem.getYRot());
                added++;
            } catch (Exception ignored) {
                // A single invalid display must not make the living golem disappear.
            }
        }
        return added;
    }

    private static void setAttribute(IronGolem golem,
                                     net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                     double value) {
        AttributeInstance instance = golem.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void restore(ServerLevel level, List<CapturedBlock> blocks) {
        for (CapturedBlock block : blocks) {
            level.setBlock(block.pos(), block.state(), 3);
        }
    }

    private static ListTag vector(float x, float y, float z) {
        ListTag values = new ListTag();
        values.add(FloatTag.valueOf(x));
        values.add(FloatTag.valueOf(y));
        values.add(FloatTag.valueOf(z));
        return values;
    }

    private static ListTag quaternionIdentity() {
        ListTag values = new ListTag();
        values.add(FloatTag.valueOf(0.0F));
        values.add(FloatTag.valueOf(0.0F));
        values.add(FloatTag.valueOf(0.0F));
        values.add(FloatTag.valueOf(1.0F));
        return values;
    }

    private static void message(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text), false);
    }

    private record CapturedBlock(BlockPos pos, BlockState state) {}

    private record Structure(List<CapturedBlock> blocks, int pumpkins, int heads,
                             int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {}

    private record Preview(String dimension, long expiresAt, List<CapturedBlock> blocks) {}

    private ColossusGolemHandler() {}
}
