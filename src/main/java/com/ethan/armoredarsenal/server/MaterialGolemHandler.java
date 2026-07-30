package com.ethan.armoredarsenal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class MaterialGolemHandler {
    private static final double BEDROCK_GOLEM_HEALTH = 500.0;
    private static final double BEDROCK_GOLEM_ATTACK = 45.0;
    private static final double BEDROCK_GOLEM_SPEED = 0.18;

    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isGolemHead(event.getPlacedBlock())) {
            return;
        }

        GolemPattern pattern = findPattern(level, event.getPos());
        if (pattern == null) {
            return;
        }

        Entity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        spawnMaterialGolem(level, player, pattern);
    }

    private static GolemPattern findPattern(ServerLevel level, BlockPos headPos) {
        GolemPattern xAxis = findPattern(level, headPos, Direction.WEST, Direction.EAST);
        if (xAxis != null) {
            return xAxis;
        }

        return findPattern(level, headPos, Direction.NORTH, Direction.SOUTH);
    }

    private static GolemPattern findPattern(ServerLevel level, BlockPos headPos, Direction firstArm, Direction secondArm) {
        BlockPos torsoPos = headPos.below();
        BlockPos basePos = headPos.below(2);
        BlockPos firstArmPos = torsoPos.relative(firstArm);
        BlockPos secondArmPos = torsoPos.relative(secondArm);

        BlockState torsoState = level.getBlockState(torsoPos);
        if (!isAllowedMaterial(torsoState)) {
            return null;
        }

        Block material = torsoState.getBlock();
        if (!sameBlock(level, basePos, material)
                || !sameBlock(level, firstArmPos, material)
                || !sameBlock(level, secondArmPos, material)) {
            return null;
        }

        return new GolemPattern(material, torsoState, headPos.immutable(), torsoPos.immutable(), basePos.immutable(),
                firstArmPos.immutable(), secondArmPos.immutable());
    }

    private static boolean sameBlock(ServerLevel level, BlockPos pos, Block material) {
        return level.getBlockState(pos).is(material);
    }

    private static boolean isAllowedMaterial(BlockState state) {
        return !state.isAir()
                && !state.is(Blocks.IRON_BLOCK)
                && !isGolemHead(state);
    }

    private static boolean isGolemHead(BlockState state) {
        return state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN);
    }

    private static void spawnMaterialGolem(ServerLevel level, ServerPlayer player, GolemPattern pattern) {
        clearPatternBlocks(level, pattern);

        GolemStats stats = statsFor(level, pattern);
        IronGolem golem = EntityType.IRON_GOLEM.spawn(level,
                spawned -> configureGolem(spawned, pattern.material(), stats),
                pattern.basePos(),
                EntitySpawnReason.EVENT,
                false,
                false);

        if (golem == null) {
            restorePatternBlocks(level, pattern);
            if (player != null) {
                player.sendSystemMessage(Component.literal("The " + materialName(pattern.material()) + " Golem could not spawn here."), false);
            }
            return;
        }

        level.playSound(null, pattern.headPos(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0F, 0.65F);
        if (player != null) {
            player.sendSystemMessage(Component.literal("Spawned a " + materialName(pattern.material()) + " Golem."), false);
        }
    }

    private static void clearPatternBlocks(ServerLevel level, GolemPattern pattern) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : pattern.positions()) {
            level.setBlock(pos, air, 3);
        }
    }

    private static void restorePatternBlocks(ServerLevel level, GolemPattern pattern) {
        level.setBlock(pattern.headPos(), Blocks.CARVED_PUMPKIN.defaultBlockState(), 3);
        level.setBlock(pattern.torsoPos(), pattern.materialState(), 3);
        level.setBlock(pattern.basePos(), pattern.materialState(), 3);
        level.setBlock(pattern.firstArmPos(), pattern.materialState(), 3);
        level.setBlock(pattern.secondArmPos(), pattern.materialState(), 3);
    }

    private static void configureGolem(IronGolem golem, Block material, GolemStats stats) {
        setAttribute(golem, Attributes.MAX_HEALTH, stats.maxHealth());
        setAttribute(golem, Attributes.ATTACK_DAMAGE, stats.attackDamage());
        setAttribute(golem, Attributes.MOVEMENT_SPEED, stats.movementSpeed());
        setAttribute(golem, Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResistance());

        golem.setHealth((float)stats.maxHealth());
        golem.setPlayerCreated(true);
        golem.setPersistenceRequired();
        golem.setCustomName(Component.literal(materialName(material) + " Golem"));
        golem.setCustomNameVisible(true);
    }

    private static void setAttribute(IronGolem golem, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = golem.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static GolemStats statsFor(ServerLevel level, GolemPattern pattern) {
        if (pattern.material().equals(Blocks.BEDROCK)) {
            return new GolemStats(BEDROCK_GOLEM_HEALTH, BEDROCK_GOLEM_ATTACK, BEDROCK_GOLEM_SPEED, 1.0);
        }

        float destroySpeed = pattern.materialState().getDestroySpeed(level, pattern.torsoPos());
        double hardness = destroySpeed < 0.0F ? 50.0 : Math.min(50.0, destroySpeed);
        double resistance = Math.min(1200.0, Math.max(0.0, pattern.material().getExplosionResistance())) / 10.0;
        double toughness = hardness + resistance;

        double maxHealth = clamp(80.0 + toughness * 4.0, 60.0, 350.0);
        double attackDamage = clamp(10.0 + toughness * 0.25, 8.0, 30.0);
        double movementSpeed = clamp(0.28 - toughness * 0.0015, 0.18, 0.28);
        return new GolemStats(maxHealth, attackDamage, movementSpeed, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String materialName(Block material) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(material);
        String raw = id == null ? "material" : id.getPath();
        String[] words = raw.split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                name.append(word.substring(1));
            }
        }
        return name.isEmpty() ? "Material" : name.toString();
    }

    private record GolemPattern(Block material, BlockState materialState, BlockPos headPos, BlockPos torsoPos,
                                BlockPos basePos, BlockPos firstArmPos, BlockPos secondArmPos) {
        private BlockPos[] positions() {
            return new BlockPos[] {headPos, torsoPos, basePos, firstArmPos, secondArmPos};
        }
    }

    private record GolemStats(double maxHealth, double attackDamage, double movementSpeed, double knockbackResistance) {}

    private MaterialGolemHandler() {}
}