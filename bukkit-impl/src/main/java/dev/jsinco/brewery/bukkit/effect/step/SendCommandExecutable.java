package dev.jsinco.brewery.bukkit.effect.step;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.step.SendCommand;
import dev.jsinco.brewery.api.event.step.SendCommand.CommandSenderType;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SendCommandExecutable implements EventPropertyExecutable {

    private final String command;
    private final CommandSenderType senderType;
    private static final List<String> PLAYER_PLACEHOLDERS = List.of(
            "@player@",
            "@player_name@",
            "%player%",
            "%player_name%",
            "<player>",
            "<player_name>"
    );

    public SendCommandExecutable(String command, CommandSenderType senderType) {
        this.command = command;
        this.senderType = senderType;
    }

    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null) {
            return ExecutionResult.CONTINUE;
        }
        String command = this.command;
        for (String playerPlaceholder : PLAYER_PLACEHOLDERS) {
            command = command.replace(playerPlaceholder, player.getName());
        }
        String finalCommand = command;
        switch (senderType) {
            case PLAYER -> player.performCommand(command);
            case SERVER -> Bukkit.getGlobalRegionScheduler().execute(TheBrewingProject.getInstance(), () ->
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
        return ExecutionResult.CONTINUE;
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public EventStepProperty toProperty() {
        return new SendCommand(command, senderType);
    }

    @Override
    public ExecutionContext context() {
        return ExecutionContext.PLAYER;
    }

    @Override
    public EventPropertyExecutable withSkipPoint(@Nullable EventPropertyExecutable point) {
        return this; // NO-OP
    }

}
