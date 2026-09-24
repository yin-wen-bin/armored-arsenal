package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public final class PublicGameModeCommandTest {
    private record Source(boolean operator, boolean acceptsTargets) {}

    public static void main(String[] args) throws Exception {
        CommandDispatcher<Source> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<Source>literal("gamemode")
                .requires(Source::operator)
                .then(RequiredArgumentBuilder.<Source, String>argument("gamemode", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String mode : new String[] {"survival", "creative", "adventure", "spectator"}) {
                                builder.suggest(mode);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> 1)
                        .then(RequiredArgumentBuilder.<Source, String>argument("target", StringArgumentType.word())
                                .requires(Source::acceptsTargets)
                                .executes(context -> 2))));
        dispatcher.register(LiteralArgumentBuilder.<Source>literal("effect")
                .requires(Source::operator).executes(context -> 3));

        Source player = new Source(false, true);
        Source operator = new Source(true, true);
        expectDenied(dispatcher, "gamemode creative", player);
        PublicGameModeCommand.register(dispatcher);

        for (String mode : new String[] {"survival", "creative", "adventure", "spectator"}) {
            check(dispatcher.execute("gamemode " + mode, player) == 1, "Self mode: " + mode);
            check(dispatcher.execute("gamemode " + mode + " david", operator) == 2, "Operator target: " + mode);
            expectDenied(dispatcher, "gamemode " + mode + " david", player);
            expectDenied(dispatcher, "gamemode " + mode + " @a", player);
        }
        expectDenied(dispatcher, "gamemode creative david", new Source(true, false));
        expectDenied(dispatcher, "effect", player);
        check(dispatcher.execute("effect", operator) == 3, "Other operator commands preserved");
        check(dispatcher.getCompletionSuggestions(dispatcher.parse("game", player)).get()
                .getList().stream().anyMatch(suggestion -> suggestion.getText().equals("gamemode")),
                "Command visible to non-operators");
        check(dispatcher.getCompletionSuggestions(dispatcher.parse("gamemode ", player)).get()
                .getList().size() == 4, "Mode suggestions preserved");
        check(!dispatcher.getRoot().getChild("gamemode").getChild("gamemode").getChild("target").canUse(player),
                "Target argument hidden from non-operators");

        PublicGameModeCommand.register(dispatcher);
        expectDenied(dispatcher, "gamemode creative david", player);
        check(dispatcher.execute("gamemode survival", player) == 1, "Repeated registration preserves access");
        System.out.println("Game mode permissions passed: all self modes, restricted targets, suggestions, and unrelated commands.");
    }

    private static void expectDenied(CommandDispatcher<Source> dispatcher, String command, Source source) throws Exception {
        try {
            dispatcher.execute(command, source);
            throw new AssertionError("Unexpected permission: " + command);
        } catch (CommandSyntaxException expected) {
            // Brigadier excludes command paths whose permission checks fail.
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) {
            throw new AssertionError(message);
        }
    }
}
