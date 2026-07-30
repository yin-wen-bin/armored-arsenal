package com.ethan.armoredarsenal.server.menu;

import com.ethan.armoredarsenal.registry.ModMenus;
import com.ethan.armoredarsenal.server.MenuActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class CreativeSupplyMenu extends ActionMenu {
    public static final int BUILDING = 0;
    public static final int VALUABLES = 1;
    public static final int REDSTONE = 2;
    public static final int SURVIVAL = 3;
    public static final int MODDED = 4;
    public static final int SPAWN_EGGS = 5;

    public CreativeSupplyMenu(int containerId) {
        super(ModMenus.CREATIVE_SUPPLY.get(), containerId);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuActions.handleSupplyButton(serverPlayer, id);
            return true;
        }
        return false;
    }
}

