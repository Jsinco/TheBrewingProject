package dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.transaction.BarrelInsertEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BarrelInsertSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BarrelInsertEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on barrel brew insert";
    }

    public static BukkitSyntaxInfos.Event<BarrelInsertSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BarrelInsertSkriptEvent.class, "Barrel brew insert")
                .addEvent(BarrelInsertEvent.class)
                .addPattern("barrel [brew] insert[ion]")
                .build();
    }
}
