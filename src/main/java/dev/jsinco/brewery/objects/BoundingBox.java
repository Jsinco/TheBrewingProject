package dev.jsinco.brewery.objects;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BoundingBox {

    private final World world;
    private final int xMin;
    private final int xMax;
    private final int yMin;
    private final int yMax;
    private final int zMin;
    private final int zMax;

    public BoundingBox(Location point1, Location point2) {
        this.world = point1.getWorld();
        this.xMin = Math.min(point1.getBlockX(), point2.getBlockX());
        this.xMax = Math.max(point1.getBlockX(), point2.getBlockX());
        this.yMin = Math.min(point1.getBlockY(), point2.getBlockY());
        this.yMax = Math.max(point1.getBlockY(), point2.getBlockY());
        this.zMin = Math.min(point1.getBlockZ(), point2.getBlockZ());
        this.zMax = Math.max(point1.getBlockZ(), point2.getBlockZ());
    }

    public List<Block> blockList() {
        List<Block> bL = new ArrayList<>();
        for (int x = this.xMin; x <= this.xMax; ++x) {
            for (int y = this.yMin; y <= this.yMax; ++y) {
                for (int z = this.zMin; z <= this.zMax; ++z) {
                    Block b = this.world.getBlockAt(x, y, z);
                    bL.add(b);
                }
            }
        }
        return bL;
    }


    public boolean contains(int x, int y, int z) {
        return (x >= xMin && x <= xMax) && (y >= yMin && y <= yMax) && (z >= zMin && z <= zMax);
    }

    public boolean contains(Location loc) {
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) && loc.getWorld().equals(world);
    }

    public boolean contains(Block block) {
        return contains(block.getX(), block.getY(), block.getZ()) && block.getWorld().equals(world);
    }


}
