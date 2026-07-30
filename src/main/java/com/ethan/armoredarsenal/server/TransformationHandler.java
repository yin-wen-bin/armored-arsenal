package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.network.TransformationPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TransformationHandler {
    private static final String DATA_KEY = "ArmoredArsenalTransformation";
    private static final SuggestionProvider<CommandSourceStack> MOB_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString), builder);

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("transform")
                .executes(context -> showCurrent(context.getSource().getPlayerOrException()))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource().getPlayerOrException())))
                .then(Commands.argument("mob", StringArgumentType.word())
                        .suggests(MOB_SUGGESTIONS)
                        .executes(context -> transform(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "mob")))));
    }

    public static void tick(ServerPlayer player) {
        String selected = player.getPersistentData().getString(DATA_KEY).orElse("");
        if (selected.isBlank()) return;
        EntityType<?> type = findType(selected);
        if (type == null) {
            clearSilently(player);
            return;
        }
        applyPowers(player, type);
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
        sync(player, id);
        player.sendSystemMessage(Component.literal("Transformed into " + displayName(id) + ". Your powers now match this mob."));
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
                : "Current transformation: " + displayName(selected) + "."));
        return 1;
    }

    private static void clearSilently(ServerPlayer player) {
        player.getPersistentData().remove(DATA_KEY);
        sync(player, "");
        if (!player.isCreative() && !player.isSpectator() && !SuitPowerHandler.hasFullSuit(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void applyPowers(ServerPlayer player, EntityType<?> type) {
        Entity sample = type.create(player.level(), EntitySpawnReason.COMMAND);
        if (!(sample instanceof LivingEntity living)) {
            if (sample != null) sample.discard();
            return;
        }
        double health = living.getAttributeValue(Attributes.MAX_HEALTH);
        double damage = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = living.getAttributeValue(Attributes.MOVEMENT_SPEED);
        sample.discard();

        addEffect(player, MobEffects.HEALTH_BOOST, amplifier((health - 20.0) / 4.0, 4));
        addEffect(player, MobEffects.STRENGTH, amplifier((damage - 2.0) / 3.0, 4));
        addEffect(player, MobEffects.SPEED, amplifier((speed - 0.1) / 0.08, 3));

        MobCategory category = type.getCategory();
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        boolean aquatic = category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE || path.contains("squid")
                || path.contains("dolphin") || path.contains("guardian") || path.contains("turtle");
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
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
            addEffect(player, MobEffects.SLOW_FALLING, 0);
        } else if (!player.isCreative() && !player.isSpectator() && !SuitPowerHandler.hasFullSuit(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (health >= 100.0) addEffect(player, MobEffects.RESISTANCE, health >= 300.0 ? 2 : 1);
    }

    private static void addEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        if (amplifier >= 0) player.addEffect(new MobEffectInstance(effect, 40, amplifier, true, false, true));
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

    private TransformationHandler() {}
}