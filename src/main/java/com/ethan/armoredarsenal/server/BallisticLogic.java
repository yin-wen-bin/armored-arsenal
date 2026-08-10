package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.BallisticProfile;
import com.ethan.armoredarsenal.network.LaserBeamPayload;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BallisticLogic {
    public static void fireWeapon(ServerPlayer player, InteractionHand hand, BallisticProfile profile) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, profile.cooldownTicks());
        fireBurst(player.level(), player, player.blockPosition(), player.getEyePosition(), player.getLookAngle(), profile);
    }

    public static void fireTurret(
            ServerLevel level, BlockPos turretPos, Vec3 start, LivingEntity target, BallisticProfile profile) {
        fireBurst(level, null, turretPos, start, target.getEyePosition().subtract(start), profile);
    }

    private static void fireBurst(
            ServerLevel level, Entity owner, BlockPos soundPos, Vec3 start, Vec3 direction, BallisticProfile profile) {
        for (int shot = 0; shot < profile.projectiles(); shot++) {
            Vec3 shotDirection = spread(level, direction.normalize(), profile.spread());
            Vec3 end = start.add(shotDirection.scale(profile.range()));
            Optional<Hit> hit = findHit(level, owner, start, end);
            Vec3 tracerEnd = hit.map(Hit::position).orElse(end);
            showTracer(level, start, tracerEnd, profile);
            hit.ifPresent(result -> {
                if (result.entity() instanceof LivingEntity living) {
                    living.hurtServer(level, level.damageSources().generic(), profile.damage());
                }
            });
        }
        level.playSound(
                null, soundPos, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
                profile.projectiles() > 1 ? 1.2F : 0.9F, profile.soundPitch());
    }

    private static Optional<Hit> findHit(ServerLevel level, Entity owner, Vec3 start, Vec3 end) {
        HitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 clippedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB searchBox = new AABB(start, clippedEnd).inflate(1.0D);
        return level.getEntities(owner, searchBox, entity -> entity instanceof LivingEntity && entity.isPickable())
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.25D).clip(start, clippedEnd)
                        .map(position -> new Hit(entity, position)))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.position())));
    }

    private static Vec3 spread(ServerLevel level, Vec3 direction, float spread) {
        if (spread <= 0.0F) {
            return direction;
        }
        return direction.add(
                level.getRandom().triangle(0.0D, spread),
                level.getRandom().triangle(0.0D, spread),
                level.getRandom().triangle(0.0D, spread)).normalize();
    }

    private static void showTracer(ServerLevel level, Vec3 start, Vec3 end, BallisticProfile profile) {
        Vec3 middle = start.add(end).scale(0.5D);
        PacketDistributor.sendToPlayersNear(
                level, null, middle.x, middle.y, middle.z, Math.max(48.0D, start.distanceTo(end) + 12.0D),
                LaserBeamPayload.between(
                        level.getRandom().nextLong(), start, end,
                        profile.tracerColor(), profile.tracerWidth(), 2));
    }

    private record Hit(Entity entity, Vec3 position) {}

    private BallisticLogic() {}
}
