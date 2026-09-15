package dev.jsinco.brewery.bukkit.integration.event.skript.event.process;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.process.BrewCauldronProcessEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BrewCauldronProcessSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BrewCauldronProcessEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on cauldron process brew";
    }

    public static BukkitSyntaxInfos.Event<BrewCauldronProcessSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BrewCauldronProcessSkriptEvent.class, "Brew cauldron process")
                .addEvent(BrewCauldronProcessEvent.class)
                .addPatterns(
                        "cauldron process brew",
                        "brew cauldron process"
                )
                .build();
    }
}
