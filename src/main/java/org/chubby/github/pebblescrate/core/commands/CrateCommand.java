package org.chubby.github.pebblescrate.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.chubby.github.pebblescrate.common.lootcrates.CrateConfigManager;
import org.chubby.github.pebblescrate.common.lootcrates.CrateTransformer;

import java.util.concurrent.CompletableFuture;

public class CrateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("padmin")
                .requires(source -> hasAdminPermission(source))
                .then(Commands.literal("crate")
                        .requires(source -> hasAdminPermission(source))
                        .executes(CrateCommand::openCrateUI)
                        .then(Commands.literal("getcrate")
                                .then(Commands.argument("crateName", StringArgumentType.greedyString())
                                        .suggests(CrateCommand::getCrateNameSuggestions)
                                        .executes(CrateCommand::getCrate)))
                        .then(Commands.literal("givekey")
                                .then(Commands.argument("player", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("crateName", StringArgumentType.greedyString())
                                                        .suggests(CrateCommand::getCrateNameSuggestions)
                                                        .executes(CrateCommand::giveCrateKey)))))
                        .then(Commands.literal("activecrates")
                                .executes(CrateCommand::openActiveCratesUI))
                        .then(Commands.literal("reload")
                                .executes(CrateCommand::reloadCrateConfigs))));
    }

    private static boolean hasAdminPermission(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return source.hasPermission(2) || (isLuckPermsPresent() &&
                    getLuckPermsApi().getUserManager().getUser(player.getUUID())
                            .getCachedData().getPermissionData().checkPermission("pebbles.admin.crate").asBoolean());
        }
        return source.getEntity() == null;
    }

    private static int openCrateUI(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(Component.literal("Opening Crate Management UI"));
        }
        return 1;
    }

    private static int openActiveCratesUI(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(Component.literal("Opening Active Crates UI"));
        }
        return 1;
    }

    private static int reloadCrateConfigs(CommandContext<CommandSourceStack> context) {
        new CrateConfigManager().loadCrateConfigs();
        context.getSource().sendSuccess(()->Component.literal("Reloaded crate configs"), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> getCrateNameSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        CrateConfigManager crateConfigManager = new CrateConfigManager();
        crateConfigManager.loadCrateConfigs().forEach(config -> builder.suggest(config.crateName()));
        return builder.buildFuture();
    }

    private static int getCrate(CommandContext<CommandSourceStack> context) {
        String crateName = StringArgumentType.getString(context, "crateName");
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            new CrateTransformer(crateName, player).giveTransformer();
        }
        return 1;
    }

    private static int giveCrateKey(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String crateName = StringArgumentType.getString(context, "crateName");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        for (ServerPlayer player : EntityArgument.getPlayers(context, "player")) {
            new CrateTransformer(crateName, player).giveKey(amount, player);
            context.getSource().sendSuccess(()->Component.literal(player.getName().getString() + " received " + amount + " " + crateName + " keys!"), true);
        }
        return 1;
    }

    private static boolean isLuckPermsPresent() {
        try {
            Class.forName("net.luckperms.api.LuckPerms");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static LuckPerms getLuckPermsApi() {
        return LuckPermsProvider.get();
    }
}
