package com.ethan.armoredarsenal.registry;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.InstructionBook;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArmoredArsenal.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARMORED_ARSENAL = TABS.register(
            "armored_arsenal",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.armoredarsenal"))
                    .icon(() -> new ItemStack(ModItems.MARK_15_CHESTPLATE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MARK_15_HELMET.get());
                        output.accept(ModItems.MARK_15_CHESTPLATE.get());
                        output.accept(ModItems.MARK_15_LEGGINGS.get());
                        output.accept(ModItems.MARK_15_BOOTS.get());
                        output.accept(ModItems.LASER_RIFLE.get());
                        output.accept(ModItems.PULSE_PISTOL.get());
                        output.accept(ModItems.BEAM_CANNON.get());
                        output.accept(ModItems.CHARGED_SNIPER_LASER.get());
                        output.accept(ModItems.WATER_FLOOD_TNT.get());
                        ModItems.COUCHES.values().forEach(item -> output.accept(item.get()));
                        ModItems.ARMCHAIRS.values().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.WALL_TV.get());
                        output.accept(ModItems.PORTABLE_LASER.get());
                        output.accept(ModItems.AUTO_TURRET.get());
                        output.accept(InstructionBook.create());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
