package com.ethan.armoredarsenal.content;

import net.minecraft.network.chat.Component;

public record RocketProfile(
        String id,
        Component displayName,
        float explosionPower,
        int cooldownTicks,
        float speed,
        double range) {
    public static final RocketProfile STINGER = new RocketProfile(
            "stinger_rocket", Component.translatable("item.armoredarsenal.stinger_rocket"),
            2.0F, 18, 1.8F, 40.0D);
    public static final RocketProfile SIEGEBREAKER = new RocketProfile(
            "siegebreaker_rocket", Component.translatable("item.armoredarsenal.siegebreaker_rocket"),
            2.5F, 30, 1.55F, 48.0D);
    public static final RocketProfile TITAN = new RocketProfile(
            "titan_rocket", Component.translatable("item.armoredarsenal.titan_rocket"),
            3.0F, 50, 1.3F, 56.0D);
}
