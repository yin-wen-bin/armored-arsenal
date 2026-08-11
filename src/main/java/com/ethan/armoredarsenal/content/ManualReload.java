package com.ethan.armoredarsenal.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ManualReload {
    private static final String LOADED_KEY = "armoredarsenal_loaded";

    public static boolean requireLoaded(ServerPlayer player, ItemStack weapon, String weaponName) {
        if (!isLoaded(weapon)) {
            player.sendSystemMessage(Component.literal(
                    weaponName + " is unloaded. Crouch and right-click to reload."), true);
            return false;
        }
        setLoaded(weapon, false);
        return true;
    }

    public static void reload(ServerPlayer player, ItemStack weapon, Item ammunition, String ammunitionName) {
        if (isLoaded(weapon)) {
            player.sendSystemMessage(Component.literal("The weapon is already loaded."), true);
            return;
        }
        if (!player.isCreative() && !consumeOne(player, ammunition)) {
            player.sendSystemMessage(Component.literal("You need 1 " + ammunitionName + " to reload."), true);
            return;
        }
        setLoaded(weapon, true);
        player.level().playSound(
                null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
        player.sendSystemMessage(Component.literal("Weapon loaded."), true);
    }

    private static boolean isLoaded(ItemStack weapon) {
        return weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr(LOADED_KEY, false);
    }

    private static void setLoaded(ItemStack weapon, boolean loaded) {
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, tag -> tag.putBoolean(LOADED_KEY, loaded));
    }

    private static boolean consumeOne(ServerPlayer player, Item ammunition) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ammunition)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private ManualReload() {}
}
