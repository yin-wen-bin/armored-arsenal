package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.WeaponProfile;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LaserLogic {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final DustParticleOptions RED_LASER = new DustParticleOptions(0xFF1F18, 1.15F);
    private static final DustParticleOptions HOT_LASER_CORE = new DustParticleOptions(0xFFD9D2, 0.45F);

    public static void fireWeapon(ServerPlayer player, WeaponProfile profile) {
        long now = player.level().getGameTime();
        Map<String, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        long readyAt = playerCooldowns.getOrDefault(profile.id(), 0L);

        if (now < readyAt) {
            player.sendSystemMessage(Component.literal("Weapon cooling down."), true);
            return;
        }

        if (profile.suitEnergyCost() > 0 && !SuitPowerHandler.consumeSuitEnergy(player, profile.suitEnergyCost())) {
            player.sendSystemMessage(Component.literal("Suit energy too low."), false);
            return;
        }

        playerCooldowns.put(profile.id(), now + profile.cooldownTicks());
        fireBeam(player, profile);
    }

    public static void fireBeam(ServerPlayer player, WeaponProfile profile) {
        ServerLevel level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(profile.range()));

        Optional<BeamHit> hit = findHit(player, start, end);
        Vec3 beamEnd = hit.map(BeamHit::position).orElse(end);

        drawBeam(level, start, beamEnd);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.85F, profile.soundPitch());

        hit.ifPresent(beamHit -> {
            Entity target = beamHit.entity();
            if (target instanceof LivingEntity living) {
                living.hurtServer(level, player.damageSources().playerAttack(player), profile.damage());
                target.igniteForSeconds(2.0F);
            }
        });
    }

    private static Optional<BeamHit> findHit(ServerPlayer player, Vec3 start, Vec3 end) {
        Vec3 movement = end.subtract(start);
        AABB searchBox = player.getBoundingBox().expandTowards(movement).inflate(1.2D);

        return player.level()
                .getEntities(player, searchBox, entity -> entity instanceof LivingEntity && entity.isPickable())
                .stream()
                .map(entity -> {
                    AABB box = entity.getBoundingBox().inflate(0.35D);
                    return box.clip(start, end).map(position -> new BeamHit(entity, position));
                })
                .flatMap(optional -> optional.stream())
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.position())));
    }

    private static void drawBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.001D) {
            return;
        }

        Vec3 step = delta.normalize().scale(0.45D);
        int particles = Math.max(1, (int) (length / 0.45D));
        Vec3 current = start;
        for (int i = 0; i <= particles; i++) {
            level.sendParticles(RED_LASER, current.x, current.y, current.z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
            if (i % 2 == 0) {
                level.sendParticles(HOT_LASER_CORE, current.x, current.y, current.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            current = current.add(step);
        }
    }

    private record BeamHit(Entity entity, Vec3 position) {}

    private LaserLogic() {}
}
