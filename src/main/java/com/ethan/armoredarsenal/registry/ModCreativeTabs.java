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
                        output.accept(ModItems.INFINITY_HELMET.get());
                        output.accept(ModItems.INFINITY_CHESTPLATE.get());
                        output.accept(ModItems.INFINITY_LEGGINGS.get());
                        output.accept(ModItems.INFINITY_BOOTS.get());
                        output.accept(ModItems.COMMAND_BLOCK_AXE.get());
                        output.accept(ModItems.WITHERED_WITHER_STAR.get());
                        output.accept(ModItems.WITHERED_BEACON.get());
                        output.accept(ModItems.ARCANITE_ORE.get());
                        output.accept(ModItems.ARCANITE.get());
                        output.accept(ModItems.BEDROCK_ARCANITE.get());
                        output.accept(ModItems.ARCANITE_SWORD.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_SWORD.get());
                        output.accept(ModItems.ARCANITE_PICKAXE.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_PICKAXE.get());
                        output.accept(ModItems.ARCANITE_AXE.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_AXE.get());
                        output.accept(ModItems.ARCANITE_SHOVEL.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_SHOVEL.get());
                        output.accept(ModItems.ARCANITE_HOE.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_HOE.get());
                        output.accept(ModItems.ARCANITE_HELMET.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_HELMET.get());
                        output.accept(ModItems.ARCANITE_CHESTPLATE.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_CHESTPLATE.get());
                        output.accept(ModItems.ARCANITE_LEGGINGS.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_LEGGINGS.get());
                        output.accept(ModItems.ARCANITE_BOOTS.get());
                        output.accept(ModItems.BEDROCK_ARCANITE_BOOTS.get());
                        output.accept(ModItems.LASER_RIFLE.get());
                        output.accept(ModItems.BEDROCK_LASER_RIFLE.get());
                        output.accept(ModItems.PULSE_PISTOL.get());
                        output.accept(ModItems.BEDROCK_PULSE_PISTOL.get());
                        output.accept(ModItems.BEAM_CANNON.get());
                        output.accept(ModItems.BEDROCK_BEAM_CANNON.get());
                        output.accept(ModItems.CHARGED_SNIPER_LASER.get());
                        output.accept(ModItems.BEDROCK_CHARGED_SNIPER_LASER.get());
                        output.accept(ModItems.STINGER_ROCKET.get());
                        output.accept(ModItems.BEDROCK_STINGER_ROCKET.get());
                        output.accept(ModItems.SIEGEBREAKER_ROCKET.get());
                        output.accept(ModItems.BEDROCK_SIEGEBREAKER_ROCKET.get());
                        output.accept(ModItems.TITAN_ROCKET.get());
                        output.accept(ModItems.BEDROCK_TITAN_ROCKET.get());
                        output.accept(ModItems.RIFLE.get());
                        output.accept(ModItems.BEDROCK_RIFLE.get());
                        output.accept(ModItems.MINIGUN.get());
                        output.accept(ModItems.BEDROCK_MINIGUN.get());
                        output.accept(ModItems.BAZOOKA.get());
                        output.accept(ModItems.BEDROCK_BAZOOKA.get());
                        output.accept(ModItems.GRENADE_LAUNCHER.get());
                        output.accept(ModItems.BEDROCK_GRENADE_LAUNCHER.get());
                        output.accept(ModItems.WATER_FLOOD_TNT.get());
                        output.accept(ModItems.BEDROCK_WATER_FLOOD_TNT.get());
                        ModItems.COUCHES.values().forEach(item -> output.accept(item.get()));
                        ModItems.ARMCHAIRS.values().forEach(item -> output.accept(item.get()));
                        ModItems.TRAMPOLINES.values().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.WALL_TV.get());
                        output.accept(ModItems.PORTABLE_LASER.get());
                        output.accept(ModItems.AUTO_TURRET.get());
                        output.accept(ModItems.KITCHEN_COUNTER.get());
                        output.accept(ModItems.KITCHEN_SINK.get());
                        output.accept(ModItems.KITCHEN_CABINET.get());
                        output.accept(ModItems.KITCHEN_TOWEL.get());
                        output.accept(ModItems.KITCHEN_OVEN.get());
                        output.accept(ModItems.BATHROOM_TOILET.get());
                        output.accept(ModItems.WATER_SLIDE_SURFACE.get());
                        output.accept(InstructionBook.create());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
