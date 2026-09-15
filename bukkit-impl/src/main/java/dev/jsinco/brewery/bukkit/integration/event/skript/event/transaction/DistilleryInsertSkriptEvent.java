package dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.transaction.DistilleryInsertEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class DistilleryInsertSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof DistilleryInsertEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on distillery brew insert";
    }

    public static BukkitSyntaxInfos.Event<DistilleryInsertSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(DistilleryInsertSkriptEvent.class, "Distillery brew insert")
                .addEvent(DistilleryInsertEvent.class)
                .addPattern("distill[ery] [brew] insert[ion]")
                .build();
    }
}
