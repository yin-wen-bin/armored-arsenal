package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.ArmoredArsenal;
import java.util.EnumMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ArmoredArmorMaterials {
    public static final TagKey<Item> REPAIRS_ARCANITE = TagKey.create(
            Registries.ITEM, ArmoredArsenal.id("repairs_arcanite_equipment"));
    public static final TagKey<Block> INCORRECT_FOR_ARCANITE = TagKey.create(
            Registries.BLOCK, ArmoredArsenal.id("incorrect_for_arcanite_tool"));
    public static final ToolMaterial ARCANITE_TOOL = new ToolMaterial(
            INCORRECT_FOR_ARCANITE, 2450, 10.0F, 4.5F, 20, REPAIRS_ARCANITE);
    public static final ResourceKey<EquipmentAsset> MARK_15_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, ArmoredArsenal.id("mark_15"));

    public static final ResourceKey<EquipmentAsset> INFINITY_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, ArmoredArsenal.id("infinity"));

    public static final ArmorMaterial MARK_15 = new ArmorMaterial(
            40,
            Util.make(new EnumMap<>(ArmorType.class), defense -> {
                defense.put(ArmorType.BOOTS, 4);
                defense.put(ArmorType.LEGGINGS, 7);
                defense.put(ArmorType.CHESTPLATE, 9);
                defense.put(ArmorType.HELMET, 4);
                defense.put(ArmorType.BODY, 6);
            }),
            24,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.5F,
            0.18F,
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            MARK_15_ASSET);

    public static final ArmorMaterial INFINITY = new ArmorMaterial(
            48,
            Util.make(new EnumMap<>(ArmorType.class), defense -> {
                defense.put(ArmorType.BOOTS, 5);
                defense.put(ArmorType.LEGGINGS, 8);
                defense.put(ArmorType.CHESTPLATE, 10);
                defense.put(ArmorType.HELMET, 5);
                defense.put(ArmorType.BODY, 8);
            }),
            30,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.22F,
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            INFINITY_ASSET);

    public static final ResourceKey<EquipmentAsset> ARCANITE_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, ArmoredArsenal.id("arcanite"));
    public static final ArmorMaterial ARCANITE = new ArmorMaterial(
            42,
            Util.make(new EnumMap<>(ArmorType.class), defense -> {
                defense.put(ArmorType.BOOTS, 4);
                defense.put(ArmorType.LEGGINGS, 7);
                defense.put(ArmorType.CHESTPLATE, 9);
                defense.put(ArmorType.HELMET, 4);
                defense.put(ArmorType.BODY, 7);
            }),
            20,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            3.0F,
            0.12F,
            REPAIRS_ARCANITE,
            ARCANITE_ASSET);

    private ArmoredArmorMaterials() {}
}
