package dev.jsinco.brewery.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;

public class BlockUtil {

    // Check if a chunk/block is loaded without loading it
    public static boolean isChunkLoaded(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }


    public static boolean isLitCampfire(Block block) {
        if (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
            return ((Lightable) block.getBlockData()).isLit();
        }
        return false;
    }

    public static boolean isSource(Block block) {
        if (block.getType() == Material.LAVA || block.getType() == Material.WATER) {
            return ((Levelled) block.getBlockData()).getLevel() == 0;
        }
        return false;
    }
}
