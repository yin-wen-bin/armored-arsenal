package com.ethan.armoredarsenal.client.screen;

import com.ethan.armoredarsenal.server.menu.GunSelectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GunSelectorScreen extends BaseMenuScreen<GunSelectorMenu> {
    public GunSelectorScreen(GunSelectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addButtons() {
        int x = width / 2 - 90;
        int y = 72;
        addRenderableWidget(actionButton("Laser Rifle", x, y, GunSelectorMenu.GIVE_LASER_RIFLE));
        addRenderableWidget(actionButton("Pulse Pistol", x, y + 24, GunSelectorMenu.GIVE_PULSE_PISTOL));
        addRenderableWidget(actionButton("Beam Cannon", x, y + 48, GunSelectorMenu.GIVE_BEAM_CANNON));
        addRenderableWidget(actionButton("Charged Sniper", x, y + 72, GunSelectorMenu.GIVE_CHARGED_SNIPER));
        addRenderableWidget(actionButton("Give All", x, y + 96, GunSelectorMenu.GIVE_ALL));
    }

    @Override
    protected Component subtitle() {
        return Component.literal("Laser weapon access");
    }
}

