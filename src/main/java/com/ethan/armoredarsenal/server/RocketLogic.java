package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.RocketItem;
import com.ethan.armoredarsenal.content.RocketProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RocketLogic {
    public static void launchFromPlayer(ServerPlayer player, InteractionHand hand, RocketProfile profile) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();
        Snowball rocket = new Snowball(player.level(), player, new ItemStack(stack.getItem()));
        rocket.setPos(player.getEyePosition().add(direction.scale(0.7D)));
        prepareRocket(rocket, direction, profile.speed());
        player.level().addFreshEntity(rocket);
        player.level().playSound(
                null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        player.getCooldowns().addCooldown(stack, profile.cooldownTicks());
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }

    public static void launchFromTurret(
            ServerLevel level, BlockPos turretPos, Vec3 start, LivingEntity target, RocketProfile profile, Item rocketItem) {
        Vec3 direction = target.getEyePosition().subtract(start).normalize();
        Snowball rocket = new Snowball(level, start.x, start.y, start.z, new ItemStack(rocketItem));
        prepareRocket(rocket, direction, profile.speed());
        level.addFreshEntity(rocket);
        level.playSound(
                null, turretPos, SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS, 0.9F, 0.72F);
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball rocket)
                || !(rocket.getItem().getItem() instanceof RocketItem rocketItem)
                || !(rocket.level() instanceof ServerLevel level)) {
            return;
        }

        RocketProfile profile = rocketItem.profile();
        level.explode(
                rocket.getOwner(), rocket.getX(), rocket.getY(), rocket.getZ(),
                profile.explosionPower(), Level.ExplosionInteraction.TNT);
        rocket.discard();
        event.setCanceled(true);
    }

    public static void beforeEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Snowball rocket)
                || !(rocket.getItem().getItem() instanceof RocketItem)
                || !(rocket.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(
                ParticleTypes.FLAME, rocket.getX(), rocket.getY(), rocket.getZ(),
                2, 0.04D, 0.04D, 0.04D, 0.01D);
        level.sendParticles(
                ParticleTypes.SMOKE, rocket.getX(), rocket.getY(), rocket.getZ(),
                1, 0.05D, 0.05D, 0.05D, 0.01D);
        if (rocket.tickCount > 160) {
            rocket.discard();
        }
    }

    private static void prepareRocket(Snowball rocket, Vec3 direction, float speed) {
        rocket.setNoGravity(true);
        rocket.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
    }

    private RocketLogic() {}
}
