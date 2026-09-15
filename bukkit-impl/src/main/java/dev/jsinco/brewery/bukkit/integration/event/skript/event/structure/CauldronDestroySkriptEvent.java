package dev.jsinco.brewery.bukkit.integration.event.skript.event.structure;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.structure.CauldronDestroyEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class CauldronDestroySkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof CauldronDestroyEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on cauldron destroy";
    }

    public static BukkitSyntaxInfos.Event<CauldronDestroySkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(CauldronDestroySkriptEvent.class, "Cauldron destroy")
                .addEvent(CauldronDestroyEvent.class)
                .addPattern("cauldron (destroy|destruction)")
                .build();
    }

}
