package dev.jsinco.brewery.listeners;

import dev.jsinco.brewery.objects.Cauldron;
import dev.jsinco.brewery.objects.Tickable;
import dev.jsinco.brewery.util.Logging;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BreweryEvents implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || block.getMetadata("tickable").isEmpty()) {
            return;
        }

        Logging.debugLog("Player interacted with a tickable block");

        for (Cauldron cauldron : Tickable.getActiveCauldrons()) {
            if (cauldron.getBlock().equals(block)) {
                cauldron.onEvent(event);
                return;
            }
        }
    }
}
