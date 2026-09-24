package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.registry.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.gui.GuiLayer;

public final class SuitHudLayer implements GuiLayer {
    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        boolean infinityHelmet = minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.INFINITY_HELMET.get());
        boolean infinityChest = minecraft.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.INFINITY_CHESTPLATE.get());
        boolean infinityLegs = minecraft.player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.INFINITY_LEGGINGS.get());
        boolean infinityBoots = minecraft.player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.INFINITY_BOOTS.get());
        boolean infinity = infinityHelmet || infinityChest || infinityLegs || infinityBoots;
        boolean hasHelmet = infinity ? infinityHelmet
                : minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MARK_15_HELMET.get());
        boolean hasChest = infinity ? infinityChest
                : minecraft.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MARK_15_CHESTPLATE.get());
        boolean hasLegs = infinity ? infinityLegs
                : minecraft.player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.MARK_15_LEGGINGS.get());
        boolean hasBoots = infinity ? infinityBoots
                : minecraft.player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.MARK_15_BOOTS.get());
        if (!hasHelmet && !hasChest && !hasLegs && !hasBoots) {
            return;
        }

        Font font = minecraft.font;
        int x = 10;
        int y = graphics.guiHeight() - 58;
        String ready = hasHelmet && hasChest && hasLegs && hasBoots ? "READY" : "PARTIAL";
        String armorName = infinity ? "INFINITY" : "MARK 15";
        int armorColor = infinity ? 0xFF41E8FF : 0xFFE6C15A;
        graphics.text(font, Component.literal(armorName + " " + ready), x, y, armorColor);
        graphics.text(font, Component.literal("Health " + Math.round(minecraft.player.getHealth()) + " / "
                + Math.round(minecraft.player.getMaxHealth())), x, y + font.lineHeight + 2, 0xFFFFFFFF);
    }
}
