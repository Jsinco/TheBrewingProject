package dev.jsinco.brewery.bukkit.integration.event.skript.event;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.BrewConsumeEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BrewConsumeSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BrewConsumeEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on brew consume";
    }

    public static BukkitSyntaxInfos.Event<BrewConsumeSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BrewConsumeSkriptEvent.class, "Brew consume")
                .addEvent(BrewConsumeEvent.class)
                .addPattern("brew consume")
                .build();
    }
}
