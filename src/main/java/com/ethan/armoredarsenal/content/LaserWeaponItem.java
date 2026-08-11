package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.server.LaserLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class LaserWeaponItem extends Item {
    private final WeaponProfile profile;

    public LaserWeaponItem(Properties properties, WeaponProfile profile) {
        super(properties);
        this.profile = profile;
    }

    public WeaponProfile profile() {
        return profile;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            if (profile == WeaponProfile.LASER_RIFLE) {
                ItemStack weapon = player.getItemInHand(hand);
                if (player.isCrouching()) {
                    ManualReload.reload(serverPlayer, weapon, Items.REDSTONE, "redstone dust");
                    return InteractionResult.SUCCESS_SERVER;
                }
                if (!ManualReload.requireLoaded(serverPlayer, weapon, "Laser Rifle")) {
                    return InteractionResult.FAIL;
                }
            }
            LaserLogic.fireWeapon(serverPlayer, profile);
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
