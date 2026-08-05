package com.ethan.armoredarsenal.registry;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.CouchBlock;
import com.ethan.armoredarsenal.content.WallTvBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArmoredArsenal.MOD_ID);

    public static final DeferredBlock<CouchBlock> COUCH = BLOCKS.registerBlock(
            "couch", CouchBlock::new,
            () -> BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<WallTvBlock> WALL_TV = BLOCKS.registerBlock(
            "wall_tv", WallTvBlock::new,
            () -> BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.GLASS).noOcclusion());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {}
}