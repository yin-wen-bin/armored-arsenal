package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class WitherStormHandler {
    private static final String STORM_MARKER = "ArmoredArsenalWitherStorm";
    private static final String PHASE_KEY = "ArmoredArsenalWitherStormPhase";
    private static final double MAX_HEALTH = 1200.0D;

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("witherstorm")
                .executes(context -> spawn(context.getSource().getPlayerOrException()))
                .then(Commands.literal("clear")
                        .executes(context -> clear(context.getSource().getPlayerOrException()))));
    }

    public static int spawn(ServerPlayer player) {
        ServerLevel level = player.level();
        WitherBoss existing = findStorm(level);
        if (existing != null) {
            player.sendSystemMessage(Component.literal("A Wither Storm already exists at "
                    + existing.blockPosition().toShortString() + "."), false);
            return 0;
        }

        Vec3 look = player.getLookAngle();
        int spawnY = Math.min(level.getMaxY() - 12,
                Math.max(level.getMinY() + 12, player.blockPosition().getY() + 12));
        BlockPos spawnPos = BlockPos.containing(
                player.getX() + look.x * 24.0D,
                spawnY,
                player.getZ() + look.z * 24.0D);
        WitherBoss storm = EntityType.WITHER.spawn(
                level,
                spawned -> configure(spawned, player),
                spawnPos,
                EntitySpawnReason.COMMAND,
                false,
                false);
        if (storm == null) {
            player.sendSystemMessage(Component.literal("The Wither Storm could not spawn here."), false);
            return 0;
        }

        level.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.55F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("The sky tears open. A Wither Storm has awakened!"), false);
        player.sendSystemMessage(Component.literal(
                "Use /witherstorm clear if the battle becomes too destructive."), false);
        return 1;
    }

    public static int clear(ServerPlayer player) {
        int removed = 0;
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (isStorm(entity)) {
                    entity.discard();
                    removed++;
                }
            }
        }
        player.sendSystemMessage(Component.literal("Removed " + removed + " Wither Storm boss"
                + (removed == 1 ? "." : "es.")), false);
        return removed;
    }

    public static void afterEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherBoss storm)
                || !(storm.level() instanceof ServerLevel level)
                || !isStorm(storm)
                || !storm.isAlive()) {
            return;
        }

        int phase = phase(storm);
        int previousPhase = storm.getPersistentData().getIntOr(PHASE_KEY, 1);
        if (phase != previousPhase) {
            storm.getPersistentData().putInt(PHASE_KEY, phase);
            announcePhase(level, storm, phase);
        }

        if (storm.tickCount % 4 == 0) {
            createStormCloud(level, storm, phase);
        }
        pullNearbyPlayers(level, storm, phase);

        int minionDelay = switch (phase) {
            case 3 -> 100;
            case 2 -> 150;
            default -> 220;
        };
        if (storm.tickCount % minionDelay == 0) {
            summonMinion(level, storm, phase);
        }
        if (phase >= 2 && storm.tickCount % 40 == 0 && storm.getHealth() < storm.getMaxHealth()) {
            storm.heal(phase == 3 ? 3.0F : 1.5F);
        }
    }

    private static void configure(WitherBoss storm, ServerPlayer target) {
        storm.getPersistentData().putBoolean(STORM_MARKER, true);
        storm.getPersistentData().putInt(PHASE_KEY, 1);
        storm.setCustomName(Component.literal("Wither Storm - Phase 1"));
        storm.setCustomNameVisible(true);
        storm.setPersistenceRequired();
        storm.setInvulnerableTicks(0);
        setAttribute(storm, Attributes.MAX_HEALTH, MAX_HEALTH);
        setAttribute(storm, Attributes.ARMOR, 18.0D);
        setAttribute(storm, Attributes.ARMOR_TOUGHNESS, 12.0D);
        setAttribute(storm, Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        setAttribute(storm, Attributes.FOLLOW_RANGE, 128.0D);
        setAttribute(storm, Attributes.SCALE, 3.0D);
        storm.setHealth((float)MAX_HEALTH);
        storm.setTarget(target);
    }

    private static void setAttribute(WitherBoss storm,
                                     net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                     double value) {
        AttributeInstance instance = storm.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static int phase(WitherBoss storm) {
        float healthRatio = storm.getHealth() / storm.getMaxHealth();
        if (healthRatio <= 0.33F) {
            return 3;
        }
        if (healthRatio <= 0.66F) {
            return 2;
        }
        return 1;
    }

    private static void announcePhase(ServerLevel level, WitherBoss storm, int phase) {
        storm.setCustomName(Component.literal("Wither Storm - Phase " + phase));
        String message = phase == 3
                ? "The Wither Storm enters its final phase. Its gravity well is at maximum strength!"
                : "The Wither Storm evolves into Phase 2 and begins regenerating!";
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        level.playSound(null, storm.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 4.0F,
                phase == 3 ? 0.45F : 0.65F);
        level.sendParticles(ParticleTypes.EXPLOSION, storm.getX(), storm.getY() + 2.0D, storm.getZ(),
                phase * 12, 3.0D, 2.0D, 3.0D, 0.05D);
    }

    private static void createStormCloud(ServerLevel level, WitherBoss storm, int phase) {
        double spread = 2.5D + phase * 1.5D;
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                storm.getX(), storm.getY() + 2.0D, storm.getZ(),
                6 + phase * 3, spread, 1.5D, spread, 0.02D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                storm.getX(), storm.getY() + 1.5D, storm.getZ(),
                3 + phase * 2, spread, 2.0D, spread, 0.03D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                storm.getX(), storm.getY() + 1.0D, storm.getZ(),
                phase, spread * 0.5D, 1.0D, spread * 0.5D, 0.01D);
    }

    private static void pullNearbyPlayers(ServerLevel level, WitherBoss storm, int phase) {
        double radius = switch (phase) {
            case 3 -> 36.0D;
            case 2 -> 27.0D;
            default -> 18.0D;
        };
        AABB area = storm.getBoundingBox().inflate(radius);
        List<ServerPlayer> players = level.getEntitiesOfClass(
                ServerPlayer.class, area, player -> !player.isSpectator() && player.isAlive());
        double strength = 0.006D + phase * 0.005D;
        for (ServerPlayer player : players) {
            Vec3 pull = storm.position().add(0.0D, 2.0D, 0.0D).subtract(player.position());
            if (pull.lengthSqr() < 4.0D) {
                continue;
            }
            player.setDeltaMovement(player.getDeltaMovement().add(pull.normalize().scale(strength)));
            player.hurtMarked = true;
        }
    }

    private static void summonMinion(ServerLevel level, WitherBoss storm, int phase) {
        AABB minionArea = storm.getBoundingBox().inflate(42.0D);
        if (level.getEntitiesOfClass(WitherSkeleton.class, minionArea).size() >= 3 + phase) {
            return;
        }

        int offsetX = storm.getRandom().nextInt(13) - 6;
        int offsetZ = storm.getRandom().nextInt(13) - 6;
        BlockPos pos = BlockPos.containing(storm.getX() + offsetX, storm.getY() - 2.0D, storm.getZ() + offsetZ);
        WitherSkeleton minion = EntityType.WITHER_SKELETON.spawn(level, pos, EntitySpawnReason.REINFORCEMENT);
        if (minion != null) {
            minion.setCustomName(Component.literal("Storm Spawn"));
            minion.setPersistenceRequired();
            var target = level.getNearestPlayer(storm, 96.0D);
            if (target != null) {
                minion.setTarget(target);
            }
        }
    }

    private static WitherBoss findStorm(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof WitherBoss wither && isStorm(wither) && wither.isAlive()) {
                return wither;
            }
        }
        return null;
    }

    private static boolean isStorm(Entity entity) {
        return entity.getPersistentData().getBooleanOr(STORM_MARKER, false);
    }

    private WitherStormHandler() {}
}
