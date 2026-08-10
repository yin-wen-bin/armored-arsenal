package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.WeaponProfile;
import com.ethan.armoredarsenal.network.LaserBeamPayload;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LaserLogic {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

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

        showBeam(level, start, beamEnd, profile.beamWidth());
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.85F, profile.soundPitch());

        hit.ifPresent(beamHit -> {
            Entity target = beamHit.entity();
            if (target instanceof LivingEntity living) {
                living.hurtServer(level, player.damageSources().playerAttack(player), profile.damage());
                target.igniteForSeconds(2.0F);
            }
        });
    }

    public static void fireTurretBeam(
            ServerLevel level, net.minecraft.core.BlockPos turretPos, Vec3 start,
            LivingEntity target, WeaponProfile profile) {
        Vec3 end = target.getEyePosition();
        showBeam(level, start, end, profile.beamWidth());
        level.playSound(null, turretPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, profile.soundPitch());
        if (target.hurtServer(level, level.damageSources().magic(), profile.damage())) {
            target.igniteForSeconds(1.0F);
        }
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

    private static void showBeam(ServerLevel level, Vec3 start, Vec3 end, float width) {
        Vec3 middle = start.add(end).scale(0.5D);
        double radius = Math.max(48.0D, start.distanceTo(end) + 16.0D);
        PacketDistributor.sendToPlayersNear(
                level, null, middle.x, middle.y, middle.z, radius,
                LaserBeamPayload.between(
                        level.getRandom().nextLong(), start, end, 0xEFFF241C, width, 5));
    }

    private record BeamHit(Entity entity, Vec3 position) {}

    private LaserLogic() {}
}
