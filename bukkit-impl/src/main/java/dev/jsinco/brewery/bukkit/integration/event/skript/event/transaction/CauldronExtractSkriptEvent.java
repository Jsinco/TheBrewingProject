package dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.transaction.CauldronExtractEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class CauldronExtractSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof CauldronExtractEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on cauldron brew extract";
    }

    public static BukkitSyntaxInfos.Event<CauldronExtractSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(CauldronExtractSkriptEvent.class, "Cauldron brew insert")
                .addEvent(CauldronExtractEvent.class)
                .addPattern("cauldron [brew] extract[ion]")
                .build();
    }
}
