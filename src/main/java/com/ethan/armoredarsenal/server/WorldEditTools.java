package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class WorldEditTools {
    private static final int MAX_EDIT_BLOCKS = 65_536;
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();
    private static CommandDispatcher<CommandSourceStack> commandDispatcher;

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        commandDispatcher = dispatcher;
        dispatcher.register(command("worldedit"));
        dispatcher.register(command("we"));
        dispatcher.register(fillCommand("set", FillMode.SOLID));
        dispatcher.register(fillCommand("walls", FillMode.WALLS));
        dispatcher.register(GiantStructureTools.command());
        dispatcher.register(Commands.literal("wand").executes(context -> giveWand(context.getSource().getPlayerOrException())));
    }

    public static void command(CommandEvent event) {
        if (commandDispatcher == null) {
            return;
        }

        String raw = event.getParseResults().getReader().getString().trim();
        if (raw.equalsIgnoreCase("/giant") || raw.equalsIgnoreCase("//giant")) {
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            event.setParseResults(commandDispatcher.parse("giant", source));
            return;
        }

        String[] parts = raw.split("\\s+");
        if (parts.length == 2 && parts[0].equalsIgnoreCase("fill")) {
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            event.setParseResults(commandDispatcher.parse("we fill " + parts[1], source));
        }
    }

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !isWoodenAxe(event.getItemStack())) {
            return;
        }

        event.setCancellationResult(event.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);

        if (event.getEntity() instanceof ServerPlayer player) {
            Selection selection = selection(player);
            setCorner(player, event.getPos(), selection.nextRightClickCorner());
        }
    }

    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START || !isWoodenAxe(event.getItemStack())) {
            return;
        }

        event.setCanceled(true);

        if (event.getEntity() instanceof ServerPlayer player) {
            setCorner(player, event.getPos(), 1);
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .executes(context -> showHelp(context.getSource().getPlayerOrException()))
                .then(Commands.literal("set")
                        .executes(context -> showUsage(context.getSource().getPlayerOrException(), FillMode.SOLID))
                        .then(fillArgument(FillMode.SOLID)))
                .then(Commands.literal("fill")
                        .executes(context -> showUsage(context.getSource().getPlayerOrException(), FillMode.SOLID))
                        .then(fillArgument(FillMode.SOLID)))
                .then(Commands.literal("walls")
                        .executes(context -> showUsage(context.getSource().getPlayerOrException(), FillMode.WALLS))
                        .then(fillArgument(FillMode.WALLS)))
                .then(GiantStructureTools.command())
                .then(Commands.literal("clear").executes(context -> clearSelection(context.getSource().getPlayerOrException())))
                .then(Commands.literal("wand").executes(context -> giveWand(context.getSource().getPlayerOrException())))
                .then(Commands.literal("help").executes(context -> showHelp(context.getSource().getPlayerOrException())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> fillCommand(String name, FillMode mode) {
        return Commands.literal(name)
                .executes(context -> showUsage(context.getSource().getPlayerOrException(), mode))
                .then(fillArgument(mode));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> fillArgument(FillMode mode) {
        return Commands.argument("block", StringArgumentType.word())
                .executes(context -> fillSelection(
                        context.getSource().getPlayerOrException(),
                        StringArgumentType.getString(context, "block"),
                        mode));
    }

    private static int fillSelection(ServerPlayer player, String rawBlock, FillMode mode) {
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null || !selection.isComplete()) {
            player.sendSystemMessage(Component.literal("Select two corners with a wooden axe first."), false);
            return 0;
        }

        if (!selection.dimension().equals(player.level().dimension())) {
            player.sendSystemMessage(Component.literal("Your WorldEdit selection is in another dimension."), false);
            return 0;
        }

        BlockState blockState = parseBlockState(player, rawBlock);
        if (blockState == null) {
            return 0;
        }

        long editBlocks = selection.blockCount(mode);
        if (editBlocks > MAX_EDIT_BLOCKS) {
            player.sendSystemMessage(Component.literal(
                    "This edit would change up to " + editBlocks + " blocks. Limit is " + MAX_EDIT_BLOCKS + "."),
                    false);
            return 0;
        }

        int changed = applyFill(player.level(), selection, blockState, mode);
        String operation = mode == FillMode.WALLS ? "walls" : "fill";
        player.sendSystemMessage(Component.literal(
                "WorldEdit " + operation + " changed " + changed + " blocks to " + blockName(blockState) + "."),
                false);
        return changed;
    }

    private static int showHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "Wooden axe: right-click corner 1, then right-click corner 2. Use /we fill <block>, /walls <block>, or look at a structure and type //giant."),
                false);
        return 1;
    }

    private static int showUsage(ServerPlayer player, FillMode mode) {
        String command = mode == FillMode.WALLS ? "/walls <block>" : "/we fill <block>";
        player.sendSystemMessage(Component.literal(
                "Usage: " + command + ". First select two corners by right-clicking blocks with a wooden axe."),
                false);
        return 1;
    }

    private static int clearSelection(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("WorldEdit selection cleared."), false);
        return 1;
    }

    private static int giveWand(ServerPlayer player) {
        ItemStack axe = new ItemStack(Items.WOODEN_AXE);
        if (!player.getInventory().add(axe)) {
            player.drop(axe, false);
        }
        player.sendSystemMessage(Component.literal("WorldEdit wooden axe added."), false);
        return 1;
    }

    private static void setCorner(ServerPlayer player, BlockPos pos, int corner) {
        Selection selection = selection(player);
        selection.setCorner(player.level().dimension(), pos, corner);

        String label = corner == 1 ? "Position 1" : "Position 2";
        Component message = Component.literal("WorldEdit " + label + " set to " + formatPos(pos) + ".");
        if (selection.isComplete()) {
            message = message.copy().append(Component.literal(" Selection size: " + selection.volume() + " blocks."));
        } else {
            message = message.copy().append(Component.literal(" Select the other corner next."));
        }
        player.sendSystemMessage(message, false);
    }

    private static Selection selection(ServerPlayer player) {
        return SELECTIONS.computeIfAbsent(player.getUUID(), ignored -> new Selection());
    }

    private static BlockState parseBlockState(ServerPlayer player, String rawBlock) {
        String normalized = rawBlock.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        Block block = findBlock(normalized);
        if (block == null) {
            player.sendSystemMessage(Component.literal(
                    "Unknown block or material: " + rawBlock + ". Try a name like stone, oak_planks, diamond, or netherite."),
                    false);
            return null;
        }

        return block.defaultBlockState();
    }

    private static Block findBlock(String name) {
        String singular = name.endsWith("s") && name.length() > 1 ? name.substring(0, name.length() - 1) : name;
        String[] candidates = {
                name,
                withBlockSuffix(name),
                singular,
                withBlockSuffix(singular)
        };

        for (String candidate : candidates) {
            Identifier id = Identifier.tryParse(candidate);
            if (id == null) {
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block != null) {
                return block;
            }
        }

        return null;
    }

    private static String withBlockSuffix(String name) {
        int namespaceSeparator = name.indexOf(':');
        if (namespaceSeparator < 0) {
            return name.endsWith("_block") ? name : name + "_block";
        }

        String namespace = name.substring(0, namespaceSeparator + 1);
        String path = name.substring(namespaceSeparator + 1);
        return path.endsWith("_block") ? name : namespace + path + "_block";
    }

    private static int applyFill(Level level, Selection selection, BlockState blockState, FillMode mode) {
        int changed = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = selection.minX(); x <= selection.maxX(); x++) {
            for (int y = selection.minY(); y <= selection.maxY(); y++) {
                for (int z = selection.minZ(); z <= selection.maxZ(); z++) {
                    if (mode == FillMode.WALLS && x != selection.minX() && x != selection.maxX() && z != selection.minZ() && z != selection.maxZ()) {
                        continue;
                    }

                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).equals(blockState) && level.setBlock(cursor, blockState, 3)) {
                        changed++;
                    }
                }
            }
        }

        return changed;
    }

    private static boolean isWoodenAxe(ItemStack stack) {
        return stack.is(Items.WOODEN_AXE);
    }

    private static String blockName(BlockState blockState) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return id == null ? "unknown" : id.toShortString();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private enum FillMode {
        SOLID,
        WALLS
    }

    private static final class Selection {
        private ResourceKey<Level> dimension;
        private BlockPos first;
        private BlockPos second;
        private int nextRightClickCorner = 1;

        private void setCorner(ResourceKey<Level> dimension, BlockPos pos, int corner) {
            if (this.dimension == null || !this.dimension.equals(dimension)) {
                this.first = null;
                this.second = null;
                this.nextRightClickCorner = 1;
            }

            this.dimension = dimension;
            if (corner == 1) {
                this.first = pos.immutable();
                this.nextRightClickCorner = 2;
            } else {
                this.second = pos.immutable();
                this.nextRightClickCorner = 1;
            }
        }

        private int nextRightClickCorner() {
            if (first == null) {
                return 1;
            }
            if (second == null) {
                return 2;
            }
            return nextRightClickCorner;
        }

        private boolean isComplete() {
            return first != null && second != null;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private int minX() {
            return Math.min(first.getX(), second.getX());
        }

        private int maxX() {
            return Math.max(first.getX(), second.getX());
        }

        private int minY() {
            return Math.min(first.getY(), second.getY());
        }

        private int maxY() {
            return Math.max(first.getY(), second.getY());
        }

        private int minZ() {
            return Math.min(first.getZ(), second.getZ());
        }

        private int maxZ() {
            return Math.max(first.getZ(), second.getZ());
        }

        private long volume() {
            long dx = maxX() - minX() + 1L;
            long dy = maxY() - minY() + 1L;
            long dz = maxZ() - minZ() + 1L;
            return dx * dy * dz;
        }

        private long blockCount(FillMode mode) {
            if (mode == FillMode.SOLID) {
                return volume();
            }

            long dx = maxX() - minX() + 1L;
            long dy = maxY() - minY() + 1L;
            long dz = maxZ() - minZ() + 1L;
            long perimeter = dx == 1L || dz == 1L ? dx * dz : (2L * dx) + (2L * dz) - 4L;
            return perimeter * dy;
        }
    }

    private WorldEditTools() {}
}
