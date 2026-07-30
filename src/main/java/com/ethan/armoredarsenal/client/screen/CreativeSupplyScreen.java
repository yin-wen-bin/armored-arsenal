package com.ethan.armoredarsenal.client.screen;

import com.ethan.armoredarsenal.server.menu.CreativeSupplyMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CreativeSupplyScreen extends BaseMenuScreen<CreativeSupplyMenu> {
    public CreativeSupplyScreen(CreativeSupplyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addButtons() {
        int x = width / 2 - 90;
        int y = 64;
        addRenderableWidget(actionButton("Building Kit", x, y, CreativeSupplyMenu.BUILDING));
        addRenderableWidget(actionButton("Valuables Kit", x, y + 24, CreativeSupplyMenu.VALUABLES));
        addRenderableWidget(actionButton("Redstone Kit", x, y + 48, CreativeSupplyMenu.REDSTONE));
        addRenderableWidget(actionButton("Survival Kit", x, y + 72, CreativeSupplyMenu.SURVIVAL));
        addRenderableWidget(actionButton("Suit + Guns", x, y + 96, CreativeSupplyMenu.MODDED));
        addRenderableWidget(actionButton("Mob Eggs", x, y + 120, CreativeSupplyMenu.SPAWN_EGGS));
    }

    @Override
    protected Component subtitle() {
        return Component.literal("Survival-safe item supply");
    }
}

