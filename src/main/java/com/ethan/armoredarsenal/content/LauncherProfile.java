package com.ethan.armoredarsenal.content;

import net.minecraft.network.chat.Component;

public record LauncherProfile(
        String id,
        Component displayName,
        RocketProfile projectile,
        int cooldownTicks,
        float speed,
        boolean arcing,
        double range) {
    public static final LauncherProfile BAZOOKA = new LauncherProfile(
            "bazooka", Component.translatable("item.armoredarsenal.bazooka"),
            RocketProfile.TITAN, 55, 1.3F, false, 56.0D);
    public static final LauncherProfile GRENADE_LAUNCHER = new LauncherProfile(
            "grenade_launcher", Component.translatable("item.armoredarsenal.grenade_launcher"),
            RocketProfile.SIEGEBREAKER, 30, 1.05F, true, 36.0D);
}
