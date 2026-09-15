package dev.jsinco.brewery.bukkit.integration.event.skript;

import ch.njol.skript.Skript;
import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.breweries.BarrelAccess;
import dev.jsinco.brewery.api.breweries.Cauldron;
import dev.jsinco.brewery.api.breweries.CauldronType;
import dev.jsinco.brewery.api.breweries.DistilleryAccess;
import dev.jsinco.brewery.api.event.DrunkEvent;
import dev.jsinco.brewery.api.event.EventData;
import dev.jsinco.brewery.api.event.EventProbability;
import dev.jsinco.brewery.api.event.IntegrationEvent;
import dev.jsinco.brewery.api.recipe.RecipeEffects;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.Holder;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.event.BrewConsumeEvent;
import dev.jsinco.brewery.bukkit.api.event.DrunkEventInitiateEvent;
import dev.jsinco.brewery.bukkit.api.event.process.BrewAgeEvent;
import dev.jsinco.brewery.bukkit.api.event.process.BrewCauldronProcessEvent;
import dev.jsinco.brewery.bukkit.api.event.process.BrewDistillEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.BarrelDestroyEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.CauldronDestroyEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.DistilleryDestroyEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.BarrelExtractEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.BarrelInsertEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.CauldronExtractEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.CauldronInsertEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.DistilleryExtractEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.DistilleryInsertEvent;
import dev.jsinco.brewery.bukkit.api.integration.EventIntegration;
import dev.jsinco.brewery.bukkit.api.transaction.ItemSource;
import dev.jsinco.brewery.bukkit.api.transaction.ItemTransactionSession;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.BrewConsumeSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.DrunkEventInitiateSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.TBPReloadSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.process.BrewAgeSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.process.BrewCauldronProcessSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.process.BrewDistillSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.structure.BarrelDestroySkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.structure.CauldronDestroySkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.structure.DistilleryDestroySkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction.BarrelExtractSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction.BarrelInsertSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction.CauldronExtractSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction.CauldronInsertSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction.DistilleryExtractSkriptEvent;
import dev.jsinco.brewery.bukkit.integration.event.skript.event.transaction.DistilleryInsertSkriptEvent;
import dev.jsinco.brewery.util.ClassUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class SkriptIntegration implements EventIntegration<SkriptIntegration.SkriptIntegrationEvent> {

    public static final String NAMESPACE = "skript";
    private final Map<BreweryKey, SkriptIntegrationEvent> events = new HashMap<>();

    @Override
    public Class<SkriptIntegrationEvent> eClass() {
        return SkriptIntegrationEvent.class;
    }

    @Override
    public List<BreweryKey> listEventKeys() {
        return List.copyOf(events.keySet());
    }

    @Override
    public Optional<SkriptIntegrationEvent> convertToEvent(EventData eventData) {
        return Optional.empty();
    }

    @Override
    public EventData convertToData(SkriptIntegrationEvent event) {
        return null;
    }

    @Override
    public String getId() {
        return NAMESPACE;
    }

    @Override
    public boolean isEnabled() {
        return ClassUtil.exists("org.skriptlang.skript.addon.SkriptAddon");
    }

    @Override
    public void onLoad() {
        SkriptAddon skriptAddon = Skript.instance().registerAddon(TheBrewingProject.class, "TheBrewingProject-Skript");
        SyntaxRegistry syntaxRegistry = skriptAddon.syntaxRegistry();
        TbpSkriptEventStructure.register(syntaxRegistry, completed -> {
            if (events.containsKey(completed.key)) {
                Logger.logWarn("Duplicate skript event with key: " + completed.key);
            } else {
                events.put(completed.key, completed);
            }
        });
        EventValueRegistry eventRegistry = skriptAddon.registry(EventValueRegistry.class);

        eventRegistry.register(EventValue.builder(TbpSkriptEventStructure.DummyEvent.class, Player.class)
                .getter(TbpSkriptEventStructure.DummyEvent::getPlayer)
                .time(EventValue.Time.NOW)
                .build()
        );
        registerEvents(syntaxRegistry, eventRegistry);
    }

    private static void registerEvents(SyntaxRegistry syntaxRegistry, EventValueRegistry eventRegistry) {
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, TBPReloadSkriptEvent.syntaxInfo());
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, DrunkEventInitiateSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(DrunkEventInitiateEvent.class, Player.class, "player", DrunkEventInitiateEvent::getPlayer));
        eventRegistry.register(createEventValue(DrunkEventInitiateEvent.class, DrunkEvent.class, "drunk event", DrunkEventInitiateEvent::getDrunkenEvent));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BrewConsumeSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BrewConsumeEvent.class, ItemStack.class, "brew item", BrewConsumeEvent::getItem));
        eventRegistry.register(createEventValue(BrewConsumeEvent.class, Player.class, "player", BrewConsumeEvent::getPlayer));
        eventRegistry.register(createEventValue(BrewConsumeEvent.class, EquipmentSlot.class, "hand", BrewConsumeEvent::getHand));
        eventRegistry.register(createEventValue(BrewConsumeEvent.class, RecipeEffects.class, "recipe effects", BrewConsumeEvent::getRecipeEffects));
        eventRegistry.register(createEventValue(BrewConsumeEvent.class, ItemStack.class, "brew replacement", BrewConsumeEvent::getReplacement));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, DistilleryInsertSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(DistilleryInsertEvent.class, Player.class, "player", DistilleryInsertEvent::getPlayer));
        eventRegistry.register(createEventValue(DistilleryInsertEvent.class, DistilleryAccess.class, "distillery", DistilleryInsertEvent::getDistillery));
        eventRegistry.register(createEventValue(DistilleryInsertEvent.class, ItemTransactionSession.class, "transaction session", DistilleryInsertEvent::getTransactionSession));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, DistilleryExtractSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(DistilleryExtractEvent.class, Player.class, "player", DistilleryExtractEvent::getPlayer));
        eventRegistry.register(createEventValue(DistilleryExtractEvent.class, DistilleryAccess.class, "distillery", DistilleryExtractEvent::getDistillery));
        eventRegistry.register(createEventValue(DistilleryExtractEvent.class, ItemTransactionSession.class, "transaction session", DistilleryExtractEvent::getTransactionSession));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, CauldronInsertSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(CauldronInsertEvent.class, Cauldron.class, "cauldron", CauldronInsertEvent::getCauldron));
        eventRegistry.register(createEventValue(CauldronInsertEvent.class, ItemSource.ItemBasedSource.class, "item", CauldronInsertEvent::getItemSource));
        eventRegistry.register(createEventValue(CauldronInsertEvent.class, Player.class, "player", CauldronInsertEvent::getPlayer));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, CauldronExtractSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(CauldronExtractEvent.class, Cauldron.class, "cauldron", CauldronExtractEvent::getCauldron));
        eventRegistry.register(createEventValue(CauldronExtractEvent.class, ItemSource.class, "item", CauldronExtractEvent::getItemResult));
        eventRegistry.register(createEventValue(CauldronExtractEvent.class, Player.class, "player", CauldronExtractEvent::getPlayer));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BarrelInsertSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BarrelInsertEvent.class, BarrelAccess.class, "barrel", BarrelInsertEvent::getBarrel));
        eventRegistry.register(createEventValue(BarrelInsertEvent.class, Player.class, "player", BarrelInsertEvent::getPlayer));
        eventRegistry.register(createEventValue(BarrelInsertEvent.class, ItemTransactionSession.class, "transaction session", BarrelInsertEvent::getTransactionSession));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BarrelExtractSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BarrelExtractEvent.class, BarrelAccess.class, "barrel", BarrelExtractEvent::getBarrel));
        eventRegistry.register(createEventValue(BarrelExtractEvent.class, Player.class, "player", BarrelExtractEvent::getPlayer));
        eventRegistry.register(createEventValue(BarrelExtractEvent.class, ItemTransactionSession.class, "transaction session", BarrelExtractEvent::getTransactionSession));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, DistilleryDestroySkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(DistilleryDestroyEvent.class, DistilleryAccess.class, "distillery", DistilleryDestroyEvent::getDistillery));
        eventRegistry.register(createEventValue(DistilleryDestroyEvent.class, Player.class, "player", DistilleryDestroyEvent::getPlayer));
        eventRegistry.register(createEventValue(DistilleryDestroyEvent.class, Location.class, "location", DistilleryDestroyEvent::getLocation));
        eventRegistry.register(createEventValue(DistilleryDestroyEvent.class, BrewDrops.class, "brews", distilleryDestroyEvent -> new BrewDrops(distilleryDestroyEvent.getDrops())));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, CauldronDestroySkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(CauldronDestroyEvent.class, Cauldron.class, "cauldron", CauldronDestroyEvent::getCauldron));
        eventRegistry.register(createEventValue(CauldronDestroyEvent.class, Player.class, "player", CauldronDestroyEvent::getPlayer));
        eventRegistry.register(createEventValue(CauldronDestroyEvent.class, Location.class, "location", CauldronDestroyEvent::getLocation));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BarrelDestroySkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BarrelDestroyEvent.class, BarrelAccess.class, "barrel", BarrelDestroyEvent::getBarrel));
        eventRegistry.register(createEventValue(BarrelDestroyEvent.class, Player.class, "player", BarrelDestroyEvent::getPlayer));
        eventRegistry.register(createEventValue(BarrelDestroyEvent.class, Location.class, "location", BarrelDestroyEvent::getLocation));
        eventRegistry.register(createEventValue(BarrelDestroyEvent.class, BrewDrops.class, "brews", barrelDestroyEvent -> new BrewDrops(barrelDestroyEvent.getDrops())));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BrewDistillSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BrewDistillEvent.class, Brew.class, "brew result", BrewDistillEvent::getResult));
        eventRegistry.register(createEventValue(BrewDistillEvent.class, Brew.class, "brew source", BrewDistillEvent::getSource));
        eventRegistry.register(createEventValue(BrewDistillEvent.class, DistilleryAccess.class, "distillery", BrewDistillEvent::getDistillery));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BrewCauldronProcessSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BrewCauldronProcessEvent.class, Brew.class, "brew result", BrewCauldronProcessEvent::getResult));
        eventRegistry.register(createEventValue(BrewCauldronProcessEvent.class, Brew.class, "brew source", BrewCauldronProcessEvent::getSource));
        eventRegistry.register(createEventValue(BrewCauldronProcessEvent.class, Cauldron.class, "cauldron", BrewCauldronProcessEvent::getCauldron));
        eventRegistry.register(createEventValue(BrewCauldronProcessEvent.class, CauldronType.class, "cauldron type", BrewCauldronProcessEvent::getCauldronType));
        eventRegistry.register(createEventValue(BrewCauldronProcessEvent.class, boolean.class, "heated", BrewCauldronProcessEvent::isHeated));
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BrewAgeSkriptEvent.syntaxInfo());
        eventRegistry.register(createEventValue(BrewAgeEvent.class, Brew.class, "brew result", BrewAgeEvent::getResult));
        eventRegistry.register(createEventValue(BrewAgeEvent.class, Brew.class, "brew source", BrewAgeEvent::getSource));
        eventRegistry.register(createEventValue(BrewAgeEvent.class, BarrelAccess.class, "barrel", BrewAgeEvent::getBarrel));
    }

    public static <E extends Event, V> EventValue<E, V> createEventValue(Class<E> eClass, Class<V> vClass, String pattern, Function<E, V> conversion) {
        return EventValue.builder(eClass, vClass)
                .getter(conversion::apply)
                .time(EventValue.Time.NOW)
                .patterns(pattern)
                .build();
    }

    public record SkriptIntegrationEvent(BreweryKey key, Component displayName,
                                         Consumer<Holder.Player> run) implements IntegrationEvent {

        @Override
        public void run(Holder.Player player) {
            run.accept(player);
        }

        @Override
        public EventProbability probability() {
            return EventProbability.NONE;
        }
    }

}
