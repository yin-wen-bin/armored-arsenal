package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.server.BallisticLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class BallisticWeaponItem extends Item {
    private final BallisticProfile profile;

    public BallisticWeaponItem(Properties properties, BallisticProfile profile) {
        super(properties);
        this.profile = profile;
    }

    public BallisticProfile profile() {
        return profile;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (profile == BallisticProfile.RIFLE) {
                ItemStack weapon = player.getItemInHand(hand);
                if (player.isCrouching()) {
                    ManualReload.reload(serverPlayer, weapon, Items.IRON_NUGGET, "iron nugget");
                    return InteractionResult.SUCCESS_SERVER;
                }
                if (!ManualReload.requireLoaded(serverPlayer, weapon, "Rifle")) {
                    return InteractionResult.FAIL;
                }
            }
            BallisticLogic.fireWeapon(serverPlayer, hand, profile);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
