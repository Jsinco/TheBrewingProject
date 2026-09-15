package dev.jsinco.brewery.bukkit.integration.event.skript.event.structure;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.structure.BarrelDestroyEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BarrelDestroySkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof BarrelDestroyEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on barrel destroy";
    }

    public static BukkitSyntaxInfos.Event<BarrelDestroySkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(BarrelDestroySkriptEvent.class, "Barrel destroy")
                .addEvent(BarrelDestroyEvent.class)
                .addPattern("barrel (destroy|destruction)")
                .build();
    }
}
