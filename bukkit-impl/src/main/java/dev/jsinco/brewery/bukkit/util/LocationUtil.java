package dev.jsinco.brewery.bukkit.util;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.brew.BrewAdapterAccess;
import dev.jsinco.brewery.configuration.EventSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LocationUtil {

    /**
     * Resolves an actual valid world UUID from a String that can be a UUID or the world name
     *
     * @param worldNameOrUUID The world name or UUID
     * @return A valid World UUID
     */
    public static UUID resolveWorld(String worldNameOrUUID) {
        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(worldNameOrUUID));
        } catch (IllegalArgumentException e) {
            world = Bukkit.getWorld(worldNameOrUUID);
        }
        if (world == null) {
            return null;
        }
        return world.getUID();
    }

    public static @NonNull Location safeLocationInRadius(Location location, int radius) {
        Location center = location.clone();
        World world = center.getWorld();

        if (world == null || radius <= 0) {
            Location loc = safeLocationHere(center);
            return loc != null ? loc : center;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 50; i++) {
            int dx = random.nextInt(-radius, radius + 1);
            int dz = random.nextInt(-radius, radius + 1);
            if (dx * dx + dz * dz > radius * radius) continue; // Outside circle
            int x = center.getBlockX() + dx;
            int z = center.getBlockZ() + dz;
            Location loc = new Location(world, x + 0.5, world.getMaxHeight(), z + 0.5, center.getYaw(), center.getPitch());
            Location candidate = safeLocationHere(loc);
            if (isSafe(candidate)) return candidate;
        }

        Location loc = safeLocationHere(center);
        return loc != null ? loc : center;
    }

    public static @Nullable Location safeLocationHere(Location location) {
        Location loc = location.clone();
        if (isSafe(loc)) return loc;
        if (loc.getWorld() == null) return null;
        loc.setY(loc.getWorld().getHighestBlockYAt(loc));
        if (ThreadLocalRandom.current().nextInt(100) >=
                EventSection.events().undergroundChance()) loc.add(0, 1, 0);
        while (loc.getY() >= loc.getWorld().getMinHeight()) {
            if (isSafe(loc)) return loc;
            loc.setY(loc.getY() - 1);
        }
        return null;
    }

    public static boolean isSafe(Location location) {
        if (location == null) return false;
        World world = location.getWorld();
        if (world == null) return false;
        if (location.getY() < world.getMinHeight()
                || location.getY() > world.getMaxHeight()) return false;
        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        Block head = location.getBlock().getRelative(BlockFace.UP);

        if (below.isSolid() && !isDangerousBlock(below.getType()))
            return isEmptyOrPassable(location.getBlock().getType())
                    && isEmptyOrPassable(head.getType());

        return location.getBlock().getType() == Material.WATER &&
                below.getType() == Material.WATER &&
                isEmptyOrPassable(head.getType());
    }

    private static boolean isEmptyOrPassable(Material type) {
        if (type.isAir()) return true;
        if (type == Material.WATER ||
                type == Material.LAVA ||
                type == Material.CACTUS ||
                type == Material.SWEET_BERRY_BUSH ||
                type == Material.FIRE ||
                type == Material.SOUL_FIRE ||
                type == Material.COBWEB ||
                type == Material.POWDER_SNOW
        ) return false;
        return !type.isSolid() && !type.isOccluding();
    }

    private static boolean isDangerousBlock(Material type) {
        return switch (type) {
            case LAVA, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE, MAGMA_BLOCK, CACTUS, SWEET_BERRY_BUSH, WITHER_ROSE,
                 POWDER_SNOW -> true;
            default -> false;
        };
    }

    /**
     * Checks if a location is within the <strong>vertical</strong> build limit.
     *
     * @param location The location with a non-null world
     * @return true if the location is within the build limit
     */
    public static boolean isWithinBuildLimit(Location location) {
        World world = location.getWorld();
        return world.getMinHeight() <= location.getBlockY() && location.getBlockY() <= world.getMaxHeight();
    }

    public static void dropBrews(dev.jsinco.brewery.api.vector.Location breweryLocation, List<Brew> drops) {
        BukkitAdapter.toLocation(breweryLocation).ifPresent(location -> dropBrews(location, drops));
    }

    public static void dropBrews(Location location, List<Brew> drops) {
        Location offset = location.clone().add(0.5, 0, 0.5);
        drops.stream()
                .map(brew -> BrewAdapterAccess.toItem(brew, new Brew.State.Other()))
                .forEach(itemStack -> offset.getWorld().dropItem(offset, itemStack));
    }

}
