package com.ethan.armoredarsenal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class BuildableEndPortalHandler {
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(Items.ENDER_EYE)
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.OBSIDIAN)) {
            return;
        }

        event.setCancellationResult(event.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos center = findCenter(level, event.getPos());
        if (center == null) {
            player.sendSystemMessage(Component.literal(
                    "End portal needs a flat 5 x 5 obsidian ring or pad. Keep every outer block obsidian."), false);
            return;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(center.offset(x, 0, z), Blocks.END_PORTAL.defaultBlockState(), 3);
            }
        }

        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        level.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal("End portal activated. Enter it to reach the main End island."), false);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }

    private static BlockPos findCenter(ServerLevel level, BlockPos clicked) {
        for (int xOffset = -2; xOffset <= 2; xOffset++) {
            for (int zOffset = -2; zOffset <= 2; zOffset++) {
                BlockPos center = clicked.offset(xOffset, 0, zOffset);
                if (isCompleteRing(level, center)) {
                    return center;
                }
            }
        }
        return null;
    }

    private static boolean isCompleteRing(ServerLevel level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.offset(x, 0, z);
                boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
                if (edge && !level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                    return false;
                }
                if (!edge && !level.getBlockState(pos).isAir()
                        && !level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                    return false;
                }
            }
        }
        return true;
    }

    private BuildableEndPortalHandler() {}
}