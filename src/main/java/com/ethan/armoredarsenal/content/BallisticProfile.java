package com.ethan.armoredarsenal.content;

import net.minecraft.network.chat.Component;

public record BallisticProfile(
        String id,
        Component displayName,
        float damage,
        double range,
        int cooldownTicks,
        int projectiles,
        float spread,
        int tracerColor,
        float tracerWidth,
        float soundPitch) {
    public static final BallisticProfile RIFLE = new BallisticProfile(
            "rifle", Component.translatable("item.armoredarsenal.rifle"),
            10.0F, 58.0D, 12, 1, 0.0F, 0xEFFFF1B0, 0.035F, 1.0F);
    public static final BallisticProfile MINIGUN = new BallisticProfile(
            "minigun", Component.translatable("item.armoredarsenal.minigun"),
            3.5F, 42.0D, 8, 6, 0.045F, 0xEFFFF07A, 0.025F, 1.3F);
}
