package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.StormShape;
import com.ethan.armoredarsenal.content.StormShape.HeadSpec;
import com.ethan.armoredarsenal.network.LaserBeamPayload;
import com.ethan.armoredarsenal.network.StormDeathPayload;
import com.ethan.armoredarsenal.network.StormVisualPayload;
import com.ethan.armoredarsenal.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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

public final class WitherStormHandler {
    private static final String MARKER = "ArmoredArsenalWitherStorm";
    private static final String PART = "ArmoredArsenalStormPart";
    private static final String OWNER = "ArmoredArsenalStormOwner";
    private static final String SMOOTH = "ArmoredArsenalStormSmoothModel";
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
    private static final BlockPos CORE_ROOM_MARKER = CORE.offset(15, 5, 0);
    private static final Map<UUID, Vec3> LAST_EYE = new ConcurrentHashMap<>();

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
        storm.getPersistentData().putBoolean(SMOOTH, true);
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
        if (!storm.getPersistentData().getBooleanOr(SMOOTH, false)) {
            cleanupParts(storm);
            storm.getPersistentData().putBoolean(SMOOTH, true);
        }
        int deathTicks = storm.getPersistentData().getIntOr(DEATH_TICKS, 0);
        if (storm.tickCount % (deathTicks > 0 ? 2 : 5) == 0) {
            StormVisualPayload visual = StormVisualPayload.at(level.dimension().identifier().toString(),
                    storm.position(), deathTicks, false);
            for (ServerPlayer player : level.players()) {
                if (player.position().distanceToSqr(storm.position()) < 350.0 * 350.0) {
                    PacketDistributor.sendToPlayer(player, visual);
                }
            }
        }
        if (deathTicks > 0) {
            tickDeath(level, storm, deathTicks);
            return;
        }
        if (storm.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, storm.getX(), storm.getY() + 6.0, storm.getZ(),
                    16, 11.0, 8.0, 8.0, 0.02);
        }
        if (storm.tickCount % 40 == 0) {
            ServerPlayer target = nearestSurvivalPlayer(level, storm.position(), 70.0);
            if (target != null) {
                Vec3 head = headPosition(storm, StormShape.HEADS[storm.getRandom().nextBoolean() ? 4 : 7]);
                LaserBeamPayload beam = LaserBeamPayload.between(storm.tickCount, head.add(0, -1, -3),
                        target.getEyePosition(), 0xC9B049FF, 1.5F, 7);
                for (ServerPlayer watcher : level.players()) {
                    if (watcher.position().distanceToSqr(storm.position()) < 200.0 * 200.0) {
                        PacketDistributor.sendToPlayer(watcher, beam);
                    }
                }
                target.hurtServer(level, level.damageSources().magic(), 5.0F);
            }
        }
    }

    public static void playerTick(ServerPlayer player) {
        if (player.getPersistentData().getBooleanOr(INSIDE, false)) {
            LAST_EYE.remove(player.getUUID());
            tickCorePlayer(player);
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 previousEye = LAST_EYE.put(player.getUUID(), eye);
        WitherBoss storm = findStorm(player.level().getServer());
        if (storm == null || storm.level() != player.level()
                || storm.getPersistentData().getIntOr(DEATH_TICKS, 0) > 0
                || player.isSpectator() || player.position().distanceToSqr(storm.position()) > 48.0 * 48.0) {
            return;
        }
        for (HeadSpec spec : StormShape.HEADS) {
            Vec3 head = headPosition(storm, spec).add(0.0, 0.0, -1.5);
            if (eye.distanceToSqr(head) <= 36.0
                    || previousEye != null && previousEye.distanceToSqr(eye) < 40.0 * 40.0
                    && segmentDistanceSqr(previousEye, eye, head) <= 36.0) {
                LAST_EYE.remove(player.getUUID());
                enterCore(player, storm);
                return;
            }
        }
    }

    private static double segmentDistanceSqr(Vec3 start, Vec3 end, Vec3 point) {
        Vec3 movement = end.subtract(start);
        double lengthSq = movement.lengthSqr();
        if (lengthSq < 0.0001) {
            return point.distanceToSqr(end);
        }
        double progress = Math.clamp(point.subtract(start).dot(movement) / lengthSq, 0.0, 1.0);
        return point.distanceToSqr(start.add(movement.scale(progress)));
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
        if (!coreLevel.getBlockState(CORE).is(Blocks.COMMAND_BLOCK)
                || !coreLevel.getBlockState(CORE_ROOM_MARKER).is(Blocks.REINFORCED_DEEPSLATE)) {
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
        if (deathTicks == 0 && !player.level().getBlockState(CORE_ROOM_MARKER).is(Blocks.REINFORCED_DEEPSLATE)) {
            buildCoreRoom(player.level());
        }
        if (player.tickCount % 5 == 0) {
            PacketDistributor.sendToPlayer(player, StormVisualPayload.at(
                    player.level().dimension().identifier().toString(),
                    new Vec3(CORE.getX() + 0.5, CORE.getY() + 0.5, CORE.getZ() + 0.5),
                    deathTicks, true));
        }
        if (deathTicks > 0) {
            if (deathTicks >= 70) {
                leaveCore(player);
            }
            return;
        }
        if (player.tickCount % 5 == 0) {
            for (int side : new int[] {-1, 1}) {
                Vec3 mouth = new Vec3(CORE.getX() + 0.5 + side * 6.0, 126.0, CORE.getZ() + 0.5);
                Vec3 beamEnd = player.isCreative() || player.isSpectator()
                        ? mouth.add(0.0, -4.0, -8.0) : player.getEyePosition();
                PacketDistributor.sendToPlayer(player, LaserBeamPayload.between(
                        side < 0 ? -101 : -102, mouth, beamEnd, 0xB29B45F5, 1.0F, 8));
                if (!player.isCreative() && !player.isSpectator()
                        && player.position().distanceToSqr(mouth) < 18.0 * 18.0
                        && player.tickCount % 40 == 0) {
                    player.hurtServer(player.level(), player.level().damageSources().magic(), 3.0F);
                }
            }
        }
        if (!player.isCreative() && !player.isSpectator() && player.tickCount % 30 == 0) {
            Vec3 point = player.position();
            for (int side : new int[] {-1, 1}) {
                Vec3 guard = new Vec3(CORE.getX() + 0.5 + side * 3.5, 122.0, CORE.getZ() + 0.5);
                if (point.distanceToSqr(guard) < 3.0 * 3.0) {
                    player.hurtServer(player.level(), player.level().damageSources().magic(), 2.0F);
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
            for (int front : new int[] {-1, 1}) {
                BlockPos base = CORE.offset(side * 11, 0, front * 7);
                level.setBlock(base, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
                level.setBlock(base.above(), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
            }
        }
        level.setBlock(CORE.below(), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        level.setBlockAndUpdate(CORE, Blocks.COMMAND_BLOCK.defaultBlockState());
        level.setBlock(CORE_ROOM_MARKER, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 2);
    }

    private static void tickDeath(ServerLevel level, WitherBoss storm, int tick) {
        storm.getPersistentData().putInt(DEATH_TICKS, tick + 1);
        if (tick == 1 || tick == 45) {
            level.playSound(null, storm.blockPosition(), SoundEvents.WITHER_DEATH,
                    SoundSource.HOSTILE, tick == 1 ? 4.0F : 2.0F, tick == 1 ? 0.65F : 0.9F);
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

    private static void cleanupParts(WitherBoss storm) {
        ServerLevel level = (ServerLevel) storm.level();
        for (Entity entity : List.copyOf(storm.getPassengers())) {
            entity.discard();
        }
        List<Entity> staleParts = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity != null && entity.getPersistentData().getBooleanOr(PART, false)
                    && entity.getPersistentData().getStringOr(OWNER, "").equals(storm.getUUID().toString())) {
                staleParts.add(entity);
            }
        }
        staleParts.forEach(Entity::discard);
    }

    private static Vec3 headPosition(WitherBoss storm, HeadSpec spec) {
        return storm.position().add(spec.x(), spec.y(), spec.z());
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

    private WitherStormHandler() {}
}
