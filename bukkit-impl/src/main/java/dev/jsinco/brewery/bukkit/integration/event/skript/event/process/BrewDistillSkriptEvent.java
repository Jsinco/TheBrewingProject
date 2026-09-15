package dev.jsinco.brewery.bukkit.integration.event.skript.event.process;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.process.BrewDistillEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BrewDistillSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BrewDistillEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on brew distill";
    }

    public static BukkitSyntaxInfos.Event<BrewDistillSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BrewDistillSkriptEvent.class, "Brew distill")
                .addEvent(BrewDistillEvent.class)
                .addPattern("brew distill[ation]")
                .build();
    }
}
