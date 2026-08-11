package com.ethan.armoredarsenal.registry;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.ArmchairBlock;
import com.ethan.armoredarsenal.content.AutoTurretBlock;
import com.ethan.armoredarsenal.content.CouchBlock;
import com.ethan.armoredarsenal.content.PortableLaserBlock;
import com.ethan.armoredarsenal.content.WallTvBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
    public static final List<String> FURNITURE_COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArmoredArsenal.MOD_ID);

    public static final DeferredBlock<CouchBlock> COUCH = BLOCKS.registerBlock(
            "couch", CouchBlock::new,
            () -> BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<WallTvBlock> WALL_TV = BLOCKS.registerBlock(
            "wall_tv", WallTvBlock::new,
            () -> BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<PortableLaserBlock> PORTABLE_LASER = BLOCKS.registerBlock(
            "portable_laser", PortableLaserBlock::new,
            () -> BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 10));
    public static final DeferredBlock<AutoTurretBlock> AUTO_TURRET = BLOCKS.registerBlock(
            "auto_turret", AutoTurretBlock::new,
            () -> BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> ARCANITE_ORE = BLOCKS.registerBlock(
            "arcanite_ore", properties -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(4, 8), properties),
            () -> BlockBehaviour.Properties.of().strength(4.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final Map<String, DeferredBlock<CouchBlock>> COUCHES = registerCouches();
    public static final Map<String, DeferredBlock<ArmchairBlock>> ARMCHAIRS = registerArmchairs();

    private static Map<String, DeferredBlock<CouchBlock>> registerCouches() {
        Map<String, DeferredBlock<CouchBlock>> blocks = new LinkedHashMap<>();
        for (String color : FURNITURE_COLORS) {
            DeferredBlock<CouchBlock> block = color.equals("red") ? COUCH : BLOCKS.registerBlock(
                    color + "_couch", CouchBlock::new,
                    () -> BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOL).noOcclusion());
            blocks.put(color, block);
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<String, DeferredBlock<ArmchairBlock>> registerArmchairs() {
        Map<String, DeferredBlock<ArmchairBlock>> blocks = new LinkedHashMap<>();
        for (String color : FURNITURE_COLORS) {
            blocks.put(color, BLOCKS.registerBlock(
                    color + "_armchair", ArmchairBlock::new,
                    () -> BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOL).noOcclusion()));
        }
        return Collections.unmodifiableMap(blocks);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {}
}
