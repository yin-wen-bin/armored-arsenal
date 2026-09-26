package com.ethan.armoredarsenal.server;

import com.ethan.armoredarsenal.server.menu.CreativeSupplyMenu;
import com.ethan.armoredarsenal.server.menu.GunSelectorMenu;
import com.ethan.armoredarsenal.server.menu.SuitSelectorMenu;
import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BaseRailBlock;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

public final class ArmoredCommands {
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        PublicGameModeCommand.register(dispatcher);

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
        ColossusGolemHandler.registerCommands(dispatcher);
        TransformationHandler.registerCommands(dispatcher);
        WaterSlideTools.registerCommands(dispatcher);
        ThrowEnchantHandler.registerCommands(dispatcher);
        WitherStormHandler.registerCommands(dispatcher);
        dispatcher.register(Commands.literal("rollercoaster")
                .executes(context -> teleportToRollercoaster(context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("clearmobs")
                .executes(context -> clearMobs(context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("arsenal")
                .executes(context -> ArsenalDimensionHandler.toggleDimension(
                        context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("arsenaldimension")
                .executes(context -> ArsenalDimensionHandler.toggleDimension(
                        context.getSource().getPlayerOrException())));
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
            case "witherstorm" -> {
                event.setCanceled(true);
                WitherStormHandler.spawn(event.getPlayer());
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

    private static int teleportToRollercoaster(ServerPlayer player) {
        int radius = 64;
        int minY = Math.max(player.level().getMinY(), player.blockPosition().getY() - 12);
        int maxY = Math.min(player.level().getMaxY(), player.blockPosition().getY() + 12);
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        BlockPos origin = player.blockPosition();

        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!BaseRailBlock.isRail(player.level().getBlockState(pos))) {
                        continue;
                    }
                    int neighbors = railNeighbors(player, pos);
                    int distance = pos.distManhattan(origin);
                    int score = neighbors * 100_000 - distance;
                    if (score > bestScore) {
                        bestScore = score;
                        best = pos.immutable();
                    }
                }
            }
        }

        if (best == null) {
            player.sendSystemMessage(Component.literal("I could not find the rollercoaster nearby."), false);
            return 0;
        }

        player.teleportTo(best.getX() + 0.5, best.getY() + 1.2, best.getZ() + 0.5);
        player.sendSystemMessage(Component.literal("Teleported to the rollercoaster at "
                + best.getX() + ", " + best.getY() + ", " + best.getZ() + "."), false);
        return 1;
    }

    private static int clearMobs(ServerPlayer player) {
        int removed = 0;
        for (var level : player.level().getServer().getAllLevels()) {
            ArrayList<Mob> mobs = new ArrayList<>();
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof Mob mob) {
                    mobs.add(mob);
                }
            });
            mobs.forEach(Mob::discard);
            removed += mobs.size();
        }
        player.sendSystemMessage(Component.literal("Removed " + removed + " mobs from the loaded world."), false);
        return removed;
    }

    private static int railNeighbors(ServerPlayer player, BlockPos center) {
        int neighbors = 0;
        for (BlockPos offset : new BlockPos[] {
                center.north(), center.south(), center.east(), center.west(),
                center.above(), center.below() }) {
            if (BaseRailBlock.isRail(player.level().getBlockState(offset))) {
                neighbors++;
            }
        }
        return neighbors;
    }

    private ArmoredCommands() {}
}


