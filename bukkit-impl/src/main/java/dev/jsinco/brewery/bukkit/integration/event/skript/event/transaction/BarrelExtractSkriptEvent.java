package dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.transaction.BarrelExtractEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BarrelExtractSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BarrelExtractEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on barrel brew extraction";
    }

    public static BukkitSyntaxInfos.Event<BarrelExtractSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BarrelExtractSkriptEvent.class, "Barrel brew extract")
                .addEvent(BarrelExtractEvent.class)
                .addPattern("barrel [brew] extract[ion]")
                .build();
    }
}
