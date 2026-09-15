package dev.jsinco.brewery.bukkit.integration.event.skript.event.structure;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import dev.jsinco.brewery.bukkit.api.event.structure.DistilleryDestroyEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class DistilleryDestroySkriptEvent extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof DistilleryDestroyEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "on distillery destroy";
    }

    public static BukkitSyntaxInfos.Event<DistilleryDestroySkriptEvent> syntaxInfo() {
        return BukkitSyntaxInfos.Event.builder(DistilleryDestroySkriptEvent.class, "Distillery destroy")
                .addEvent(DistilleryDestroyEvent.class)
                .addPattern("distillery (destroy|destruction)")
                .build();
    }
}
