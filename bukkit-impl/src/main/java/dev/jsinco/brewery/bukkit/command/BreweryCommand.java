package dev.jsinco.brewery.bukkit.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.command.argument.EventArgument;
import dev.jsinco.brewery.bukkit.command.argument.OfflinePlayerArgument;
import dev.jsinco.brewery.bukkit.command.argument.OfflinePlayerSelectorArgumentResolver;
import dev.jsinco.brewery.bukkit.util.BukkitMessageUtil;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.api.event.DrunkEvent;
import dev.jsinco.brewery.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public class BreweryCommand {

    private static final SimpleCommandExceptionType ERROR_UNDEFINED_PLAYER = new SimpleCommandExceptionType(
            BukkitMessageUtil.toBrigadier("tbp.command.undefined-player")
    );

    public static void register(ReloadableRegistrarEvent<Commands> commands) {
        commands.registrar().register(build(), Config.config().commandAliases());
    }

    public static LiteralCommandNode<CommandSourceStack> build() {
        ArgumentBuilder<CommandSourceStack, ?> eventCommand = Commands.argument("event-type", new EventArgument())
                .executes(context -> {
                    Player target = getPlayer(context);
                    DrunkEvent event = context.getArgument("event-type", DrunkEvent.class);
                    TheBrewingProject.getInstance().getDrunkEventExecutor().doDrunkEvent(target.getUniqueId(), event);
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                });
        return Commands.literal("tbp")
                .then(CreateCommand.command()
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.create")))
                .then(InfoCommand.command("info", false)
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.info")))
                .then(InfoCommand.command("debug", true)
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.debug")))
                .then(SealCommand.command()
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.seal")))
                .then(BrewerCommand.command()
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.brewer")))
                .then(StatusCommand.command()
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.status")))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            MessageUtil.message(sender, "tbp.command.reload-message");
                            TheBrewingProject.getInstance().reload();
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.reload"))
                )
                .then(Commands.literal("event")
                        .then(eventCommand)
                        .then(playerBranch(argument -> argument.then(eventCommand)))
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.event"))
                ).then(Commands.literal("replicate")
                        .then(playerBranch(argument -> argument.then(ReplicateCommand.command())))
                        .then(ReplicateCommand.command())
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.replicate"))
                ).then(Commands.literal("version")
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.version"))
                        .executes(commandContext -> {
                            MessageUtil.message(commandContext.getSource().getSender(), "tbp.command.version", Placeholder.unparsed("version", TheBrewingProject.getInstance().getPluginMeta().getVersion()));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
                ).then(Commands.literal("encryption")
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.encryption"))
                        .then(EncryptionCommand.command())
                ).then(DebugDumpCommand.command()
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.dump"))
                )
                .build();
    }

    public static ArgumentBuilder<CommandSourceStack, ?> playerBranch(Consumer<ArgumentBuilder<CommandSourceStack, ?>> childAction) {
        ArgumentBuilder<CommandSourceStack, ?> child = Commands.argument("player", ArgumentTypes.player());
        childAction.accept(child);
        return Commands.literal("for")
                .then(child)
                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.other"));
    }

    public static ArgumentBuilder<CommandSourceStack, ?> offlinePlayerBranch(Consumer<ArgumentBuilder<CommandSourceStack, ?>> childAction) {
        ArgumentBuilder<CommandSourceStack, ?> child = Commands.argument("player", OfflinePlayerArgument.SINGLE);
        childAction.accept(child);
        return Commands.literal("for")
                .then(child)
                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("brewery.command.other"));
    }


    public static OfflinePlayer getOfflinePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            List<OfflinePlayer> resolved = context.getArgument("player", OfflinePlayerSelectorArgumentResolver.class)
                    .resolve(context.getSource());
            if (resolved.isEmpty()) throw ERROR_UNDEFINED_PLAYER.create();
            return resolved.getFirst();
        } catch (IllegalArgumentException ignored) {}
        if (context.getSource().getSender() instanceof OfflinePlayer player) return player;
        throw ERROR_UNDEFINED_PLAYER.create();
    }

    public static Player getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            List<Player> resolved = context.getArgument("player", PlayerSelectorArgumentResolver.class)
                    .resolve(context.getSource());
            if (resolved.isEmpty()) throw ERROR_UNDEFINED_PLAYER.create();
            return resolved.getFirst();
        } catch (IllegalArgumentException e) {
            if (context.getSource().getSender() instanceof Player player) return player;
            throw ERROR_UNDEFINED_PLAYER.create();
        }
    }
}
