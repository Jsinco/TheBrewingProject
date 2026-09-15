package dev.jsinco.brewery.bukkit.integration.event.skript.event;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.TBPReloadEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class TBPReloadSkriptEvent extends SkriptEvent {

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof TBPReloadEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on tbp reload";
    }

    public static BukkitSyntaxInfos.Event<TBPReloadSkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(TBPReloadSkriptEvent.class, "TheBrewingProject reload")
                .addEvent(TBPReloadEvent.class)
                .addPattern("tbp reload")
                .build();
    }
}
