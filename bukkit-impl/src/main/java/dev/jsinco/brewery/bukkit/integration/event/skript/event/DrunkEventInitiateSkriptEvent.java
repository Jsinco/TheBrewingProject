package dev.jsinco.brewery.bukkit.integration.event.skript.event;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.DrunkEventInitiateEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;

public class DrunkEventInitiateSkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof DrunkEventInitiateEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on drunk event initiate";
    }

    public static BukkitSyntaxInfos.Event<DrunkEventInitiateSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(DrunkEventInitiateSkriptEvent.class, "Drunk event initiate")
                .addPattern("[player] drunk[en] event initiate")
                .addEvent(DrunkEventInitiateEvent.class)
                .build();
    }
}
