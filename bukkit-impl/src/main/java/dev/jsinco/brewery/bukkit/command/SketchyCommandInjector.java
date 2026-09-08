package dev.jsinco.brewery.bukkit.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jsinco.brewery.api.util.Logger;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Collection;

// Injects brigadier commands into a running server
public final class SketchyCommandInjector {

    private static final String PAPER_COMMANDS_CLASS = "io.papermc.paper.command.brigadier.PaperCommands";

    private SketchyCommandInjector() {
    }

    // https://github.com/PaperMC/Paper/blob/main/paper-server/src/main/java/io/papermc/paper/command/brigadier/PaperCommands.java
    public static void inject(JavaPlugin plugin, LiteralCommandNode<CommandSourceStack> node, Collection<String> aliases) {
        try {
            Class<?> paperCommands = Class.forName(PAPER_COMMANDS_CLASS);
            Commands commands = (Commands) paperCommands.getField("INSTANCE").get(null);
            Method setValid = paperCommands.getMethod("setValid");
            Method invalidate = paperCommands.getMethod("invalidate");
            setValid.invoke(commands);
            try {
                commands.register(plugin.getPluginMeta(), node, null, aliases);
            } finally {
                invalidate.invoke(commands);
            }
        } catch (Exception e) {
            Logger.logErr("Failed to inject the brigadier command tree into the running server:");
            Logger.logErr(e);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, ignored -> player.updateCommands(), null);
        }
    }
}
