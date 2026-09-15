package dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.transaction.CauldronInsertEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class CauldronInsertSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof CauldronInsertEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on cauldron brew insert";
    }

    public static BukkitSyntaxInfos.Event<DistilleryExtractSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(DistilleryExtractSkriptEvent.class, "Cauldron brew insert")
                .addEvent(CauldronInsertEvent.class)
                .addPattern("cauldron [brew] insert[ion]")
                .build();
    }
}
