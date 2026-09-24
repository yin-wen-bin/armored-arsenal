package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.WeaponProfile;
import com.ethan.armoredarsenal.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class SuitPowerHandler {
    private static final int MAX_ENERGY = 100;
    private static final Map<UUID, SuitState> STATES = new HashMap<>();

    public static void tick(ServerPlayer player) {
        SuitState state = state(player);
        boolean suited = hasFullSuit(player);
        boolean infinity = hasFullInfinitySuit(player);

        if (!suited) {
            if (state.flightGranted) {
                revokeFlight(player, state);
            }
            state.hover = false;
            state.stealth = false;
            return;
        }

        grantFlight(player, state);

        if (player.tickCount % 5 == 0 && state.energy < MAX_ENERGY) {
            state.energy = Math.min(MAX_ENERGY, state.energy + (state.stealth ? 1 : 2));
        }

        if (state.hover) {
            applyHover(player, state);
        }

        if (isPoweredArmorHelmet(player.getItemBySlot(EquipmentSlot.HEAD))) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }

        if (state.stealth) {
            if (infinity) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 0, true, false, true));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, true));
            }
            if (player.tickCount % 20 == 0) {
                if (!consumeEnergy(player, state, 2)) {
                    state.stealth = false;
                    player.sendSystemMessage(Component.literal(infinity
                            ? "Cosmic shield disabled: suit energy depleted."
                            : "Stealth disabled: suit energy depleted."), false);
                }
            }
        }

        if (infinity) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, true));
            if (player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth()
                    && consumeEnergy(player, state, 2)) {
                player.heal(1.0F);
            }
        }

        if (player.tickCount % 20 == 0) {
            String suitName = infinity ? "INFINITY" : "MARK 15";
            String specialName = infinity ? "Shield" : "Stealth";
            player.sendSystemMessage(Component.literal(suitName + " | Energy " + state.energy + "% | Hover "
                    + (state.hover ? "ON" : "OFF") + " | " + specialName + " "
                    + (state.stealth ? "ON" : "OFF")), true);
        }
    }

    public static boolean hasFullSuit(ServerPlayer player) {
        return hasFullMark15Suit(player) || hasFullInfinitySuit(player);
    }

    public static boolean hasFullMark15Suit(ServerPlayer player) {
        return isMark15Piece(player.getItemBySlot(EquipmentSlot.HEAD))
                && isMark15Piece(player.getItemBySlot(EquipmentSlot.CHEST))
                && isMark15Piece(player.getItemBySlot(EquipmentSlot.LEGS))
                && isMark15Piece(player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static boolean hasFullInfinitySuit(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.INFINITY_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.INFINITY_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.INFINITY_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.INFINITY_BOOTS.get());
    }

    public static boolean isMark15Piece(ItemStack stack) {
        return stack.is(ModItems.MARK_15_HELMET.get())
                || stack.is(ModItems.MARK_15_CHESTPLATE.get())
                || stack.is(ModItems.MARK_15_LEGGINGS.get())
                || stack.is(ModItems.MARK_15_BOOTS.get());
    }

    public static boolean isInfinityPiece(ItemStack stack) {
        return stack.is(ModItems.INFINITY_HELMET.get())
                || stack.is(ModItems.INFINITY_CHESTPLATE.get())
                || stack.is(ModItems.INFINITY_LEGGINGS.get())
                || stack.is(ModItems.INFINITY_BOOTS.get());
    }

    public static boolean isPoweredArmorPiece(ItemStack stack) {
        return isMark15Piece(stack) || isInfinityPiece(stack);
    }

    private static boolean isPoweredArmorHelmet(ItemStack stack) {
        return stack.is(ModItems.MARK_15_HELMET.get()) || stack.is(ModItems.INFINITY_HELMET.get());
    }

    public static void toggleHover(ServerPlayer player) {
        if (!hasFullSuit(player)) {
            player.sendSystemMessage(Component.literal("Equip a full powered armor set first."), false);
            return;
        }

        SuitState state = state(player);
        if (!state.hover && state.energy < 10) {
            player.sendSystemMessage(Component.literal("Suit energy too low for hover."), false);
            return;
        }

        state.hover = !state.hover;
        grantFlight(player, state);
        player.sendSystemMessage(Component.literal("Hover " + (state.hover ? "enabled." : "disabled.")), false);
    }

    public static void toggleStealth(ServerPlayer player) {
        if (!hasFullSuit(player)) {
            player.sendSystemMessage(Component.literal("Equip a full powered armor set first."), false);
            return;
        }

        SuitState state = state(player);
        if (!state.stealth && state.energy < 15) {
            player.sendSystemMessage(Component.literal("Suit energy too low for special mode."), false);
            return;
        }

        state.stealth = !state.stealth;
        String special = hasFullInfinitySuit(player) ? "Cosmic shield " : "Stealth ";
        player.sendSystemMessage(Component.literal(special + (state.stealth ? "enabled." : "disabled.")), false);
    }

    public static void fireRepulsor(ServerPlayer player) {
        if (!hasFullSuit(player)) {
            player.sendSystemMessage(Component.literal("Equip a full powered armor set first."), false);
            return;
        }

        SuitState state = state(player);
        long gameTime = player.level().getGameTime();
        if (gameTime < state.repulsorReadyAt) {
            player.sendSystemMessage(Component.literal("Repulsor charging."), true);
            return;
        }

        if (!consumeEnergy(player, state, WeaponProfile.REPULSOR.suitEnergyCost())) {
            player.sendSystemMessage(Component.literal("Suit energy too low for repulsors."), false);
            return;
        }

        state.repulsorReadyAt = gameTime + WeaponProfile.REPULSOR.cooldownTicks();
        LaserLogic.fireBeam(player, WeaponProfile.REPULSOR);
    }

    public static boolean consumeSuitEnergy(ServerPlayer player, int amount) {
        return consumeEnergy(player, state(player), amount);
    }

    public static int energy(ServerPlayer player) {
        return state(player).energy;
    }

    public static void clearSuitState(ServerPlayer player) {
        SuitState state = state(player);
        state.hover = false;
        state.stealth = false;
        revokeFlight(player, state);
    }

    private static void grantFlight(ServerPlayer player, SuitState state) {
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        state.flightGranted = true;
    }

    private static void revokeFlight(ServerPlayer player, SuitState state) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        state.flightGranted = false;
    }

    private static void applyHover(ServerPlayer player, SuitState state) {
        if (player.tickCount % 10 == 0 && !consumeEnergy(player, state, 1)) {
            state.hover = false;
            player.sendSystemMessage(Component.literal("Hover disabled: suit energy depleted."), false);
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        if (!player.onGround() && motion.y < -0.03D) {
            player.setDeltaMovement(motion.x, -0.03D, motion.z);
            player.resetFallDistance();
        }
    }

    private static boolean consumeEnergy(ServerPlayer player, SuitState state, int amount) {
        if (state.energy < amount) {
            return false;
        }
        state.energy -= amount;
        return true;
    }

    private static SuitState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new SuitState());
    }

    private static final class SuitState {
        private int energy = MAX_ENERGY;
        private boolean hover;
        private boolean stealth;
        private boolean flightGranted;
        private long repulsorReadyAt;
    }

    private SuitPowerHandler() {}
}
