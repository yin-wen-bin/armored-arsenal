package com.ethan.armoredarsenal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HopperDripstoneHandler {
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.getFace() != Direction.UP
                || !event.getItemStack().is(Items.POINTED_DRIPSTONE)
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.HOPPER)) {
            return;
        }

        event.setCancellationResult(event.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos dripstonePos = event.getPos().above();
        if (!level.getBlockState(dripstonePos).canBeReplaced()) {
            return;
        }

        BlockState dripstone = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP)
                .setValue(PointedDripstoneBlock.WATERLOGGED, false);
        level.setBlock(dripstonePos, dripstone, 3);
        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
    }

    private HopperDripstoneHandler() {}
}
