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

        boolean hasHelmet = minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MARK_15_HELMET.get());
        boolean hasChest = minecraft.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MARK_15_CHESTPLATE.get());
        boolean hasLegs = minecraft.player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.MARK_15_LEGGINGS.get());
        boolean hasBoots = minecraft.player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.MARK_15_BOOTS.get());
        if (!hasHelmet && !hasChest && !hasLegs && !hasBoots) {
            return;
        }

        Font font = minecraft.font;
        int x = 10;
        int y = graphics.guiHeight() - 58;
        String ready = hasHelmet && hasChest && hasLegs && hasBoots ? "READY" : "PARTIAL";
        graphics.text(font, Component.literal("MARK 15 " + ready), x, y, 0xFFE6C15A);
        graphics.text(font, Component.literal("Health " + Math.round(minecraft.player.getHealth()) + " / "
                + Math.round(minecraft.player.getMaxHealth())), x, y + font.lineHeight + 2, 0xFFFFFFFF);
    }
}

