/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.oldtest.chat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.oldtest.client.TestScreen;

@Mod("client_command_test")
public class ClientCommandTest {
    public ClientCommandTest() {
        NeoForge.EVENT_BUS.addListener(this::init);
    }

    private void init(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("clientcommandtest")
                        // Used for checking suggestion providers that aren't registered
                        .then(Commands.literal("rawsuggest")
                                .then(Commands.argument("block", ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.BLOCK.keySet(), b))
                                        .executes(this::testCommand)))
                        // Used for checking suggestion providers that are registered
                        .then(Commands.literal("registeredsuggest").then(
                                Commands.argument("block", ResourceLocationArgument.id())
                                        .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                                        .executes(this::testCommand)))
                        // Used for checking if attempting to get the server on the client side errors
                        .then(Commands.literal("server")
                                .executes((context) -> {
                                    context.getSource().getServer();
                                    context.getSource().sendSuccess(() -> Component.literal("Successfully called getServer should have errored"), false);
                                    return 1;
                                }))
                        // Used for checking if attempting to get the server level on the client side errors
                        .then(Commands.literal("level")
                                .executes((context) -> {
                                    context.getSource().getLevel();
                                    context.getSource().sendSuccess(() -> Component.literal("Successfully called getLevel should have errored"), false);
                                    return 1;
                                }))
                        // Used for checking if getting a known objective argument works on the client side
                        .then(Commands.literal("get_objective")
                                .then(Commands.argument("objective", ObjectiveArgument.objective())
                                        .executes((context) -> {
                                            final Component msg = Component.literal("Regular: ")
                                                    .append(ObjectiveArgument.getObjective(context, "objective").getFormattedDisplayName());
                                            context.getSource().sendSuccess(() -> msg, false);
                                            return 1;
                                        })))
                        // 1.21.2 commands now directly use the registry for advancements and recipes
                        // where advancements and recipes are not synced
//                        // Used for checking if getting a known advancement works on the client side
//                        .then(Commands.literal("get_advancement")
//                                .then(Commands.argument("advancement", ResourceLocationArgument.id())
//                                        .executes((context) -> {
//                                            final Component msg = ResourceLocationArgument.getAdvancement(context, "advancement").value().display().get().getTitle();
//                                            context.getSource().sendSuccess(() -> msg, false);
//                                            return 1;
//                                        })))
//                        // Used for checking if getting a known recipe works on the client side
//                        .then(Commands.literal("get_recipe")
//                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
//                                        .executes((context) -> {
//                                            final Component msg = ResourceLocationArgument.getRecipe(context, "recipe").value().getResultItem(context.getSource().registryAccess()).getDisplayName();
//                                            context.getSource()
//                                                    .sendSuccess(() -> msg, false);
//                                            return 1;
//                                        })))
                        // Used for checking if getting a team works on the client side
                        .then(Commands.literal("get_team")
                                .then(Commands.argument("team", TeamArgument.team())
                                        .executes((context) -> {
                                            final Component msg = TeamArgument.getTeam(context, "team").getFormattedDisplayName();
                                            context.getSource().sendSuccess(() -> msg, false);
                                            return 1;
                                        })))
                        // Used for checking if a block position is valid works on the client side
                        .then(Commands.literal("get_loaded_blockpos")
                                .then(Commands.argument("blockpos", BlockPosArgument.blockPos())
                                        .executes((context) -> {
                                            final Component msg = Component.literal(BlockPosArgument.getLoadedBlockPos(context, "blockpos").toString());
                                            context.getSource()
                                                    .sendSuccess(() -> msg, false);
                                            return 1;
                                        })))
                        // Used for checking if a command can have a requirement
                        .then(Commands.literal("requires")
                                .requires((source) -> false)
                                .executes((context) -> {
                                    context.getSource().sendSuccess(() -> Component.literal("Executed command"), false);
                                    return 1;
                                }))
                        // Used for testing the screen after using commands
                        .then(Commands.literal("screentest")
                                .executes((stack) -> TestScreen.open())));

        // Used for testing that client command redirects can only be used with client commands
        LiteralArgumentBuilder<CommandSourceStack> fork = Commands.literal("clientcommandfork");
        fork.fork(event.getDispatcher().getRoot(), (context) -> List.of(context.getSource(), context.getSource()))
                .executes((context) -> {
                    context.getSource().sendSuccess(() -> Component.literal("Executing forked command"), false);
                    return 1;
                });
        event.getDispatcher().register(fork);
    }

    private int testCommand(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Input: " + ResourceLocationArgument.getId(context, "block")), false);
        context.getSource().sendSuccess(() -> Component.literal("Teams: " + context.getSource().getAllTeams()), false);
        context.getSource().sendSuccess(() -> Component.literal("Players: " + context.getSource().getOnlinePlayerNames()), false);
//        context.getSource().sendSuccess(() -> Component.literal("First recipe: " + context.getSource().getRecipeNames().findFirst().get()), false);
        context.getSource().sendSuccess(() -> Component.literal("Levels: " + context.getSource().levels()), false);
        context.getSource().sendSuccess(() -> Component.literal("Registry Access: " + context.getSource().registryAccess()), false);
        return 0;
    }
}
