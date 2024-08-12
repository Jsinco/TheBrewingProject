package dev.jsinco.brewery.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * Class for traditional barrels which use BoundingBoxes to determine the area of the barrel.
 *
 */
public class Barrel implements Tickable {

    private final UUID objectId;
    private final BoundingBox boundingBox;
    private final Location barrelSign;
    private final Inventory inventory;

    public Barrel(BoundingBox boundingBox, Location barrelSign) {
        this.objectId = UUID.randomUUID();
        this.boundingBox = boundingBox;
        this.barrelSign = barrelSign;
        this.inventory = Bukkit.createInventory(null, 27, "Barrel");
    }

    public Barrel(UUID objectId, BoundingBox boundingBox, Location barrelSign, Inventory inventory) {
        this.objectId = objectId;
        this.boundingBox = boundingBox;
        this.barrelSign = barrelSign;
        this.inventory = inventory;
    }
}
