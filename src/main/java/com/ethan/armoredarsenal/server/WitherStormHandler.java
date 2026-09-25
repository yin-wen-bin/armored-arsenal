package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.network.StormDeathPayload;
import com.ethan.armoredarsenal.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WitherStormHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WitherStormHandler.class);
    private static final String MARKER = "ArmoredArsenalWitherStorm";
    private static final String PART = "ArmoredArsenalStormPart";
    private static final String OWNER = "ArmoredArsenalStormOwner";
    private static final String HEAD = "ArmoredArsenalStormHead";
    private static final String DEATH_TICKS = "ArmoredArsenalStormDeathTicks";
    private static final String MIGRATED = "ArmoredArsenalStormNineHeaded";
    private static final String CORE_HIT = "ArmoredArsenalStormCoreHit";
    private static final String INSIDE = "ArmoredArsenalInsideStorm";
    private static final String RETURN_DIMENSION = "ArmoredArsenalStormReturnDimension";
    private static final String RETURN_X = "ArmoredArsenalStormReturnX";
    private static final String RETURN_Y = "ArmoredArsenalStormReturnY";
    private static final String RETURN_Z = "ArmoredArsenalStormReturnZ";
    private static final BlockPos CORE = new BlockPos(250000, 121, 8);
    private static final BlockPos ARRIVAL = new BlockPos(250000, 121, -7);
    private static final double[] HEAD_X = {-8.0, 0.0, 8.0};
    private static final double[] HEAD_Y = {5.0, 11.0, 17.0};

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("witherstorm")
                .executes(context -> summon(context.getSource()))
                .then(Commands.literal("clear")
                        .executes(context -> clear(context.getSource().getPlayerOrException())))
                .then(Commands.literal("axe")
                        .executes(context -> giveAxe(context.getSource().getPlayerOrException()))));
    }

    private static int summon(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return spawn(player);
        }
        return spawnAt(source.getLevel(), BlockPos.containing(source.getPosition()), null);
    }

    public static int giveAxe(ServerPlayer player) {
        ItemStack axe = new ItemStack(ModItems.COMMAND_BLOCK_AXE.get());
        if (!player.getInventory().add(axe)) {
            player.drop(axe, false);
        }
        player.sendSystemMessage(Component.literal("Command Block Axe ready. Strike the core with it."), false);
        return 1;
    }

    public static int spawn(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        int y = Math.min(level.getMaxY() - 36, Math.max(level.getMinY() + 24, player.blockPosition().getY() + 24));
        BlockPos pos = BlockPos.containing(player.getX() + look.x * 45.0, y, player.getZ() + look.z * 45.0);
        if (level.dimension().equals(Level.OVERWORLD)) {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
            pos = new BlockPos(pos.getX(), Math.min(level.getMaxY() - 30, Math.max(y, surface + 26)), pos.getZ());
        }
        return spawnAt(level, pos, player);
    }

    private static int spawnAt(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (findStorm(level.getServer()) != null) {
            if (player != null) {
                player.sendSystemMessage(Component.literal("A Wither Storm already exists."), false);
            }
            return 0;
        }
        WitherBoss storm = EntityType.WITHER.spawn(level, boss -> {
            boss.getPersistentData().putBoolean(MARKER, true);
            boss.getPersistentData().putBoolean(MIGRATED, true);
            boss.setCustomName(Component.literal("WITHER STORM"));
            boss.setCustomNameVisible(true);
            boss.setPersistenceRequired();
            boss.setNoAi(true);
            boss.setNoGravity(true);
            boss.setInvisible(true);
            boss.setInvulnerableTicks(0);
            boss.setHealth(boss.getMaxHealth());
        }, pos, EntitySpawnReason.COMMAND, false, false);
        if (storm == null) {
            if (player != null) {
                player.sendSystemMessage(Component.literal("The Wither Storm could not spawn here."), false);
            }
            return 0;
        }
        level.setChunkForced(storm.chunkPosition().x(), storm.chunkPosition().z(), true);
        buildBody(storm);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.5F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("The nine-headed Wither Storm has appeared. Fly into a head to reach its core."), false);
        if (player != null) {
            player.sendSystemMessage(Component.literal("Storm location: " + pos.toShortString()), false);
        }
        return 1;
    }

    public static int clear(ServerPlayer player) {
        WitherBoss storm = findStorm(player.level().getServer());
        if (storm == null) {
            return 0;
        }
        ServerLevel level = (ServerLevel) storm.level();
        cleanupParts(storm);
        level.setChunkForced(storm.chunkPosition().x(), storm.chunkPosition().z(), false);
        storm.discard();
        for (ServerPlayer visitor : level.getServer().getPlayerList().getPlayers()) {
            if (visitor.getPersistentData().getBooleanOr(INSIDE, false)) {
                leaveCore(visitor);
            }
        }
        player.sendSystemMessage(Component.literal("Wither Storm removed."), false);
        return 1;
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof WitherBoss storm && isStorm(storm)) {
            event.setCanceled(true);
        }
    }

    public static void afterEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof WitherBoss storm) || !(storm.level() instanceof ServerLevel level)
                || !isStorm(storm) || !storm.isAlive()) {
            return;
        }
        if (!storm.getPersistentData().getBooleanOr(MIGRATED, false)) {
            storm.getPersistentData().putBoolean(MIGRATED, true);
            storm.setNoAi(true);
            storm.setNoGravity(true);
            storm.setInvisible(true);
            storm.setCustomName(Component.literal("WITHER STORM"));
        }
        if (storm.getPassengers().isEmpty() && storm.getPersistentData().getIntOr(DEATH_TICKS, 0) == 0) {
            buildBody(storm);
        }
        int deathTicks = storm.getPersistentData().getIntOr(DEATH_TICKS, 0);
        if (deathTicks > 0) {
            tickDeath(level, storm, deathTicks);
            return;
        }
        if (storm.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, storm.getX(), storm.getY() + 6.0, storm.getZ(),
                    16, 11.0, 8.0, 8.0, 0.02);
        }
        if (storm.tickCount % 12 == 0) {
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    Vec3 head = headPosition(storm, col, row);
                    Vec3 ground = head.add(0.0, -Math.min(24.0, head.y - level.getMinY()), -6.0);
                    purpleBeam(level, head.add(0.0, 0.0, -3.0), ground, 1.1);
                }
            }
        }
        if (storm.tickCount % 40 == 0) {
            ServerPlayer target = nearestSurvivalPlayer(level, storm.position(), 70.0);
            if (target != null) {
                Vec3 head = headPosition(storm, storm.getRandom().nextInt(3), storm.getRandom().nextInt(3));
                purpleBeam(level, head.add(0.0, 0.0, -3.0), target.getEyePosition(), 0.5);
                target.hurtServer(level, level.damageSources().magic(), 5.0F);
            }
        }
    }

    public static void playerTick(ServerPlayer player) {
        if (player.getPersistentData().getBooleanOr(INSIDE, false)) {
            tickCorePlayer(player);
            return;
        }
        if (player.tickCount % 2 != 0) {
            return;
        }
        WitherBoss storm = findStorm(player.level().getServer());
        if (storm == null || storm.level() != player.level()
                || storm.getPersistentData().getIntOr(DEATH_TICKS, 0) > 0
                || player.isSpectator() || player.position().distanceToSqr(storm.position()) > 40.0 * 40.0) {
            return;
        }
        Vec3 movement = player.position().subtract(player.xo, player.yo, player.zo);
        if (movement.lengthSqr() < 0.0025) {
            return;
        }
        Vec3 previousEye = player.getEyePosition().subtract(movement);
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                Vec3 head = headPosition(storm, col, row);
                double progress = Math.clamp(head.subtract(previousEye).dot(movement) / movement.lengthSqr(), 0.0, 1.0);
                Vec3 closest = previousEye.add(movement.scale(progress));
                if (progress > 0.0 && closest.distanceToSqr(head) <= 13.0) {
                    enterCore(player, storm);
                    return;
                }
            }
        }
    }

    public static void coreStrike(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !event.getPos().equals(CORE)
                || !player.level().dimension().equals(ArsenalDimensionHandler.ARSENAL_DIMENSION)
                || !player.getPersistentData().getBooleanOr(INSIDE, false)) {
            return;
        }
        event.setCanceled(true);
        if (!player.getMainHandItem().is(ModItems.COMMAND_BLOCK_AXE.get())) {
            player.sendSystemMessage(Component.literal("Only the Command Block Axe can destroy the core."), true);
            return;
        }
        WitherBoss storm = findStorm(player.level().getServer());
        if (storm == null || storm.getPersistentData().getIntOr(DEATH_TICKS, 0) > 0) {
            return;
        }
        player.level().setBlockAndUpdate(CORE, Blocks.AIR.defaultBlockState());
        storm.getPersistentData().putString(CORE_HIT, player.getUUID().toString());
        storm.getPersistentData().putInt(DEATH_TICKS, 1);
        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("The command block core has shattered!"), false);
        player.level().playSound(null, CORE, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 4.0F, 0.55F);
        for (ServerPlayer visitor : player.level().getServer().getPlayerList().getPlayers()) {
            if (visitor.getPersistentData().getBooleanOr(INSIDE, false)) {
                PacketDistributor.sendToPlayer(visitor, new StormDeathPayload(90));
            }
        }
    }

    private static void enterCore(ServerPlayer player, WitherBoss storm) {
        ServerLevel coreLevel = player.level().getServer().getLevel(ArsenalDimensionHandler.ARSENAL_DIMENSION);
        if (coreLevel == null) {
            player.sendSystemMessage(Component.literal("The core chamber is unavailable."), false);
            return;
        }
        if (!coreLevel.getBlockState(CORE).is(Blocks.COMMAND_BLOCK)) {
            buildCoreRoom(coreLevel);
        }
        CompoundTag data = player.getPersistentData();
        data.putBoolean(INSIDE, true);
        data.putString(RETURN_DIMENSION, player.level().dimension().identifier().toString());
        data.putDouble(RETURN_X, storm.getX());
        data.putDouble(RETURN_Y, storm.getY() - 14.0);
        data.putDouble(RETURN_Z, storm.getZ() - 24.0);
        player.teleportTo(coreLevel, ARRIVAL.getX() + 0.5, ARRIVAL.getY(), ARRIVAL.getZ() + 0.5,
                Set.<Relative>of(), 180.0F, 0.0F, true);
        player.sendSystemMessage(Component.literal(
                "Inside the storm. The two heads guard the command block core. Use a Command Block Axe."), false);
    }

    private static void tickCorePlayer(ServerPlayer player) {
        if (!player.level().dimension().equals(ArsenalDimensionHandler.ARSENAL_DIMENSION)) {
            player.getPersistentData().remove(INSIDE);
            return;
        }
        WitherBoss storm = findStorm(player.level().getServer());
        if (storm == null) {
            leaveCore(player);
            return;
        }
        int deathTicks = storm.getPersistentData().getIntOr(DEATH_TICKS, 0);
        if (deathTicks > 0) {
            if (deathTicks >= 70) {
                leaveCore(player);
            }
            return;
        }
        if (player.tickCount % 10 == 0) {
            for (int side : new int[] {-1, 1}) {
                Vec3 mouth = new Vec3(CORE.getX() + side * 6.0, 125.0, CORE.getZ() - 2.0);
                Vec3 beamEnd = player.isCreative() || player.isSpectator()
                        ? mouth.add(0.0, -4.0, -8.0) : player.getEyePosition();
                purpleBeam(player.level(), mouth, beamEnd, 0.45);
                if (!player.isCreative() && !player.isSpectator()
                        && player.position().distanceToSqr(mouth) < 18.0 * 18.0
                        && player.tickCount % 40 == 0) {
                    player.hurtServer(player.level(), player.level().damageSources().magic(), 3.0F);
                }
            }
        }
    }

    private static void leaveCore(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        Identifier id = Identifier.tryParse(data.getStringOr(RETURN_DIMENSION, "minecraft:overworld"));
        ServerLevel destination = id == null ? null : player.level().getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, id));
        if (destination == null) {
            destination = player.level().getServer().overworld();
        }
        double x = data.getDoubleOr(RETURN_X, destination.getServer().getRespawnData().pos().getX() + 0.5);
        double y = data.getDoubleOr(RETURN_Y, destination.getServer().getRespawnData().pos().getY() + 2.0);
        double z = data.getDoubleOr(RETURN_Z, destination.getServer().getRespawnData().pos().getZ() + 0.5);
        data.remove(INSIDE);
        player.teleportTo(destination, x, y, z, Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
    }

    private static void buildCoreRoom(ServerLevel level) {
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                for (int dy = -1; dy <= 12; dy++) {
                    BlockPos pos = new BlockPos(CORE.getX() + dx, 121 + dy, CORE.getZ() + dz - 3);
                    boolean shell = dy == -1 || dy == 12 || Math.abs(dx) == 15 || Math.abs(dz) == 15;
                    level.setBlock(pos, shell ? Blocks.BLACK_CONCRETE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int side : new int[] {-1, 1}) {
            int x = CORE.getX() + side * 6;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 3; dy <= 7; dy++) {
                    for (int dz = 3; dz <= 5; dz++) {
                        level.setBlock(new BlockPos(x + dx, 121 + dy, CORE.getZ() + dz),
                                Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                    }
                }
            }
            for (int eye : new int[] {-1, 1}) {
                level.setBlock(new BlockPos(x + eye, 126, CORE.getZ() + 2),
                        Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), 2);
            }
            level.setBlock(new BlockPos(x, 124, CORE.getZ() + 2),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
            for (int segment = 0; segment < 7; segment++) {
                level.setBlock(new BlockPos(CORE.getX() + side * (11 - segment), 121 + segment / 2,
                        CORE.getZ() - 5 + segment), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
            }
        }
        level.setBlock(CORE.below(), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        level.setBlockAndUpdate(CORE, Blocks.COMMAND_BLOCK.defaultBlockState());
    }

    private static void tickDeath(ServerLevel level, WitherBoss storm, int tick) {
        storm.getPersistentData().putInt(DEATH_TICKS, tick + 1);
        if (tick == 1) {
            for (Entity passenger : List.copyOf(storm.getPassengers())) {
                int head = passenger.getPersistentData().getIntOr(HEAD, -1);
                if (head >= 3) {
                    passenger.stopRiding();
                    passenger.getPersistentData().putBoolean("ArmoredArsenalFallingHead", true);
                }
            }
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity.getPersistentData().getBooleanOr("ArmoredArsenalFallingHead", false)
                    && entity.getPersistentData().getStringOr(OWNER, "").equals(storm.getUUID().toString())) {
                entity.setPos(storm.getX(), storm.getY() - Math.min(30.0, tick * 0.32), storm.getZ());
            }
        }
        if (tick % 4 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, storm.getX(), storm.getY() + 8, storm.getZ(),
                    8, 12.0, 10.0, 10.0, 0.08);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, storm.getX(), storm.getY() + 8, storm.getZ(),
                    40, 14.0, 10.0, 12.0, 0.06);
        }
        if (tick >= 90) {
            finishDeath(level, storm);
        }
    }

    private static void finishDeath(ServerLevel level, WitherBoss storm) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getPersistentData().getBooleanOr(INSIDE, false)) {
                leaveCore(player);
            }
        }
        UUID winner = null;
        try {
            winner = UUID.fromString(storm.getPersistentData().getStringOr(CORE_HIT, ""));
        } catch (IllegalArgumentException ignored) {
            // The boss still drops its items if the winning player has left.
        }
        ServerPlayer victor = winner == null ? null : level.getServer().getPlayerList().getPlayer(winner);
        if (victor != null) {
            victor.giveExperienceLevels(10_000_000);
            victor.sendSystemMessage(Component.literal("The storm's XP exceeds Minecraft's numeric limit. Awarded ten million usable levels."), false);
        }
        for (int i = 0; i < 3; i++) {
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                    level, storm.getX() + i - 1, storm.getY() - 13.0, storm.getZ(),
                    new ItemStack(ModItems.WITHERED_WITHER_STAR.get()));
            level.addFreshEntity(item);
        }
        cleanupParts(storm);
        level.setChunkForced(storm.chunkPosition().x(), storm.chunkPosition().z(), false);
        storm.discard();
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("The Wither Storm has disintegrated. Three Withered Wither Stars remain."), false);
    }

    private static void buildBody(WitherBoss storm) {
        addPart(storm, Blocks.BLACK_CONCRETE, -11, -4, -1, 22, 17, 14, -1);
        addPart(storm, Blocks.CRYING_OBSIDIAN, -4, 2, -2, 8, 8, 1, -1);
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                int head = col * 3 + row;
                float x = (float) HEAD_X[col];
                float y = (float) HEAD_Y[row];
                addPart(storm, Blocks.BLACK_CONCRETE, x - 2.5F, y - 2.5F, -11, 5, 5, 5, head);
                addPart(storm, Blocks.PURPLE_STAINED_GLASS, x - 1.6F, y + 0.7F, -11.15F, 0.8F, 0.8F, 0.25F, head);
                addPart(storm, Blocks.PURPLE_STAINED_GLASS, x + 0.8F, y + 0.7F, -11.15F, 0.8F, 0.8F, 0.25F, head);
                addPart(storm, Blocks.CRYING_OBSIDIAN, x - 1.2F, y - 1.4F, -11.2F, 2.4F, 1.2F, 0.3F, head);
            }
        }
        for (int side : new int[] {-1, 1}) {
            for (int front : new int[] {-1, 1}) {
                for (int segment = 0; segment < 6; segment++) {
                    float x = side * (8.0F + segment * 1.4F);
                    float y = 4.0F - segment * 1.4F;
                    float z = front * (4.0F + segment * 1.1F);
                    addPart(storm, Blocks.BLACK_CONCRETE, x - 1.0F, y - 1.0F, z - 1.0F,
                            2.0F - segment * 0.2F, 2.0F - segment * 0.2F, 2.0F - segment * 0.2F, -1);
                }
            }
        }
    }

    private static void addPart(WitherBoss storm, net.minecraft.world.level.block.Block block,
                                float x, float y, float z, float sx, float sy, float sz, int head) {
        ServerLevel level = (ServerLevel) storm.level();
        CompoundTag nbt = new CompoundTag();
        CompoundTag blockState = new CompoundTag();
        blockState.putString("Name", BuiltInRegistries.BLOCK.getKey(block).toString());
        nbt.put("block_state", blockState);
        CompoundTag transform = new CompoundTag();
        transform.put("translation", vector(x, y, z));
        transform.put("scale", vector(sx, sy, sz));
        transform.put("left_rotation", quaternionIdentity());
        transform.put("right_rotation", quaternionIdentity());
        nbt.put("transformation", transform);
        nbt.putFloat("view_range", 256.0F);
        try {
            Entity display = SummonCommand.createEntity(level.getServer().createCommandSourceStack(),
                    BuiltInRegistries.ENTITY_TYPE.get(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BLOCK_DISPLAY)).orElseThrow(),
                    storm.position(), nbt, false);
            display.getPersistentData().putBoolean(PART, true);
            display.getPersistentData().putString(OWNER, storm.getUUID().toString());
            display.getPersistentData().putInt(HEAD, head);
            display.startRiding(storm, true, true);
        } catch (Exception exception) {
            LOGGER.error("Could not create Wither Storm display", exception);
        }
    }

    private static void cleanupParts(WitherBoss storm) {
        ServerLevel level = (ServerLevel) storm.level();
        for (Entity entity : List.copyOf(storm.getPassengers())) {
            entity.discard();
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity.getPersistentData().getBooleanOr(PART, false)
                    && entity.getPersistentData().getStringOr(OWNER, "").equals(storm.getUUID().toString())) {
                entity.discard();
            }
        }
    }

    private static Vec3 headPosition(WitherBoss storm, int col, int row) {
        return storm.position().add(HEAD_X[col], HEAD_Y[row], -8.5);
    }

    private static ServerPlayer nearestSurvivalPlayer(ServerLevel level, Vec3 point, double radius) {
        ServerPlayer nearest = null;
        double nearestSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = player.position().distanceToSqr(point);
            if (distance < nearestSq) {
                nearestSq = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void purpleBeam(ServerLevel level, Vec3 start, Vec3 end, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.1) {
            return;
        }
        Vec3 direction = delta.scale(1.0 / length);
        for (double step = 0; step <= Math.min(length, 65.0); step += spacing) {
            Vec3 point = start.add(direction.scale(step));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                    1, 0.12, 0.12, 0.12, 0.0);
        }
    }

    private static WitherBoss findStorm(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof WitherBoss wither && isStorm(wither) && wither.isAlive()) {
                    return wither;
                }
            }
        }
        return null;
    }

    private static boolean isStorm(Entity entity) {
        return entity.getPersistentData().getBooleanOr(MARKER, false);
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
        values.add(FloatTag.valueOf(0));
        values.add(FloatTag.valueOf(0));
        values.add(FloatTag.valueOf(0));
        values.add(FloatTag.valueOf(1));
        return values;
    }

    private WitherStormHandler() {}
}
