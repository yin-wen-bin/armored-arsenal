package com.ethan.armoredarsenal.client.screen;

import com.ethan.armoredarsenal.server.menu.SuitSelectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SuitSelectorScreen extends BaseMenuScreen<SuitSelectorMenu> {
    public SuitSelectorScreen(SuitSelectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addButtons() {
        int x = width / 2 - 90;
        int y = 58;
        addRenderableWidget(actionButton("Equip Mark 15", x, y, SuitSelectorMenu.EQUIP_MARK_15));
        addRenderableWidget(actionButton("Equip Infinity Armor", x, y + 24, SuitSelectorMenu.EQUIP_INFINITY));
        addRenderableWidget(actionButton("Toggle Hover", x, y + 48, SuitSelectorMenu.TOGGLE_HOVER));
        addRenderableWidget(actionButton("Fire Energy Beam", x, y + 72, SuitSelectorMenu.FIRE_REPULSOR));
        addRenderableWidget(actionButton("Toggle Special", x, y + 96, SuitSelectorMenu.TOGGLE_STEALTH));
        addRenderableWidget(actionButton("Remove Suit", x, y + 120, SuitSelectorMenu.REMOVE_SUIT));
    }

    @Override
    protected Component subtitle() {
        return Component.literal("Powered armor controls");
    }
}
