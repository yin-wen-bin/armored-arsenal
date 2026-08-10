package com.ethan.armoredarsenal.registry;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.ArmoredArmorMaterials;
import com.ethan.armoredarsenal.content.LaserWeaponItem;
import com.ethan.armoredarsenal.content.WeaponProfile;
import com.ethan.armoredarsenal.content.WaterFloodTntItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public static final DeferredItem<BlockItem> COUCH = ITEMS.registerSimpleBlockItem("couch", ModBlocks.COUCH);
    public static final DeferredItem<BlockItem> WALL_TV = ITEMS.registerSimpleBlockItem("wall_tv", ModBlocks.WALL_TV);
    public static final DeferredItem<BlockItem> PORTABLE_LASER =
            ITEMS.registerSimpleBlockItem("portable_laser", ModBlocks.PORTABLE_LASER);
    public static final Map<String, DeferredItem<BlockItem>> COUCHES = registerCouches();
    public static final Map<String, DeferredItem<BlockItem>> ARMCHAIRS = registerArmchairs();

    private static Map<String, DeferredItem<BlockItem>> registerCouches() {
        Map<String, DeferredItem<BlockItem>> items = new LinkedHashMap<>();
        ModBlocks.COUCHES.forEach((color, block) -> items.put(color,
                color.equals("red") ? COUCH : ITEMS.registerSimpleBlockItem(color + "_couch", block)));
        return Collections.unmodifiableMap(items);
    }

    private static Map<String, DeferredItem<BlockItem>> registerArmchairs() {
        Map<String, DeferredItem<BlockItem>> items = new LinkedHashMap<>();
        ModBlocks.ARMCHAIRS.forEach((color, block) ->
                items.put(color, ITEMS.registerSimpleBlockItem(color + "_armchair", block)));
        return Collections.unmodifiableMap(items);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}