package com.ethan.armoredarsenal.registry;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.ArmoredArmorMaterials;
import com.ethan.armoredarsenal.content.LaserWeaponItem;
import com.ethan.armoredarsenal.content.WeaponProfile;
import com.ethan.armoredarsenal.content.WaterFloodTntItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArmoredArsenal.MOD_ID);

    public static final DeferredItem<Item> MARK_15_HELMET = ITEMS.registerItem(
            "mark_15_helmet",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.MARK_15, ArmorType.HELMET)));
    public static final DeferredItem<Item> MARK_15_CHESTPLATE = ITEMS.registerItem(
            "mark_15_chestplate",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.MARK_15, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> MARK_15_LEGGINGS = ITEMS.registerItem(
            "mark_15_leggings",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.MARK_15, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> MARK_15_BOOTS = ITEMS.registerItem(
            "mark_15_boots",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.MARK_15, ArmorType.BOOTS)));

    public static final DeferredItem<Item> LASER_RIFLE = ITEMS.registerItem(
            "laser_rifle",
            properties -> new LaserWeaponItem(properties.stacksTo(1).durability(640), WeaponProfile.LASER_RIFLE));
    public static final DeferredItem<Item> PULSE_PISTOL = ITEMS.registerItem(
            "pulse_pistol",
            properties -> new LaserWeaponItem(properties.stacksTo(1).durability(420), WeaponProfile.PULSE_PISTOL));
    public static final DeferredItem<Item> BEAM_CANNON = ITEMS.registerItem(
            "beam_cannon",
            properties -> new LaserWeaponItem(properties.stacksTo(1).durability(900), WeaponProfile.BEAM_CANNON));
    public static final DeferredItem<Item> CHARGED_SNIPER_LASER = ITEMS.registerItem(
            "charged_sniper_laser",
            properties -> new LaserWeaponItem(properties.stacksTo(1).durability(720), WeaponProfile.CHARGED_SNIPER));
    public static final DeferredItem<Item> WATER_FLOOD_TNT = ITEMS.registerItem(
            "water_flood_tnt", properties -> new WaterFloodTntItem(properties.stacksTo(16)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}

