package com.ethan.armoredarsenal.registry;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.server.menu.CreativeSupplyMenu;
import com.ethan.armoredarsenal.server.menu.GunSelectorMenu;
import com.ethan.armoredarsenal.server.menu.SuitSelectorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ArmoredArsenal.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SuitSelectorMenu>> SUIT_SELECTOR =
            MENUS.register("suit_selector", () -> IMenuTypeExtension.create((windowId, inventory, data) -> new SuitSelectorMenu(windowId)));
    public static final DeferredHolder<MenuType<?>, MenuType<GunSelectorMenu>> GUN_SELECTOR =
            MENUS.register("gun_selector", () -> IMenuTypeExtension.create((windowId, inventory, data) -> new GunSelectorMenu(windowId)));
    public static final DeferredHolder<MenuType<?>, MenuType<CreativeSupplyMenu>> CREATIVE_SUPPLY =
            MENUS.register("creative_supply", () -> IMenuTypeExtension.create((windowId, inventory, data) -> new CreativeSupplyMenu(windowId)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private ModMenus() {}
}

