package com.ethan.armoredarsenal.registry;
import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.ArmoredArmorMaterials;
import com.ethan.armoredarsenal.content.BallisticProfile;
import com.ethan.armoredarsenal.content.BallisticWeaponItem;
import com.ethan.armoredarsenal.content.LauncherProfile;
import com.ethan.armoredarsenal.content.LauncherWeaponItem;
import com.ethan.armoredarsenal.content.LaserWeaponItem;
import com.ethan.armoredarsenal.content.RocketItem;
import com.ethan.armoredarsenal.content.RocketProfile;
import com.ethan.armoredarsenal.content.WeaponProfile;
import com.ethan.armoredarsenal.content.WaterFloodTntItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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
    public static final DeferredItem<Item> INFINITY_HELMET = ITEMS.registerItem(
            "infinity_helmet",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.INFINITY, ArmorType.HELMET).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFINITY_CHESTPLATE = ITEMS.registerItem(
            "infinity_chestplate",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.INFINITY, ArmorType.CHESTPLATE).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFINITY_LEGGINGS = ITEMS.registerItem(
            "infinity_leggings",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.INFINITY, ArmorType.LEGGINGS).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFINITY_BOOTS = ITEMS.registerItem(
            "infinity_boots",
            properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.INFINITY, ArmorType.BOOTS).rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> ARCANITE_ORE =
            ITEMS.registerSimpleBlockItem("arcanite_ore", ModBlocks.ARCANITE_ORE);
    public static final DeferredItem<Item> ARCANITE = ITEMS.registerSimpleItem("arcanite");
    public static final DeferredItem<Item> WITHERED_WITHER_STAR = ITEMS.registerItem(
            "withered_wither_star", properties -> new Item(properties.rarity(Rarity.EPIC).stacksTo(16)));
    public static final DeferredItem<Item> COMMAND_BLOCK_AXE = ITEMS.registerItem(
            "command_block_axe", properties -> new Item(properties.axe(
                    ArmoredArmorMaterials.ARCANITE_TOOL, 8.0F, -2.8F).rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> WITHERED_BEACON =
            ITEMS.registerSimpleBlockItem("withered_beacon", ModBlocks.WITHERED_BEACON);
    public static final DeferredItem<Item> ARCANITE_SWORD = ITEMS.registerItem(
            "arcanite_sword", properties -> new Item(properties.sword(ArmoredArmorMaterials.ARCANITE_TOOL, 3.5F, -2.3F)));
    public static final DeferredItem<Item> ARCANITE_PICKAXE = ITEMS.registerItem(
            "arcanite_pickaxe", properties -> new Item(properties.pickaxe(ArmoredArmorMaterials.ARCANITE_TOOL, 1.5F, -2.7F)));
    public static final DeferredItem<Item> ARCANITE_AXE = ITEMS.registerItem(
            "arcanite_axe", properties -> new Item(properties.axe(ArmoredArmorMaterials.ARCANITE_TOOL, 5.5F, -3.0F)));
    public static final DeferredItem<Item> ARCANITE_SHOVEL = ITEMS.registerItem(
            "arcanite_shovel", properties -> new Item(properties.shovel(ArmoredArmorMaterials.ARCANITE_TOOL, 1.5F, -3.0F)));
    public static final DeferredItem<Item> ARCANITE_HOE = ITEMS.registerItem(
            "arcanite_hoe", properties -> new Item(properties.hoe(ArmoredArmorMaterials.ARCANITE_TOOL, -4.0F, 0.0F)));
    public static final DeferredItem<Item> ARCANITE_HELMET = ITEMS.registerItem(
            "arcanite_helmet", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.HELMET)));
    public static final DeferredItem<Item> ARCANITE_CHESTPLATE = ITEMS.registerItem(
            "arcanite_chestplate", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> ARCANITE_LEGGINGS = ITEMS.registerItem(
            "arcanite_leggings", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> ARCANITE_BOOTS = ITEMS.registerItem(
            "arcanite_boots", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.BOOTS)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE = ITEMS.registerSimpleItem("bedrock_arcanite");
    public static final DeferredItem<Item> BEDROCK_ARCANITE_SWORD = ITEMS.registerItem(
            "bedrock_arcanite_sword", properties -> new Item(properties.sword(ArmoredArmorMaterials.ARCANITE_TOOL, 4.0F, -2.0F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_PICKAXE = ITEMS.registerItem(
            "bedrock_arcanite_pickaxe", properties -> new Item(properties.pickaxe(ArmoredArmorMaterials.ARCANITE_TOOL, 2.0F, -2.5F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_AXE = ITEMS.registerItem(
            "bedrock_arcanite_axe", properties -> new Item(properties.axe(ArmoredArmorMaterials.ARCANITE_TOOL, 6.0F, -2.8F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_SHOVEL = ITEMS.registerItem(
            "bedrock_arcanite_shovel", properties -> new Item(properties.shovel(ArmoredArmorMaterials.ARCANITE_TOOL, 2.0F, -2.8F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_HOE = ITEMS.registerItem(
            "bedrock_arcanite_hoe", properties -> new Item(properties.hoe(ArmoredArmorMaterials.ARCANITE_TOOL, -3.5F, -0.2F).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_HELMET = ITEMS.registerItem(
            "bedrock_arcanite_helmet", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.HELMET).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_CHESTPLATE = ITEMS.registerItem(
            "bedrock_arcanite_chestplate", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_LEGGINGS = ITEMS.registerItem(
            "bedrock_arcanite_leggings", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BEDROCK_ARCANITE_BOOTS = ITEMS.registerItem(
            "bedrock_arcanite_boots", properties -> new Item(properties.humanoidArmor(ArmoredArmorMaterials.ARCANITE, ArmorType.BOOTS).rarity(Rarity.UNCOMMON)));
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
    public static final DeferredItem<Item> STINGER_ROCKET = ITEMS.registerItem(
            "stinger_rocket", properties -> new RocketItem(properties.stacksTo(16), RocketProfile.STINGER));
    public static final DeferredItem<Item> SIEGEBREAKER_ROCKET = ITEMS.registerItem(
            "siegebreaker_rocket", properties -> new RocketItem(properties.stacksTo(16), RocketProfile.SIEGEBREAKER));
    public static final DeferredItem<Item> TITAN_ROCKET = ITEMS.registerItem(
            "titan_rocket", properties -> new RocketItem(properties.stacksTo(16), RocketProfile.TITAN));
    public static final DeferredItem<Item> RIFLE = ITEMS.registerItem(
            "rifle", properties -> new BallisticWeaponItem(properties.stacksTo(1).durability(720), BallisticProfile.RIFLE));
    public static final DeferredItem<Item> MINIGUN = ITEMS.registerItem(
            "minigun", properties -> new BallisticWeaponItem(properties.stacksTo(1).durability(1200), BallisticProfile.MINIGUN));
    public static final DeferredItem<Item> BAZOOKA = ITEMS.registerItem(
            "bazooka", properties -> new LauncherWeaponItem(properties.stacksTo(1).durability(500), LauncherProfile.BAZOOKA));
    public static final DeferredItem<Item> GRENADE_LAUNCHER = ITEMS.registerItem(
            "grenade_launcher", properties -> new LauncherWeaponItem(properties.stacksTo(1).durability(640), LauncherProfile.GRENADE_LAUNCHER));
    public static final DeferredItem<Item> WATER_FLOOD_TNT = ITEMS.registerItem(
            "water_flood_tnt", properties -> new WaterFloodTntItem(properties.stacksTo(16)));
    public static final DeferredItem<Item> BEDROCK_LASER_RIFLE = ITEMS.registerItem(
            "bedrock_laser_rifle", properties -> new LaserWeaponItem(properties.stacksTo(1).durability(700).rarity(Rarity.UNCOMMON), WeaponProfile.LASER_RIFLE));
    public static final DeferredItem<Item> BEDROCK_PULSE_PISTOL = ITEMS.registerItem(
            "bedrock_pulse_pistol", properties -> new LaserWeaponItem(properties.stacksTo(1).durability(500).rarity(Rarity.UNCOMMON), WeaponProfile.PULSE_PISTOL));
    public static final DeferredItem<Item> BEDROCK_BEAM_CANNON = ITEMS.registerItem(
            "bedrock_beam_cannon", properties -> new LaserWeaponItem(properties.stacksTo(1).durability(980).rarity(Rarity.UNCOMMON), WeaponProfile.BEAM_CANNON));
    public static final DeferredItem<Item> BEDROCK_CHARGED_SNIPER_LASER = ITEMS.registerItem(
            "bedrock_charged_sniper_laser",
            properties -> new LaserWeaponItem(properties.stacksTo(1).durability(790).rarity(Rarity.UNCOMMON), WeaponProfile.CHARGED_SNIPER));
    public static final DeferredItem<Item> BEDROCK_STINGER_ROCKET = ITEMS.registerItem(
            "bedrock_stinger_rocket", properties -> new RocketItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON), RocketProfile.STINGER));
    public static final DeferredItem<Item> BEDROCK_SIEGEBREAKER_ROCKET = ITEMS.registerItem(
            "bedrock_siegebreaker_rocket", properties -> new RocketItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON), RocketProfile.SIEGEBREAKER));
    public static final DeferredItem<Item> BEDROCK_TITAN_ROCKET = ITEMS.registerItem(
            "bedrock_titan_rocket", properties -> new RocketItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON), RocketProfile.TITAN));
    public static final DeferredItem<Item> BEDROCK_RIFLE = ITEMS.registerItem(
            "bedrock_rifle", properties -> new BallisticWeaponItem(properties.stacksTo(1).durability(800).rarity(Rarity.UNCOMMON), BallisticProfile.RIFLE));
    public static final DeferredItem<Item> BEDROCK_MINIGUN = ITEMS.registerItem(
            "bedrock_minigun", properties -> new BallisticWeaponItem(properties.stacksTo(1).durability(1400).rarity(Rarity.UNCOMMON), BallisticProfile.MINIGUN));
    public static final DeferredItem<Item> BEDROCK_BAZOOKA = ITEMS.registerItem(
            "bedrock_bazooka", properties -> new LauncherWeaponItem(properties.stacksTo(1).durability(620).rarity(Rarity.UNCOMMON), LauncherProfile.BAZOOKA));
    public static final DeferredItem<Item> BEDROCK_GRENADE_LAUNCHER = ITEMS.registerItem(
            "bedrock_grenade_launcher", properties -> new LauncherWeaponItem(properties.stacksTo(1).durability(740).rarity(Rarity.UNCOMMON), LauncherProfile.GRENADE_LAUNCHER));
    public static final DeferredItem<Item> BEDROCK_WATER_FLOOD_TNT = ITEMS.registerItem(
            "bedrock_water_flood_tnt", properties -> new WaterFloodTntItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> COUCH = ITEMS.registerSimpleBlockItem("couch", ModBlocks.COUCH);
    public static final DeferredItem<BlockItem> WALL_TV = ITEMS.registerSimpleBlockItem("wall_tv", ModBlocks.WALL_TV);
    public static final DeferredItem<BlockItem> PORTABLE_LASER =
            ITEMS.registerSimpleBlockItem("portable_laser", ModBlocks.PORTABLE_LASER);
    public static final DeferredItem<BlockItem> AUTO_TURRET =
            ITEMS.registerSimpleBlockItem("auto_turret", ModBlocks.AUTO_TURRET);
    public static final DeferredItem<BlockItem> KITCHEN_COUNTER =
            ITEMS.registerSimpleBlockItem("kitchen_counter", ModBlocks.KITCHEN_COUNTER);
    public static final DeferredItem<BlockItem> KITCHEN_SINK =
            ITEMS.registerSimpleBlockItem("kitchen_sink", ModBlocks.KITCHEN_SINK);
    public static final DeferredItem<BlockItem> KITCHEN_CABINET =
            ITEMS.registerSimpleBlockItem("kitchen_cabinet", ModBlocks.KITCHEN_CABINET);
    public static final DeferredItem<BlockItem> KITCHEN_TOWEL =
            ITEMS.registerSimpleBlockItem("kitchen_towel", ModBlocks.KITCHEN_TOWEL);
    public static final DeferredItem<BlockItem> KITCHEN_OVEN =
            ITEMS.registerSimpleBlockItem("kitchen_oven", ModBlocks.KITCHEN_OVEN);
    public static final DeferredItem<BlockItem> BATHROOM_TOILET =
            ITEMS.registerSimpleBlockItem("bathroom_toilet", ModBlocks.BATHROOM_TOILET);
    public static final DeferredItem<BlockItem> WATER_SLIDE_SURFACE =
            ITEMS.registerSimpleBlockItem("water_slide_surface", ModBlocks.WATER_SLIDE_SURFACE);
    public static final Map<String, DeferredItem<BlockItem>> COUCHES = registerCouches();
    public static final Map<String, DeferredItem<BlockItem>> ARMCHAIRS = registerArmchairs();
    public static final Map<String, DeferredItem<BlockItem>> TRAMPOLINES = registerTrampolines();
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
    private static Map<String, DeferredItem<BlockItem>> registerTrampolines() {
        Map<String, DeferredItem<BlockItem>> items = new LinkedHashMap<>();
        ModBlocks.TRAMPOLINES.forEach((color, block) ->
                items.put(color, ITEMS.registerSimpleBlockItem(color + "_trampoline", block)));
        return Collections.unmodifiableMap(items);
    }
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
    private ModItems() {}
}
