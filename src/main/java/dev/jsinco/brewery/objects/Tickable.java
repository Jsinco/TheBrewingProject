package dev.jsinco.brewery.objects;

import lombok.Getter;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * All brewery objects which need to be constantly ticked extend this class (BreweryPlayer, Cauldron, Barrel, Distillery).
 */
public abstract class Tickable {

    @Getter
    private static final List<Cauldron> activeCauldrons = new ArrayList<>();
    @Getter
    private static final List<Barrel> activeBarrels = new ArrayList<>();
    @Getter
    private static final List<BreweryPlayer> activeBreweryPlayers = new ArrayList<>();

    /**
     * Called for all tickable objects every 1200 ticks (1 minute).
     */
    public abstract void tick();

    /**
     * Called every tick asynchronously. (Mainly for cauldron particles)
     */
    public void asyncFastTick() {
    }


    public abstract void add();
    public abstract void remove();

    public abstract void onEvent(Event e);
}
