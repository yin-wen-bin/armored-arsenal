package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

public final class PublicGameModeCommand {
    public static <S> void register(CommandDispatcher<S> dispatcher) {
        CommandNode<S> original = dispatcher.getRoot().getChild("gamemode");
        if (original == null) {
            return;
        }

        CommandNode<S> replacement = original.createBuilder().requires(source -> true).build();
        for (CommandNode<S> mode : original.getChildren()) {
            CommandNode<S> publicMode = mode.createBuilder().build();
            for (CommandNode<S> target : mode.getChildren()) {
                CommandNode<S> restrictedTarget = target.createBuilder()
                        .requires(original.getRequirement().and(target.getRequirement())).build();
                target.getChildren().forEach(restrictedTarget::addChild);
                publicMode.addChild(restrictedTarget);
            }
            replacement.addChild(publicMode);
        }

        // Brigadier merges existing nodes without replacing their permission predicate.
        dispatcher.getRoot().getChildren().remove(original);
        dispatcher.getRoot().addChild(replacement);
    }

    private PublicGameModeCommand() {}
}
