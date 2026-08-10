package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.server.RocketLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class LauncherWeaponItem extends Item {
    private final LauncherProfile profile;

    public LauncherWeaponItem(Properties properties, LauncherProfile profile) {
        super(properties);
        this.profile = profile;
    }

    public LauncherProfile profile() {
        return profile;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RocketLogic.launchWeaponFromPlayer(serverPlayer, hand, profile);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
