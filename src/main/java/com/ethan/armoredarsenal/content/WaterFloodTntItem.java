package com.ethan.armoredarsenal.content;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class WaterFloodTntItem extends Item {
    public static final String ENTITY_MARKER = "ArmoredArsenalWaterFloodTnt";

    public WaterFloodTntItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            Vec3 spawn = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
            PrimedTnt tnt = new PrimedTnt(level, spawn.x, spawn.y, spawn.z, context.getPlayer());
            tnt.getPersistentData().putBoolean(ENTITY_MARKER, true);
            level.addFreshEntity(tnt);
            level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}