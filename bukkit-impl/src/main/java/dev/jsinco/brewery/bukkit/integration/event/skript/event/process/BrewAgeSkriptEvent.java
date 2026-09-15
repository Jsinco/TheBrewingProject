package dev.jsinco.brewery.bukkit.integration.event.skript.event.process;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.process.BrewAgeEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BrewAgeSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BrewAgeEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on brew age";
    }

    public static BukkitSyntaxInfos.Event<BrewAgeSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BrewAgeSkriptEvent.class, "Brew distill")
                .addEvent(BrewAgeEvent.class)
                .addPatterns("brew age[ing]")
                .build();
    }
}
