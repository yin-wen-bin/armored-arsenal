package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.server.menu.CreativeSupplyMenu;
import com.ethan.armoredarsenal.server.menu.GunSelectorMenu;
import com.ethan.armoredarsenal.server.menu.SuitSelectorMenu;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

public final class ArmoredCommands {
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("suits").executes(context -> {
            openSuitMenu(context.getSource().getPlayerOrException());
            return 1;
        }));

        dispatcher.register(Commands.literal("guns").executes(context -> {
            openGunMenu(context.getSource().getPlayerOrException());
            return 1;
        }));

        dispatcher.register(Commands.literal("items").executes(context -> {
            openSupplyMenu(context.getSource().getPlayerOrException());
            return 1;
        }));

        dispatcher.register(Commands.literal("manual").executes(context -> {
            MenuActions.giveInstructionBook(context.getSource().getPlayerOrException());
            return 1;
        }));

        dispatcher.register(Commands.literal("guide").executes(context -> {
            MenuActions.giveInstructionBook(context.getSource().getPlayerOrException());
            return 1;
        }));

        WorldEditTools.registerCommands(dispatcher);
        TransformationHandler.registerCommands(dispatcher);
    }

    public static void handlePlainChatCommands(ServerChatEvent event) {
        String raw = event.getRawText().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        switch (raw) {
            case "suits" -> {
                event.setCanceled(true);
                openSuitMenu(event.getPlayer());
            }
            case "guns" -> {
                event.setCanceled(true);
                openGunMenu(event.getPlayer());
            }
            case "items" -> {
                event.setCanceled(true);
                openSupplyMenu(event.getPlayer());
            }
            case "manual", "guide", "book" -> {
                event.setCanceled(true);
                MenuActions.giveInstructionBook(event.getPlayer());
            }
            case "give ethan0315 bedrock" -> {
                event.setCanceled(true);
                MenuActions.giveBedrockStacks(event.getPlayer());
            }
            default -> {
            }
        }
    }

    public static void openSuitMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new SuitSelectorMenu(containerId),
                Component.translatable("menu.armoredarsenal.suits")));
    }

    public static void openGunMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new GunSelectorMenu(containerId),
                Component.translatable("menu.armoredarsenal.guns")));
    }

    public static void openSupplyMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new CreativeSupplyMenu(containerId),
                Component.translatable("menu.armoredarsenal.items")));
    }

    private ArmoredCommands() {}
}


