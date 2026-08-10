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
        int left = width / 2 - 184;
        int right = width / 2 + 4;
        int y = 72;
        addRenderableWidget(actionButton("Laser Rifle", left, y, GunSelectorMenu.GIVE_LASER_RIFLE));
        addRenderableWidget(actionButton("Pulse Pistol", left, y + 24, GunSelectorMenu.GIVE_PULSE_PISTOL));
        addRenderableWidget(actionButton("Beam Cannon", left, y + 48, GunSelectorMenu.GIVE_BEAM_CANNON));
        addRenderableWidget(actionButton("Charged Sniper", left, y + 72, GunSelectorMenu.GIVE_CHARGED_SNIPER));
        addRenderableWidget(actionButton("Rifle", right, y, GunSelectorMenu.GIVE_RIFLE));
        addRenderableWidget(actionButton("Minigun", right, y + 24, GunSelectorMenu.GIVE_MINIGUN));
        addRenderableWidget(actionButton("Bazooka", right, y + 48, GunSelectorMenu.GIVE_BAZOOKA));
        addRenderableWidget(actionButton("Grenade Launcher", right, y + 72, GunSelectorMenu.GIVE_GRENADE_LAUNCHER));
        addRenderableWidget(actionButton("Give All", width / 2 - 90, y + 104, GunSelectorMenu.GIVE_ALL));
    }

    @Override
    protected Component subtitle() {
        return Component.literal("Weapon access");
    }
}
