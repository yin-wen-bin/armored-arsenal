package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.content.InstructionBook;
import com.ethan.armoredarsenal.registry.ModItems;
import com.ethan.armoredarsenal.server.menu.CreativeSupplyMenu;
import com.ethan.armoredarsenal.server.menu.GunSelectorMenu;
import com.ethan.armoredarsenal.server.menu.SuitSelectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MenuActions {
    public static void handleSuitButton(ServerPlayer player, int id) {
        switch (id) {
            case SuitSelectorMenu.EQUIP_MARK_15 -> equipMark15(player);
            case SuitSelectorMenu.EQUIP_INFINITY -> equipInfinity(player);
            case SuitSelectorMenu.TOGGLE_HOVER -> SuitPowerHandler.toggleHover(player);
            case SuitSelectorMenu.FIRE_REPULSOR -> SuitPowerHandler.fireRepulsor(player);
            case SuitSelectorMenu.TOGGLE_STEALTH -> SuitPowerHandler.toggleStealth(player);
            case SuitSelectorMenu.REMOVE_SUIT -> removePoweredSuit(player);
            default -> player.sendSystemMessage(Component.literal("Unknown suit action."), false);
        }
    }

    public static void handleGunButton(ServerPlayer player, int id) {
        switch (id) {
            case GunSelectorMenu.GIVE_LASER_RIFLE -> give(player, ModItems.LASER_RIFLE.get());
            case GunSelectorMenu.GIVE_PULSE_PISTOL -> give(player, ModItems.PULSE_PISTOL.get());
            case GunSelectorMenu.GIVE_BEAM_CANNON -> give(player, ModItems.BEAM_CANNON.get());
            case GunSelectorMenu.GIVE_CHARGED_SNIPER -> give(player, ModItems.CHARGED_SNIPER_LASER.get());
            case GunSelectorMenu.GIVE_RIFLE -> give(player, ModItems.RIFLE.get());
            case GunSelectorMenu.GIVE_MINIGUN -> give(player, ModItems.MINIGUN.get());
            case GunSelectorMenu.GIVE_BAZOOKA -> give(player, ModItems.BAZOOKA.get());
            case GunSelectorMenu.GIVE_GRENADE_LAUNCHER -> give(player, ModItems.GRENADE_LAUNCHER.get());
            case GunSelectorMenu.GIVE_ALL -> {
                give(player, ModItems.LASER_RIFLE.get());
                give(player, ModItems.PULSE_PISTOL.get());
                give(player, ModItems.BEAM_CANNON.get());
                give(player, ModItems.CHARGED_SNIPER_LASER.get());
                give(player, ModItems.STINGER_ROCKET.get());
                give(player, ModItems.SIEGEBREAKER_ROCKET.get());
                give(player, ModItems.TITAN_ROCKET.get());
                give(player, ModItems.RIFLE.get());
                give(player, ModItems.MINIGUN.get());
                give(player, ModItems.BAZOOKA.get());
                give(player, ModItems.GRENADE_LAUNCHER.get());
            }
            default -> player.sendSystemMessage(Component.literal("Unknown weapon action."), false);
        }
    }

    public static void handleSupplyButton(ServerPlayer player, int id) {
        switch (id) {
            case CreativeSupplyMenu.BUILDING -> giveMany(player,
                    stack(Items.STONE, 64), stack(Items.OAK_PLANKS, 64), stack(Items.GLASS, 64),
                    stack(Items.TORCH, 64), stack(Items.LADDER, 64), stack(Items.WATER_BUCKET, 1));
            case CreativeSupplyMenu.VALUABLES -> giveMany(player,
                    stack(Items.DIAMOND_BLOCK, 64), stack(Items.EMERALD_BLOCK, 64), stack(Items.NETHERITE_BLOCK, 16),
                    stack(Items.IRON_BLOCK, 64), stack(Items.GOLD_BLOCK, 64), stack(Items.REDSTONE_BLOCK, 64));
            case CreativeSupplyMenu.REDSTONE -> giveMany(player,
                    stack(Items.REDSTONE, 64), stack(Items.REPEATER, 64), stack(Items.COMPARATOR, 64),
                    stack(Items.PISTON, 64), stack(Items.OBSERVER, 64), stack(Items.LEVER, 64));
            case CreativeSupplyMenu.SURVIVAL -> giveMany(player,
                    stack(Items.COOKED_BEEF, 64), stack(Items.GOLDEN_APPLE, 16), stack(Items.BOW, 1),
                    stack(Items.ARROW, 64), stack(Items.SHIELD, 1), stack(Items.ENDER_PEARL, 16),
                    InstructionBook.create());
            case CreativeSupplyMenu.MODDED -> {
                equipMark15(player);
                handleGunButton(player, GunSelectorMenu.GIVE_ALL);
                give(player, ModItems.WATER_FLOOD_TNT.get());
                give(player, ModItems.COUCH.get());
                give(player, ModItems.COUCHES.get("blue").get());
                give(player, ModItems.ARMCHAIRS.get("white").get());
                give(player, ModItems.ARMCHAIRS.get("black").get());
                give(player, ModItems.WALL_TV.get());
                give(player, ModItems.PORTABLE_LASER.get());
                give(player, ModItems.PORTABLE_LASER.get());
                give(player, ModItems.AUTO_TURRET.get());
                give(player, ModItems.KITCHEN_COUNTER.get());
                give(player, ModItems.KITCHEN_SINK.get());
                give(player, ModItems.KITCHEN_CABINET.get());
                give(player, ModItems.KITCHEN_TOWEL.get());
                give(player, ModItems.KITCHEN_OVEN.get());
                give(player, ModItems.BATHROOM_TOILET.get());
                giveMany(player,
                        stack(ModItems.ARCANITE.get(), 64),
                        stack(ModItems.ARCANITE_PICKAXE.get(), 1),
                        stack(ModItems.ARCANITE_SWORD.get(), 1));
                giveInstructionBook(player);
            }
            case CreativeSupplyMenu.SPAWN_EGGS -> giveMany(player,
                    stack(Items.ZOMBIE_SPAWN_EGG, 16), stack(Items.SKELETON_SPAWN_EGG, 16),
                    stack(Items.CREEPER_SPAWN_EGG, 16), stack(Items.ENDERMAN_SPAWN_EGG, 16));
            default -> player.sendSystemMessage(Component.literal("Unknown supply action."), false);
        }
    }

    public static void equipMark15(ServerPlayer player) {
        SuitPowerHandler.clearSuitState(player);
        equip(player, EquipmentSlot.HEAD, ModItems.MARK_15_HELMET.get());
        equip(player, EquipmentSlot.CHEST, ModItems.MARK_15_CHESTPLATE.get());
        equip(player, EquipmentSlot.LEGS, ModItems.MARK_15_LEGGINGS.get());
        equip(player, EquipmentSlot.FEET, ModItems.MARK_15_BOOTS.get());
        player.sendSystemMessage(Component.literal("Mark 15-style powered suit equipped."), false);
    }

    public static void equipInfinity(ServerPlayer player) {
        SuitPowerHandler.clearSuitState(player);
        equip(player, EquipmentSlot.HEAD, ModItems.INFINITY_HELMET.get());
        equip(player, EquipmentSlot.CHEST, ModItems.INFINITY_CHESTPLATE.get());
        equip(player, EquipmentSlot.LEGS, ModItems.INFINITY_LEGGINGS.get());
        equip(player, EquipmentSlot.FEET, ModItems.INFINITY_BOOTS.get());
        player.sendSystemMessage(Component.literal("Infinity Armor equipped. Cosmic shield ready."), false);
    }

    public static void giveInstructionBook(ServerPlayer player) {
        ItemStack book = InstructionBook.create();
        insertOrDrop(player, book);
        player.sendSystemMessage(Component.literal("Added ").append(book.getHoverName()), false);
    }

    public static void giveBedrockStacks(ServerPlayer player) {
        insertOrDrop(player, stack(Items.BEDROCK, 64));
        insertOrDrop(player, stack(Items.BEDROCK, 64));
        player.sendSystemMessage(Component.literal("Added 2 stacks of bedrock for ethan0315."), false);
    }

    private static void removePoweredSuit(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack equipped = player.getItemBySlot(slot);
            if (SuitPowerHandler.isPoweredArmorPiece(equipped)) {
                insertOrDrop(player, equipped.copy());
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        SuitPowerHandler.clearSuitState(player);
        player.sendSystemMessage(Component.literal("Powered suit removed."), false);
    }

    private static void equip(ServerPlayer player, EquipmentSlot slot, Item item) {
        ItemStack current = player.getItemBySlot(slot);
        if (!current.isEmpty()) {
            insertOrDrop(player, current.copy());
        }
        player.setItemSlot(slot, new ItemStack(item));
    }

    private static void give(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        insertOrDrop(player, stack);
        player.sendSystemMessage(Component.literal("Added ").append(stack.getHoverName()), false);
    }

    private static void giveMany(ServerPlayer player, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            insertOrDrop(player, stack);
        }
        player.sendSystemMessage(Component.literal("Supply kit added."), false);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static void insertOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private MenuActions() {}
}
