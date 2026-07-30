package com.ethan.armoredarsenal.content;

import com.ethan.armoredarsenal.ArmoredArsenal;
import java.util.EnumMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ArmoredArmorMaterials {
    public static final ResourceKey<EquipmentAsset> MARK_15_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, ArmoredArsenal.id("mark_15"));

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

    private ArmoredArmorMaterials() {}
}
