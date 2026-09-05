package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.network.TransformationPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TransformationHandler {
    private static final String DATA_KEY = "ArmoredArsenalTransformation";
    private static final Map<UUID, Long> POWER_COOLDOWNS = new HashMap<>();
    private static final Map<EntityType<?>, TransformationTraits> TRAIT_CACHE = new HashMap<>();
    private static final SuggestionProvider<CommandSourceStack> MOB_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString), builder);

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("transform")
                .executes(context -> showCurrent(context.getSource().getPlayerOrException()))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource().getPlayerOrException())))
                .then(Commands.argument("mob", StringArgumentType.word())
                        .suggests(MOB_SUGGESTIONS)
                        .executes(context -> transform(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "mob")))));
        dispatcher.register(Commands.literal("power")
                .executes(context -> usePower(context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("sonicboom")
                .executes(context -> useSonicBoomAlias(context.getSource().getPlayerOrException())));
    }

    private static int usePower(ServerPlayer player) {
        String selected = player.getPersistentData().getString(DATA_KEY).orElse("");
        if (selected.isBlank()) {
            player.sendSystemMessage(Component.literal("Transform into a mob first with /transform <mob>."), true);
            return 0;
        }

        EntityType<?> type = findType(selected);
        if (type == null) {
            clearSilently(player);
            player.sendSystemMessage(Component.literal("That transformation no longer exists."), true);
            return 0;
        }

        long now = player.level().getGameTime();
        long readyAt = POWER_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (now < readyAt) {
            long tenths = ((readyAt - now) * 10L + 19L) / 20L;
            player.sendSystemMessage(Component.literal(
                    "Mob power is recharging: " + (tenths / 10L) + "." + (tenths % 10L) + "s"), true);
            return 0;
        }

        String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        PowerUse used = activatePower(player, type, path);
        if (used == null) return 0;

        POWER_COOLDOWNS.put(player.getUUID(), now + used.cooldownTicks());
        player.sendSystemMessage(Component.literal(used.name() + " activated."), true);
        return 1;
    }

    private static int useSonicBoomAlias(ServerPlayer player) {
        String selected = player.getPersistentData().getString(DATA_KEY).orElse("");
        if (!selected.endsWith(":warden")) {
            player.sendSystemMessage(Component.literal("/sonicboom is the Warden shortcut. Use /power for your current mob."), true);
            return 0;
        }
        return usePower(player);
    }

    private static PowerUse activatePower(ServerPlayer player, EntityType<?> type, String path) {
        return switch (path) {
            case "warden" -> useSonicBoomPower(player)
                    ? new PowerUse("Sonic Boom", 40) : null;
            case "blaze" -> blazeVolley(player);
            case "ghast" -> ghastFireball(player);
            case "wither" -> witherSkull(player);
            case "ender_dragon" -> dragonBreath(player);
            case "breeze" -> windCharge(player);
            case "shulker" -> shulkerBullet(player);
            case "evoker" -> evokerFangs(player);
            case "guardian", "elder_guardian" -> guardianBeam(player, path.equals("elder_guardian"));
            case "creeper" -> creeperBlast(player);
            case "skeleton", "stray", "bogged", "parched", "illusioner", "pillager" ->
                    skeletonArrow(player, path);
            case "drowned" -> drownedTrident(player);
            case "snow_golem" -> snowGolemVolley(player);
            case "llama", "trader_llama" -> llamaSpit(player);
            case "witch" -> witchBrew(player);
            case "enderman" -> endermanTeleport(player);
            case "spider", "cave_spider" -> spiderSnare(player, path.equals("cave_spider"));
            case "slime", "magma_cube" -> slimeSlam(player, path.equals("magma_cube"));
            case "iron_golem", "giant", "ravager" -> heavyQuake(player, path);
            case "copper_golem" -> copperPulse(player);
            case "goat", "hoglin", "zoglin" -> ramCharge(player, path);
            case "bee", "pufferfish" -> poisonPulse(player, path);
            case "squid", "glow_squid" -> inkCloud(player, path.equals("glow_squid"));
            case "frog" -> frogTongue(player);
            case "vex", "phantom" -> aerialDash(player, "Phantom Dive", 32);
            case "allay" -> allayCharm(player);
            case "bat", "parrot", "happy_ghast" -> aerialDash(player, "Aerial Gust", 36);
            case "axolotl", "cod", "dolphin", "nautilus", "salmon", "tadpole", "tropical_fish" ->
                    aquaticSurge(player, path);
            case "turtle", "armadillo" -> shellGuard(player, path);
            case "chicken", "rabbit" -> leap(player, path);
            case "cat", "ocelot", "fox", "wolf" -> pounce(player, path);
            case "camel", "camel_husk", "donkey", "horse", "mule", "pig", "skeleton_horse", "zombie_horse" ->
                    mountCharge(player, path);
            case "cow", "mooshroom" -> nourishingMeal(player, path);
            case "sheep" -> woolGuard(player);
            case "panda", "polar_bear" -> heavySwipe(player, path);
            case "sniffer" -> scentSearch(player);
            case "villager", "wandering_trader" -> villagerBlessing(player);
            case "piglin" -> goldRush(player);
            case "piglin_brute", "vindicator", "wither_skeleton" -> brutalStrike(player, path);
            case "zombie", "husk" -> undeadSurge(player, path);
            case "creaking" -> creakingRoot(player);
            case "endermite", "silverfish" -> infestation(player);
            case "strider" -> striderSprint(player);
            default -> fallbackPower(player, type);
        };
    }

    private static PowerUse blazeVolley(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        if (side.lengthSqr() > 0.001) side = side.normalize();
        for (int i = -1; i <= 1; i++) {
            Vec3 direction = look.add(side.scale(i * 0.09)).normalize();
            SmallFireball fireball = new SmallFireball(player.level(), player, direction);
            placeAtMuzzle(player, fireball, direction);
            player.level().addFreshEntity(fireball);
        }
        player.playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 1.0F);
        return new PowerUse("Blaze Fireball Volley", 30);
    }

    private static PowerUse ghastFireball(ServerPlayer player) {
        Vec3 direction = player.getLookAngle().normalize();
        LargeFireball fireball = new LargeFireball(player.level(), player, direction, 2);
        placeAtMuzzle(player, fireball, direction);
        player.level().addFreshEntity(fireball);
        player.playSound(SoundEvents.GHAST_SHOOT, 2.0F, 1.0F);
        return new PowerUse("Ghast Fireball", 45);
    }

    private static PowerUse witherSkull(ServerPlayer player) {
        Vec3 direction = player.getLookAngle().normalize();
        WitherSkull skull = new WitherSkull(player.level(), player, direction);
        skull.setDangerous(player.getRandom().nextInt(4) == 0);
        placeAtMuzzle(player, skull, direction);
        player.level().addFreshEntity(skull);
        player.playSound(SoundEvents.WITHER_SHOOT, 2.0F, 1.0F);
        return new PowerUse("Wither Skull", 28);
    }

    private static PowerUse dragonBreath(ServerPlayer player) {
        Vec3 direction = player.getLookAngle().normalize();
        DragonFireball fireball = new DragonFireball(player.level(), player, direction);
        placeAtMuzzle(player, fireball, direction);
        player.level().addFreshEntity(fireball);
        player.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 0.9F);
        return new PowerUse("Dragon Breath", 50);
    }

    private static PowerUse windCharge(ServerPlayer player) {
        WindCharge charge = new WindCharge(player, player.level(), player.getX(), player.getEyeY(), player.getZ());
        charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.8F, 0.2F);
        player.level().addFreshEntity(charge);
        player.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
        return new PowerUse("Wind Charge", 24);
    }

    private static PowerUse shulkerBullet(ServerPlayer player) {
        Optional<LivingEntity> target = requireTarget(player, 28.0);
        if (target.isEmpty()) return null;
        ShulkerBullet bullet = new ShulkerBullet(player.level(), player, target.get(), Direction.Axis.Y);
        player.level().addFreshEntity(bullet);
        player.playSound(SoundEvents.SHULKER_SHOOT, 1.5F, 1.0F);
        return new PowerUse("Homing Shulker Bullet", 38);
    }

    private static PowerUse evokerFangs(ServerPlayer player) {
        Vec3 horizontal = horizontalLook(player);
        for (int i = 1; i <= 9; i++) {
            Vec3 point = player.position().add(horizontal.scale(i * 1.35));
            double y = findGroundY(player.level(), point.x, player.getY(), point.z);
            player.level().addFreshEntity(new EvokerFangs(
                    player.level(), point.x, y, point.z, (float) Math.atan2(horizontal.z, horizontal.x), i, player));
        }
        player.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 1.0F);
        return new PowerUse("Evoker Fang Line", 50);
    }

    private static PowerUse guardianBeam(ServerPlayer player, boolean elder) {
        Optional<LivingEntity> target = requireTarget(player, elder ? 30.0 : 24.0);
        if (target.isEmpty()) return null;
        LivingEntity victim = target.get();
        drawBeam(player.level(), ParticleTypes.ELECTRIC_SPARK, player.getEyePosition(), victim.getEyePosition(), 0.4);
        victim.hurtServer(player.level(), player.damageSources().magic(), elder ? 14.0F : 9.0F);
        if (elder) victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 240, 1), player);
        player.playSound(SoundEvents.GUARDIAN_ATTACK, 1.8F, elder ? 0.75F : 1.0F);
        return new PowerUse(elder ? "Elder Guardian Beam" : "Guardian Beam", elder ? 55 : 40);
    }

    private static PowerUse creeperBlast(ServerPlayer player) {
        Vec3 blast = player.getEyePosition().add(player.getLookAngle().normalize().scale(4.0));
        boolean wasInvulnerable = player.isInvulnerable();
        player.setInvulnerable(true);
        try {
            player.level().explode(player, blast.x, blast.y, blast.z, 3.0F, false, Level.ExplosionInteraction.NONE);
        } finally {
            player.setInvulnerable(wasInvulnerable);
        }
        return new PowerUse("Creeper Blast", 80);
    }

    private static PowerUse skeletonArrow(ServerPlayer player, String path) {
        Arrow arrow = new Arrow(
                player.level(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(path.equals("pillager") ? 5.0 : 4.0);
        if (path.equals("stray")) arrow.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 1));
        if (path.equals("bogged")) arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        if (path.equals("parched")) arrow.igniteForSeconds(8.0F);
        if (path.equals("illusioner")) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0));
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.2F, 0.4F);
        player.level().addFreshEntity(arrow);
        player.playSound(SoundEvents.SKELETON_SHOOT, 1.2F, 1.0F);
        return new PowerUse(displayName("minecraft:" + path) + " Arrow", 24);
    }

    private static PowerUse drownedTrident(ServerPlayer player) {
        ThrownTrident trident = new ThrownTrident(player.level(), player, new ItemStack(Items.TRIDENT));
        trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.7F, 0.5F);
        player.level().addFreshEntity(trident);
        player.playSound(SoundEvents.DROWNED_SHOOT, 1.2F, 1.0F);
        return new PowerUse("Drowned Trident", 34);
    }

    private static PowerUse snowGolemVolley(ServerPlayer player) {
        for (int i = -1; i <= 1; i++) {
            Snowball snowball = new Snowball(player.level(), player, new ItemStack(Items.SNOWBALL));
            snowball.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 3.0F, 0.0F, 1.8F, 1.0F);
            player.level().addFreshEntity(snowball);
        }
        player.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.2F, 1.0F);
        return new PowerUse("Snowball Volley", 20);
    }

    private static PowerUse llamaSpit(ServerPlayer player) {
        Optional<LivingEntity> target = requireTarget(player, 18.0);
        if (target.isEmpty()) return null;
        LivingEntity victim = target.get();
        drawBeam(player.level(), ParticleTypes.SPIT, player.getEyePosition(), victim.getEyePosition(), 0.7);
        victim.hurtServer(player.level(), player.damageSources().playerAttack(player), 3.0F);
        Vec3 push = victim.position().subtract(player.position()).normalize();
        victim.push(push.x * 0.7, 0.2, push.z * 0.7);
        player.playSound(SoundEvents.LLAMA_SPIT, 1.2F, 1.0F);
        return new PowerUse("Llama Spit", 22);
    }

    private static PowerUse witchBrew(ServerPlayer player) {
        Optional<LivingEntity> target = requireTarget(player, 18.0);
        if (target.isEmpty()) return null;
        LivingEntity victim = target.get();
        drawBeam(player.level(), ParticleTypes.WITCH, player.getEyePosition(), victim.getEyePosition(), 0.6);
        victim.hurtServer(player.level(), player.damageSources().magic(), 4.0F);
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1), player);
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1), player);
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0), player);
        player.playSound(SoundEvents.WITCH_THROW, 1.2F, 1.0F);
        return new PowerUse("Witch Hex", 45);
    }

    private static PowerUse endermanTeleport(ServerPlayer player) {
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle().normalize();
        for (int distance = 16; distance >= 3; distance--) {
            Vec3 destination = start.add(look.scale(distance));
            if (player.randomTeleport(destination.x, destination.y, destination.z, true)) {
                player.level().sendParticles(ParticleTypes.PORTAL, start.x, start.y + 1.0, start.z,
                        40, 0.4, 0.9, 0.4, 0.15);
                player.level().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0,
                        player.getZ(), 40, 0.4, 0.9, 0.4, 0.15);
                player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5F, 1.0F);
                return new PowerUse("Enderman Teleport", 30);
            }
        }
        player.sendSystemMessage(Component.literal("No safe teleport destination is in front of you."), true);
        return null;
    }

    private static PowerUse spiderSnare(ServerPlayer player, boolean poisonous) {
        Optional<LivingEntity> target = requireTarget(player, 16.0);
        if (target.isEmpty()) return null;
        LivingEntity victim = target.get();
        drawBeam(player.level(), ParticleTypes.POOF, player.getEyePosition(), victim.getEyePosition(), 0.45);
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 4), player);
        victim.addEffect(new MobEffectInstance(MobEffects.WEAVING, 180, 0), player);
        if (poisonous) victim.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1), player);
        return new PowerUse(poisonous ? "Venom Web" : "Spider Web", 42);
    }

    private static PowerUse slimeSlam(ServerPlayer player, boolean magma) {
        player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.8, 0.0));
        damageNearby(player, 4.0, magma ? 8.0F : 6.0F, 1.2, 0.45);
        particleBurst(player, magma ? ParticleTypes.FLAME : ParticleTypes.ITEM_SLIME, 45, 1.8);
        if (magma) {
            for (LivingEntity target : nearbyTargets(player, 4.0)) target.igniteForTicks(100);
        }
        player.playSound(SoundEvents.SLIME_JUMP, 1.5F, magma ? 0.7F : 0.9F);
        return new PowerUse(magma ? "Magma Slam" : "Slime Bounce", 42);
    }

    private static PowerUse heavyQuake(ServerPlayer player, String path) {
        float damage = path.equals("giant") ? 16.0F : path.equals("ravager") ? 12.0F : 10.0F;
        damageNearby(player, path.equals("giant") ? 7.0 : 5.0, damage, 2.0, 0.7);
        particleBurst(player, ParticleTypes.EXPLOSION, 25, 2.5);
        player.playSound(path.equals("ravager") ? SoundEvents.RAVAGER_ROAR : SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.8F);
        return new PowerUse(path.equals("giant") ? "Giant Quake" : path.equals("ravager") ? "Ravager Roar" : "Golem Ground Slam", 55);
    }

    private static PowerUse copperPulse(ServerPlayer player) {
        for (LivingEntity target : nearbyTargets(player, 6.0)) {
            target.hurtServer(player.level(), player.damageSources().lightningBolt(), 7.0F);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0), player);
        }
        particleBurst(player, ParticleTypes.ELECTRIC_SPARK, 80, 3.0);
        player.playSound(SoundEvents.COPPER_GOLEM_SPIN, 1.5F, 1.0F);
        return new PowerUse("Copper Shock Pulse", 48);
    }

    private static PowerUse ramCharge(ServerPlayer player, String path) {
        Vec3 look = horizontalLook(player);
        player.setDeltaMovement(player.getDeltaMovement().add(look.scale(0.65)).add(0.0, 0.1, 0.0));
        Optional<LivingEntity> target = findLookTarget(player, 7.0);
        if (target.isPresent()) {
            LivingEntity victim = target.get();
            victim.hurtServer(player.level(), player.damageSources().playerAttack(player), path.equals("goat") ? 8.0F : 11.0F);
            victim.push(look.x * 2.2, 0.65, look.z * 2.2);
        }
        player.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.5F, path.equals("goat") ? 1.0F : 0.75F);
        return new PowerUse(path.equals("goat") ? "Goat Ram" : "Hoglin Charge", 38);
    }

    private static PowerUse poisonPulse(ServerPlayer player, String path) {
        boolean bee = path.equals("bee");
        for (LivingEntity target : nearbyTargets(player, bee ? 4.0 : 5.0)) {
            target.hurtServer(player.level(), player.damageSources().sting(player), bee ? 5.0F : 3.0F);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, bee ? 120 : 180, bee ? 0 : 1), player);
        }
        particleBurst(player, bee ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.BUBBLE, 50, 2.2);
        player.playSound(SoundEvents.BEE_STING, 1.3F, bee ? 1.0F : 0.7F);
        return new PowerUse(bee ? "Bee Sting Swarm" : "Pufferfish Poison Burst", 45);
    }

    private static PowerUse inkCloud(ServerPlayer player, boolean glowing) {
        for (LivingEntity target : nearbyTargets(player, 6.0)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), player);
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
        particleBurst(player, glowing ? ParticleTypes.GLOW_SQUID_INK : ParticleTypes.SQUID_INK, 90, 3.0);
        player.playSound(glowing ? SoundEvents.GLOW_SQUID_SQUIRT : SoundEvents.SQUID_SQUIRT, 1.5F, 1.0F);
        return new PowerUse(glowing ? "Glowing Ink Cloud" : "Ink Cloud", 55);
    }

    private static PowerUse frogTongue(ServerPlayer player) {
        Optional<LivingEntity> target = requireTarget(player, 14.0);
        if (target.isEmpty()) return null;
        LivingEntity victim = target.get();
        drawBeam(player.level(), ParticleTypes.ITEM_SLIME, player.getEyePosition(), victim.getEyePosition(), 0.35);
        Vec3 pull = player.position().subtract(victim.position()).normalize();
        victim.setDeltaMovement(victim.getDeltaMovement().add(pull.scale(1.35)).add(0.0, 0.35, 0.0));
        victim.hurtServer(player.level(), player.damageSources().playerAttack(player), 3.0F);
        player.playSound(SoundEvents.FROG_TONGUE, 1.5F, 1.0F);
        return new PowerUse("Frog Tongue Pull", 30);
    }

    private static PowerUse aerialDash(ServerPlayer player, String name, int cooldown) {
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.scale(0.85).add(0.0, 0.18, 0.0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
        particleBurst(player, ParticleTypes.CLOUD, 35, 1.2);
        player.playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 1.1F);
        return new PowerUse(name, cooldown);
    }

    private static PowerUse allayCharm(ServerPlayer player) {
        player.heal(8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 1));
        particleBurst(player, ParticleTypes.HEART, 35, 2.0);
        player.playSound(SoundEvents.ALLAY_THROW, 1.2F, 1.2F);
        return new PowerUse("Allay Healing Charm", 100);
    }

    private static PowerUse aquaticSurge(ServerPlayer player, String path) {
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.scale(path.equals("dolphin") ? 0.9 : 0.7));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 240, 0));
        if (path.equals("axolotl")) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        if (path.equals("nautilus")) player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 180, 2));
        particleBurst(player, ParticleTypes.BUBBLE, 60, 2.0);
        player.playSound(SoundEvents.DOLPHIN_SWIM, 1.2F, 1.0F);
        return new PowerUse(path.equals("dolphin") ? "Dolphin Dash" : "Aquatic Surge", 34);
    }

    private static PowerUse shellGuard(ServerPlayer player, String path) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 240, path.equals("armadillo") ? 3 : 2));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 2));
        particleBurst(player, ParticleTypes.ENCHANTED_HIT, 45, 1.4);
        player.playSound(path.equals("armadillo") ? SoundEvents.ARMADILLO_ROLL : SoundEvents.PLAYER_ATTACK_STRONG, 1.4F, 0.8F);
        return new PowerUse(path.equals("armadillo") ? "Armadillo Roll Guard" : "Turtle Shell Guard", 100);
    }

    private static PowerUse leap(ServerPlayer player, String path) {
        Vec3 look = horizontalLook(player);
        player.setDeltaMovement(player.getDeltaMovement().add(look.scale(path.equals("rabbit") ? 0.45 : 0.3))
                .add(0.0, path.equals("rabbit") ? 0.9 : 0.7, 0.0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
        particleBurst(player, ParticleTypes.CLOUD, 25, 0.9);
        return new PowerUse(path.equals("rabbit") ? "Rabbit Super Leap" : "Chicken Glide Leap", 28);
    }

    private static PowerUse pounce(ServerPlayer player, String path) {
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.scale(0.75).add(0.0, 0.25, 0.0));
        Optional<LivingEntity> target = findLookTarget(player, 8.0);
        if (target.isPresent()) {
            LivingEntity victim = target.get();
            victim.hurtServer(player.level(), player.damageSources().playerAttack(player), path.equals("wolf") ? 8.0F : 6.0F);
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), player);
        }
        particleBurst(player, ParticleTypes.CRIT, 30, 1.0);
        return new PowerUse(displayName("minecraft:" + path) + " Pounce", 30);
    }

    private static PowerUse mountCharge(ServerPlayer player, String path) {
        Vec3 look = horizontalLook(player);
        player.setDeltaMovement(player.getDeltaMovement().add(look.scale(0.7)).add(0.0, 0.15, 0.0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 3));
        damageNearby(player, 2.5, path.contains("husk") ? 8.0F : 5.0F, 1.2, 0.25);
        particleBurst(player, ParticleTypes.CLOUD, 40, 1.2);
        return new PowerUse("Stampede Charge", 40);
    }

    private static PowerUse nourishingMeal(ServerPlayer player, String path) {
        player.heal(path.equals("mooshroom") ? 12.0F : 8.0F);
        player.getFoodData().eat(8, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1));
        particleBurst(player, ParticleTypes.HEART, 30, 1.4);
        return new PowerUse(path.equals("mooshroom") ? "Mooshroom Stew Heal" : "Cow Milk Heal", 100);
    }

    private static PowerUse woolGuard(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 3));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 180, 1));
        particleBurst(player, ParticleTypes.POOF, 55, 1.6);
        return new PowerUse("Wool Shield", 100);
    }

    private static PowerUse heavySwipe(ServerPlayer player, String path) {
        damageNearby(player, 4.0, path.equals("polar_bear") ? 10.0F : 8.0F, 1.5, 0.45);
        particleBurst(player, ParticleTypes.CRIT, 45, 2.0);
        player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 0.75F);
        return new PowerUse(path.equals("polar_bear") ? "Polar Bear Swipe" : "Panda Slam", 44);
    }

    private static PowerUse scentSearch(ServerPlayer player) {
        for (LivingEntity target : nearbyTargets(player, 32.0)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0), player);
        }
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 300, 2));
        particleBurst(player, ParticleTypes.HAPPY_VILLAGER, 45, 3.0);
        player.playSound(SoundEvents.SNIFFER_SNIFFING, 1.5F, 1.0F);
        return new PowerUse("Sniffer Scent Search", 100);
    }

    private static PowerUse villagerBlessing(ServerPlayer player) {
        player.heal(6.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 1));
        player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 1));
        particleBurst(player, ParticleTypes.HAPPY_VILLAGER, 55, 2.3);
        player.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.2F, 1.0F);
        return new PowerUse("Villager Blessing", 120);
    }

    private static PowerUse goldRush(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 220, 2));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 220, 2));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 220, 2));
        particleBurst(player, ParticleTypes.ENCHANT, 50, 1.8);
        return new PowerUse("Piglin Gold Rush", 80);
    }

    private static PowerUse brutalStrike(ServerPlayer player, String path) {
        Optional<LivingEntity> target = requireTarget(player, 6.0);
        if (target.isEmpty()) return null;
        LivingEntity victim = target.get();
        victim.hurtServer(player.level(), player.damageSources().playerAttack(player), path.equals("piglin_brute") ? 14.0F : 11.0F);
        Vec3 look = horizontalLook(player);
        victim.push(look.x * 1.5, 0.5, look.z * 1.5);
        if (path.equals("wither_skeleton")) victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 180, 1), player);
        particleBurstAt(player.level(), ParticleTypes.CRIT, victim.position().add(0.0, 1.0, 0.0), 35, 0.7);
        player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 0.8F);
        return new PowerUse(path.equals("wither_skeleton") ? "Withering Sword Strike" : "Brutal Axe Strike", 32);
    }

    private static PowerUse undeadSurge(ServerPlayer player, String path) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, path.equals("husk") ? 2 : 1));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 240, 1));
        for (LivingEntity target : nearbyTargets(player, 4.0)) {
            target.addEffect(new MobEffectInstance(path.equals("husk") ? MobEffects.HUNGER : MobEffects.WEAKNESS, 180, 1), player);
        }
        particleBurst(player, path.equals("husk") ? ParticleTypes.ASH : ParticleTypes.SOUL, 50, 2.0);
        return new PowerUse(path.equals("husk") ? "Husk Hunger Aura" : "Zombie Undead Surge", 70);
    }

    private static PowerUse creakingRoot(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 260, 3));
        for (LivingEntity target : nearbyTargets(player, 7.0)) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 4), player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 1), player);
        }
        particleBurst(player, ParticleTypes.WHITE_ASH, 70, 3.0);
        player.playSound(SoundEvents.CREAKING_FREEZE, 1.5F, 0.8F);
        return new PowerUse("Creaking Root Snare", 85);
    }

    private static PowerUse infestation(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 3));
        for (LivingEntity target : nearbyTargets(player, 4.0)) {
            target.hurtServer(player.level(), player.damageSources().magic(), 5.0F);
            target.addEffect(new MobEffectInstance(MobEffects.INFESTED, 160, 0), player);
        }
        particleBurst(player, ParticleTypes.POOF, 60, 2.0);
        return new PowerUse("Infestation Burst", 55);
    }

    private static PowerUse striderSprint(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 3));
        player.setDeltaMovement(player.getDeltaMovement().add(horizontalLook(player).scale(0.55)));
        particleBurst(player, ParticleTypes.FLAME, 45, 1.5);
        return new PowerUse("Strider Lava Sprint", 55);
    }

    private static PowerUse fallbackPower(ServerPlayer player, EntityType<?> type) {
        MobCategory category = type.getCategory();
        if (category == MobCategory.MONSTER) {
            damageNearby(player, 4.0, 7.0F, 1.2, 0.4);
            particleBurst(player, ParticleTypes.DAMAGE_INDICATOR, 40, 1.8);
            return new PowerUse("Monster Fury", 40);
        }
        if (category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return aquaticSurge(player, "aquatic_mob");
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 2));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
        player.heal(4.0F);
        particleBurst(player, ParticleTypes.TOTEM_OF_UNDYING, 35, 1.5);
        return new PowerUse("Survival Instinct", 60);
    }

    private static boolean useSonicBoomPower(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(20.0));
        Optional<LivingEntity> target = findLookTarget(player, start, end);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.literal("Aim at a mob or player within 20 blocks."), true);
            return false;
        }

        LivingEntity victim = target.get();
        Vec3 delta = victim.getEyePosition().subtract(start);
        Vec3 direction = delta.normalize();
        int steps = (int) Math.floor(delta.length()) + 7;
        for (int i = 1; i < steps; i++) {
            Vec3 particle = start.add(direction.scale(i));
            player.level().sendParticles(
                    ParticleTypes.SONIC_BOOM, particle.x, particle.y, particle.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        player.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
        if (victim.hurtServer(player.level(), player.damageSources().sonicBoom(player), 10.0F)) {
            double resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            victim.push(
                    direction.x * 2.5 * (1.0 - resistance),
                    direction.y * 0.5 * (1.0 - resistance),
                    direction.z * 2.5 * (1.0 - resistance));
        }
        return true;
    }

    private static Optional<LivingEntity> requireTarget(ServerPlayer player, double range) {
        Optional<LivingEntity> target = findLookTarget(player, range);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.literal("Aim at a mob or player within " + (int) range + " blocks."), true);
        }
        return target;
    }

    private static Optional<LivingEntity> findLookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        return findLookTarget(player, start, start.add(player.getLookAngle().normalize().scale(range)));
    }

    private static Optional<LivingEntity> findLookTarget(ServerPlayer player, Vec3 start, Vec3 end) {
        Vec3 movement = end.subtract(start);
        AABB search = player.getBoundingBox().expandTowards(movement).inflate(1.5);
        return player.level().getEntitiesOfClass(
                        LivingEntity.class, search, entity -> entity != player && entity.isPickable() && !player.isAlliedTo(entity))
                .stream()
                .filter(entity -> entity.getBoundingBox().inflate(0.7).clip(start, end).isPresent())
                .min(Comparator.comparingDouble(entity -> start.distanceToSqr(entity.getEyePosition())));
    }

    private static List<LivingEntity> nearbyTargets(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && !player.isAlliedTo(entity));
    }

    private static void damageNearby(ServerPlayer player, double radius, float damage, double horizontalPush, double verticalPush) {
        for (LivingEntity target : nearbyTargets(player, radius)) {
            target.hurtServer(player.level(), player.damageSources().playerAttack(player), damage);
            Vec3 away = target.position().subtract(player.position());
            Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
            if (horizontal.lengthSqr() < 0.001) horizontal = horizontalLook(player);
            else horizontal = horizontal.normalize();
            target.push(horizontal.x * horizontalPush, verticalPush, horizontal.z * horizontalPush);
        }
    }

    private static void drawBeam(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.01) return;
        Vec3 direction = delta.scale(1.0 / length);
        int steps = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(direction.scale(Math.min(length, i * spacing)));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void particleBurst(ServerPlayer player, ParticleOptions particle, int count, double spread) {
        particleBurstAt(player.level(), particle, player.position().add(0.0, 1.0, 0.0), count, spread);
    }

    private static void particleBurstAt(ServerLevel level, ParticleOptions particle, Vec3 point, int count, double spread) {
        level.sendParticles(particle, point.x, point.y, point.z, count, spread, spread * 0.6, spread, 0.08);
    }

    private static void placeAtMuzzle(ServerPlayer player, Entity projectile, Vec3 direction) {
        Vec3 muzzle = player.getEyePosition().add(direction.normalize().scale(1.0));
        projectile.setPos(muzzle.x, muzzle.y - 0.1, muzzle.z);
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.001) {
            double radians = Math.toRadians(player.getYRot());
            return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        }
        return horizontal.normalize();
    }

    private static double findGroundY(ServerLevel level, double x, double startY, double z) {
        BlockPos cursor = BlockPos.containing(x, startY + 2.0, z);
        for (int i = 0; i < 8; i++) {
            if (level.getBlockState(cursor).isAir() && level.getBlockState(cursor.below()).blocksMotion()) {
                return cursor.getY();
            }
            cursor = cursor.below();
        }
        return startY;
    }

    public static void tick(ServerPlayer player) {
        String selected = player.getPersistentData().getString(DATA_KEY).orElse("");
        if (selected.isBlank()) return;
        EntityType<?> type = findType(selected);
        if (type == null) {
            clearSilently(player);
            return;
        }
        if (player.tickCount % 20 == 0) applyPowers(player, type);
        if (player.tickCount % 100 == 0) sync(player, selected);
    }

    private static int transform(ServerPlayer player, String rawName) {
        EntityType<?> type = findType(rawName);
        if (type == null) {
            player.sendSystemMessage(Component.literal("Unknown mob: " + rawName));
            return 0;
        }
        Entity sample = type.create(player.level(), EntitySpawnReason.COMMAND);
        if (!(sample instanceof LivingEntity)) {
            if (sample != null) sample.discard();
            player.sendSystemMessage(Component.literal(rawName + " is not a living mob."));
            return 0;
        }
        sample.discard();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        player.getPersistentData().putString(DATA_KEY, id);
        POWER_COOLDOWNS.remove(player.getUUID());
        sync(player, id);
        applyPowers(player, type);
        player.sendSystemMessage(Component.literal(
                "Transformed into " + displayName(id) + ". Type /power to use this mob's active power."));
        return 1;
    }

    private static int clear(ServerPlayer player) {
        clearSilently(player);
        player.sendSystemMessage(Component.literal("Transformation cleared."));
        return 1;
    }

    private static int showCurrent(ServerPlayer player) {
        String selected = player.getPersistentData().getString(DATA_KEY).orElse("");
        player.sendSystemMessage(Component.literal(selected.isBlank()
                ? "Use /transform <mob>, for example /transform blaze. Use /transform clear to return."
                : "Current transformation: " + displayName(selected) + ". Type /power to use its active power."));
        return 1;
    }

    private static void clearSilently(ServerPlayer player) {
        player.getPersistentData().remove(DATA_KEY);
        POWER_COOLDOWNS.remove(player.getUUID());
        sync(player, "");
        if (!player.isCreative() && !player.isSpectator() && !SuitPowerHandler.hasFullSuit(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void applyPowers(ServerPlayer player, EntityType<?> type) {
        TransformationTraits traits = TRAIT_CACHE.computeIfAbsent(type, ignored -> readTraits(player, type));
        double health = traits.health();
        double damage = traits.damage();
        double speed = traits.speed();

        addEffect(player, MobEffects.HEALTH_BOOST, amplifier((health - 20.0) / 4.0, 4));
        addEffect(player, MobEffects.STRENGTH, amplifier((damage - 2.0) / 3.0, 4));
        addEffect(player, MobEffects.SPEED, amplifier((speed - 0.1) / 0.08, 3));

        MobCategory category = type.getCategory();
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        boolean aquatic = category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE || path.contains("squid")
                || path.contains("dolphin") || path.contains("guardian") || path.contains("turtle")
                || path.contains("nautilus");
        boolean flying = path.contains("bat") || path.contains("bee") || path.contains("blaze")
                || path.contains("ghast") || path.contains("phantom") || path.contains("parrot")
                || path.contains("allay") || path.contains("vex") || path.contains("dragon") || path.contains("wither");
        boolean fireproof = type.fireImmune() || path.contains("blaze") || path.contains("magma") || path.contains("strider");
        boolean nightVision = category == MobCategory.MONSTER || path.contains("bat") || path.contains("cat");

        if (aquatic) {
            addEffect(player, MobEffects.WATER_BREATHING, 0);
            addEffect(player, MobEffects.DOLPHINS_GRACE, 0);
        }
        if (fireproof) addEffect(player, MobEffects.FIRE_RESISTANCE, 0);
        if (nightVision) addEffect(player, MobEffects.NIGHT_VISION, 0);
        if (flying) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            addEffect(player, MobEffects.SLOW_FALLING, 0);
        } else if (!player.isCreative() && !player.isSpectator() && !SuitPowerHandler.hasFullSuit(player)) {
            if (player.getAbilities().mayfly || player.getAbilities().flying) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        if (health >= 100.0) addEffect(player, MobEffects.RESISTANCE, health >= 300.0 ? 2 : 1);
    }

    private static TransformationTraits readTraits(ServerPlayer player, EntityType<?> type) {
        Entity sample = type.create(player.level(), EntitySpawnReason.COMMAND);
        if (!(sample instanceof LivingEntity living)) {
            if (sample != null) sample.discard();
            return new TransformationTraits(20.0, 2.0, 0.1);
        }
        double health = living.getAttribute(Attributes.MAX_HEALTH) == null
                ? 20.0
                : living.getAttributeValue(Attributes.MAX_HEALTH);
        double damage = living.getAttribute(Attributes.ATTACK_DAMAGE) == null
                ? 2.0
                : living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = living.getAttribute(Attributes.MOVEMENT_SPEED) == null
                ? 0.1
                : living.getAttributeValue(Attributes.MOVEMENT_SPEED);
        sample.discard();
        return new TransformationTraits(health, damage, speed);
    }

    private static void addEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        if (amplifier >= 0) player.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, true));
    }

    private static int amplifier(double value, int maximum) {
        if (value <= 0.0) return -1;
        return Math.min(maximum, Math.max(0, (int) Math.ceil(value) - 1));
    }

    private static EntityType<?> findType(String rawName) {
        String normalized = rawName.toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
        return BuiltInRegistries.ENTITY_TYPE.getValue(id);
    }

    private static void sync(ServerPlayer player, String entityType) {
        PacketDistributor.sendToAllPlayers(new TransformationPayload(player.getId(), entityType));
    }

    private static String displayName(String id) {
        String path = id.substring(id.indexOf(':') + 1).replace('_', ' ');
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private record PowerUse(String name, int cooldownTicks) {}

    private record TransformationTraits(double health, double damage, double speed) {}

    private TransformationHandler() {}
}
