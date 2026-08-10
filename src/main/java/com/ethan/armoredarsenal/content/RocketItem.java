package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.server.RocketLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class RocketItem extends Item {
    private final RocketProfile profile;

    public RocketItem(Properties properties, RocketProfile profile) {
        super(properties);
        this.profile = profile;
    }

    public RocketProfile profile() {
        return profile;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RocketLogic.launchFromPlayer(serverPlayer, hand, profile);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
