package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.SittableFurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class CouchSittingHandler {
    private static final String SEAT_MARKER = "ArmoredArsenalCouchSeat";
    private static final String SEAT_POS = "ArmoredArsenalCouchPos";

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof SittableFurnitureBlock)) {
            return;
        }

        event.setCancellationResult(event.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || player.isPassenger()) {
            return;
        }

        BlockPos couchPos = event.getPos();
        Minecart seat = level.getEntitiesOfClass(Minecart.class, new AABB(couchPos).inflate(0.75),
                        cart -> cart.getPersistentData().getBooleanOr(SEAT_MARKER, false))
                .stream().findFirst().orElse(null);
        if (seat != null && !seat.getPassengers().isEmpty()) {
            player.sendSystemMessage(Component.literal("Someone is already sitting here."), true);
            return;
        }
        if (seat == null) {
            seat = EntityType.MINECART.create(level, EntitySpawnReason.TRIGGERED);
            if (seat == null) return;
            seat.getPersistentData().putBoolean(SEAT_MARKER, true);
            seat.getPersistentData().putLong(SEAT_POS, couchPos.asLong());
            seat.setInvisible(true);
            seat.setNoGravity(true);
            seat.setInvulnerable(true);
            seat.setSilent(true);
            seat.setPos(couchPos.getX() + 0.5, couchPos.getY() + 0.3, couchPos.getZ() + 0.5);
            level.addFreshEntity(seat);
        }

        player.setYRot(level.getBlockState(couchPos).getValue(HorizontalDirectionalBlock.FACING).toYRot());
        player.startRiding(seat, true, true);
    }

    public static void beforeEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Minecart seat)
                || !seat.getPersistentData().getBooleanOr(SEAT_MARKER, false)) {
            return;
        }
        BlockPos couchPos = BlockPos.of(seat.getPersistentData().getLongOr(SEAT_POS, seat.blockPosition().asLong()));
        if (!(seat.level().getBlockState(couchPos).getBlock() instanceof SittableFurnitureBlock)
                || seat.getPassengers().isEmpty()) {
            seat.discard();
            return;
        }
        seat.setDeltaMovement(0.0, 0.0, 0.0);
        seat.setPos(couchPos.getX() + 0.5, couchPos.getY() + 0.3, couchPos.getZ() + 0.5);
    }

    private CouchSittingHandler() {}
}