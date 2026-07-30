package com.ethan.armoredarsenal.server.menu;

import com.ethan.armoredarsenal.registry.ModMenus;
import com.ethan.armoredarsenal.server.MenuActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SuitSelectorMenu extends ActionMenu {
    public static final int EQUIP_MARK_15 = 0;
    public static final int TOGGLE_HOVER = 1;
    public static final int FIRE_REPULSOR = 2;
    public static final int TOGGLE_STEALTH = 3;
    public static final int REMOVE_SUIT = 4;

    public SuitSelectorMenu(int containerId) {
        super(ModMenus.SUIT_SELECTOR.get(), containerId);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuActions.handleSuitButton(serverPlayer, id);
            return true;
        }
        return false;
    }
}

