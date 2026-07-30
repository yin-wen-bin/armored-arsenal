package com.ethan.armoredarsenal.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

abstract class BaseMenuScreen<M extends AbstractContainerMenu> extends Screen implements MenuAccess<M> {
    protected final M menu;
    protected final Inventory inventory;

    protected BaseMenuScreen(M menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        this.inventory = inventory;
    }

    @Override
    public M getMenu() {
        return menu;
    }

    @Override
    protected void init() {
        super.init();
        addButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 28, 0xFFFFFFFF);
        graphics.centeredText(font, subtitle(), width / 2, 44, 0xFFB7C9D9);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    protected abstract void addButtons();

    protected abstract Component subtitle();

    protected Button actionButton(String label, int x, int y, int buttonId) {
        return Button.builder(Component.literal(label), button -> sendButton(buttonId))
                .pos(x, y)
                .size(180, 20)
                .build();
    }

    protected void sendButton(int buttonId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }
}

