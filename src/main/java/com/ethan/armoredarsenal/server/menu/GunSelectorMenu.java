package com.ethan.armoredarsenal.server.menu;

import com.ethan.armoredarsenal.registry.ModMenus;
import com.ethan.armoredarsenal.server.MenuActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class GunSelectorMenu extends ActionMenu {
    public static final int GIVE_LASER_RIFLE = 0;
    public static final int GIVE_PULSE_PISTOL = 1;
    public static final int GIVE_BEAM_CANNON = 2;
    public static final int GIVE_CHARGED_SNIPER = 3;
    public static final int GIVE_ALL = 4;
    public static final int GIVE_RIFLE = 5;
    public static final int GIVE_MINIGUN = 6;
    public static final int GIVE_BAZOOKA = 7;
    public static final int GIVE_GRENADE_LAUNCHER = 8;

    public GunSelectorMenu(int containerId) {
        super(ModMenus.GUN_SELECTOR.get(), containerId);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuActions.handleGunButton(serverPlayer, id);
            return true;
        }
        return false;
    }
}
