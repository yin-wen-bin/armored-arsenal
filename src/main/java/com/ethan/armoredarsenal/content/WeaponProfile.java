package com.ethan.armoredarsenal.content;

import net.minecraft.network.chat.Component;

public record WeaponProfile(
        String id,
        Component displayName,
        float damage,
        double range,
        int cooldownTicks,
        int suitEnergyCost,
        float soundPitch) {
    public static final WeaponProfile LASER_RIFLE =
            new WeaponProfile("laser_rifle", Component.translatable("item.armoredarsenal.laser_rifle"), 9.0F, 46.0D, 14, 0, 1.15F);
    public static final WeaponProfile PULSE_PISTOL =
            new WeaponProfile("pulse_pistol", Component.translatable("item.armoredarsenal.pulse_pistol"), 5.0F, 28.0D, 8, 0, 1.45F);
    public static final WeaponProfile BEAM_CANNON =
            new WeaponProfile("beam_cannon", Component.translatable("item.armoredarsenal.beam_cannon"), 18.0F, 34.0D, 38, 0, 0.65F);
    public static final WeaponProfile CHARGED_SNIPER =
            new WeaponProfile("charged_sniper_laser", Component.translatable("item.armoredarsenal.charged_sniper_laser"), 22.0F, 80.0D, 50, 0, 0.9F);
    public static final WeaponProfile REPULSOR =
            new WeaponProfile("mark_15_repulsor", Component.translatable("power.armoredarsenal.repulsor"), 12.0F, 36.0D, 18, 10, 1.9F);
}

