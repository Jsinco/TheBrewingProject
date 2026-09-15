package dev.jsinco.brewery.bukkit.integration.event.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;
import java.util.function.Consumer;

public class TbpSkriptEventStructure extends Structure {


    private BreweryKey key;
    private Component displayName;
    private SectionNode source;
    private List<TriggerItem> code;
    private final Consumer<SkriptIntegration.SkriptIntegrationEvent> completedConsumer;

    public TbpSkriptEventStructure(Consumer<SkriptIntegration.SkriptIntegrationEvent> completedConsumer) {
        this.completedConsumer = completedConsumer;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (debug) {
            return String.format("TbpSkriptEventStructure(key='%s', displayName='%s')", key.minimalized(SkriptIntegration.NAMESPACE), MiniMessage.miniMessage().serialize(displayName));
        }
        return String.format("[tbp_]register event with key \"%s\" [and ][display ]name \"%s\"", key.minimalized(SkriptIntegration.NAMESPACE), MiniMessage.miniMessage().serialize(displayName));
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
        this.key = BreweryKey.parse((String) args[0].getSingle());
        this.displayName = (Component) args[1].getSingle();
        if (entryContainer == null) {
            Skript.error("[TBP] Empty entry container, this event does nothing!");
            return false;
        }
        this.source = entryContainer.getSource();
        return true;
    }

    @Override
    public boolean load() {
        this.code = ScriptLoader.loadItems(source);
        completedConsumer.accept(toTbpEvent());
        return true;
    }

    public SkriptIntegration.SkriptIntegrationEvent toTbpEvent() {
        return new SkriptIntegration.SkriptIntegrationEvent(
                key,
                displayName,
                player -> BukkitAdapter.toPlayer(player).ifPresent(bukkitPlayer ->
                        TriggerItem.walk(code.getFirst(), new DummyEvent(bukkitPlayer))
                )
        );
    }

    public static void register(SyntaxRegistry syntaxRegistry, Consumer<SkriptIntegration.SkriptIntegrationEvent> completedConsumer) {
        syntaxRegistry
                .register(SyntaxRegistry.STRUCTURE, DefaultSyntaxInfos.Structure.builder(TbpSkriptEventStructure.class)
                        .addPattern("[tbp_]register event with key %text% [and ][display ]name %text component%")
                        .supplier(() -> new TbpSkriptEventStructure(completedConsumer))
                        .build()
                );
    }

    public static class DummyEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();

        private final Player player;

        public DummyEvent(Player player) {
            this.player = player;
        }

        public Player getPlayer() {
            return this.player;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }
}
