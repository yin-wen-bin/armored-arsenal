package com.ethan.armoredarsenal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class MaterialGolemHandler {
    private static final String MATERIAL_KEY = "ArmoredArsenalGolemMaterial";
    private static final String DURABILITY_KEY = "ArmoredArsenalGolemDurability";
    private static final String DISPLAY_KEY = "ArmoredArsenalGolemDisplay";
    private static final double SUPPORTED_HEALTH = 1024.0;
    private static final double DIAMOND_EFFECTIVE_HEARTS = 99_999_999.0;
    private static final double NETHERITE_EFFECTIVE_HEARTS = 999_999_999.0;
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

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) {
            return;
        }

        CompoundTag data = golem.getPersistentData();
        double durability = data.getDoubleOr(DURABILITY_KEY, 0.0);
        if (durability < 1.0) {
            return;
        }

        if (data.getStringOr(MATERIAL_KEY, "").equals("minecraft:bedrock")) {
            event.setCanceled(true);
            return;
        }

        event.setAmount((float)Math.max(0.000001, event.getAmount() * SUPPORTED_HEALTH / (durability * 2.0)));
    }

    public static void afterEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.getPersistentData().getBooleanOr(DISPLAY_KEY, false) && entity.getVehicle() == null) {
            entity.discard();
            return;
        }

        if (entity instanceof IronGolem golem && !golem.level().isClientSide()
                && golem.getPersistentData().contains(MATERIAL_KEY) && !hasMaterialBody(golem)) {
            String materialName = golem.getPersistentData().getStringOr(MATERIAL_KEY, "minecraft:iron_block");
            Identifier materialId = Identifier.tryParse(materialName);
            if (materialId != null) {
                golem.setInvisible(true);
                addMaterialBody(golem, materialId);
            }
        }
    }

    private static boolean hasMaterialBody(IronGolem golem) {
        return golem.getPassengers().stream()
                .anyMatch(passenger -> passenger.getPersistentData().getBooleanOr(DISPLAY_KEY, false));
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
        Identifier materialId = BuiltInRegistries.BLOCK.getKey(material);
        golem.getPersistentData().putString(MATERIAL_KEY, materialId.toString());
        golem.getPersistentData().putDouble(DURABILITY_KEY, stats.effectiveHearts());
        golem.setInvisible(true);
        addMaterialBody(golem, materialId);
    }

    private static void setAttribute(IronGolem golem, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = golem.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static GolemStats statsFor(ServerLevel level, GolemPattern pattern) {
        if (pattern.material().equals(Blocks.BEDROCK)) {
            return new GolemStats(SUPPORTED_HEALTH, BEDROCK_GOLEM_ATTACK, BEDROCK_GOLEM_SPEED, 1.0, Double.MAX_VALUE);
        }
        if (pattern.material().equals(Blocks.NETHERITE_BLOCK)) {
            return new GolemStats(SUPPORTED_HEALTH, 38.0, 0.20, 1.0, NETHERITE_EFFECTIVE_HEARTS);
        }
        if (pattern.material().equals(Blocks.DIAMOND_BLOCK)) {
            return new GolemStats(SUPPORTED_HEALTH, 32.0, 0.22, 1.0, DIAMOND_EFFECTIVE_HEARTS);
        }

        float destroySpeed = pattern.materialState().getDestroySpeed(level, pattern.torsoPos());
        double hardness = destroySpeed < 0.0F ? 50.0 : Math.min(50.0, destroySpeed);
        double resistance = Math.min(1200.0, Math.max(0.0, pattern.material().getExplosionResistance())) / 10.0;
        double toughness = hardness + resistance;

        double maxHealth = clamp(80.0 + toughness * 4.0, 60.0, 350.0);
        double attackDamage = clamp(10.0 + toughness * 0.25, 8.0, 30.0);
        double movementSpeed = clamp(0.28 - toughness * 0.0015, 0.18, 0.28);
        return new GolemStats(maxHealth, attackDamage, movementSpeed, 1.0, 0.0);
    }

    private static void addMaterialBody(IronGolem golem, Identifier materialId) {
        addDisplay(golem, materialId, -0.55F, 0.55F, -0.35F, 1.10F, 1.15F, 0.70F);
        addDisplay(golem, materialId, -1.15F, 0.65F, -0.30F, 0.60F, 1.75F, 0.60F);
        addDisplay(golem, materialId, 0.55F, 0.65F, -0.30F, 0.60F, 1.75F, 0.60F);
        addDisplay(golem, materialId, -0.50F, -0.95F, -0.25F, 0.45F, 1.55F, 0.50F);
        addDisplay(golem, materialId, 0.05F, -0.95F, -0.25F, 0.45F, 1.55F, 0.50F);
        addDisplay(golem, Identifier.fromNamespaceAndPath("minecraft", "carved_pumpkin"), -0.45F, 1.70F, -0.45F, 0.90F, 0.90F, 0.90F);
    }

    private static void addDisplay(IronGolem golem, Identifier blockId, float x, float y, float z,
                                   float sx, float sy, float sz) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }

        CompoundTag nbt = new CompoundTag();
        CompoundTag blockState = new CompoundTag();
        blockState.putString("Name", blockId.toString());
        nbt.put("block_state", blockState);
        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", vector(x, y, z));
        transformation.put("scale", vector(sx, sy, sz));
        transformation.put("left_rotation", quaternionIdentity());
        transformation.put("right_rotation", quaternionIdentity());
        nbt.put("transformation", transformation);
        try {
            Entity display = SummonCommand.createEntity(level.getServer().createCommandSourceStack(),
                    BuiltInRegistries.ENTITY_TYPE.get(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BLOCK_DISPLAY)).orElseThrow(),
                    golem.position(), nbt, false);
            display.getPersistentData().putBoolean(DISPLAY_KEY, true);
            display.startRiding(golem, true, true);
        } catch (Exception ignored) {
            // A missing block model should not prevent the combat entity from spawning.
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

    private record GolemStats(double maxHealth, double attackDamage, double movementSpeed,
                              double knockbackResistance, double effectiveHearts) {}

    private MaterialGolemHandler() {}
}
