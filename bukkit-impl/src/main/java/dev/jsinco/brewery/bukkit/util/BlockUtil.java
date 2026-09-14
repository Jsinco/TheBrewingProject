package dev.jsinco.brewery.bukkit.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.util.ClassUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockUtil {

    private static final boolean PROTOCOL_LIB_ENABLED = ClassUtil.exists("com.comphenix.protocol.events.PacketContainer");
    private static final BoundingBox FULL_BLOCK = BoundingBox.of(new Vector(0, 0, 0), new Vector(1, 1, 1));
    private static final ItemStack NO_TOOL = ItemStack.of(Material.AIR);

    public static boolean isChunkLoaded(dev.jsinco.brewery.api.vector.Location block) {
        return BukkitAdapter.toLocation(block)
                .map(Location::isChunkLoaded)
                .orElse(false);
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

    public static void playWobbleEffect(dev.jsinco.brewery.api.vector.Location location, Player player) {
        if (!PROTOCOL_LIB_ENABLED) {
            return;
        }
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            return;
        }
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_ACTION);
        packet.getBlockPositionModifier()
                .writeSafely(0, new BlockPosition(location.x(), location.y(), location.z()));
        packet.getBytes()
                .writeSafely(0, (byte) 1)
                .writeSafely(1, (byte) 1);
        packet.getIntegers().writeSafely(0, 1); // Block id (this field is not read anyhow
        protocolManager.sendServerPacket(player, packet);
    }

    public static boolean isFullBlock(Block block) {
        return isFullBlock(block.getCollisionShape());
    }

    public static boolean isFullBlock(VoxelShape collisionShape) {
        Collection<BoundingBox> boxes = collisionShape.getBoundingBoxes();
        if (boxes.size() != 1) {
            return false;
        }
        BoundingBox hitbox = boxes.iterator().next();
        return hitbox.contains(FULL_BLOCK);
    }

    /**
     * Gets the tool types that mine a specific block faster than using your hand.
     *
     * @param blockType the specified block
     * @return set of tool types
     */
    public static Set<ToolType> getFasterTools(BlockType blockType) {
        BlockData blockData = blockType.createBlockData();
        return Arrays.stream(ToolType.values())
                .filter(toolType -> toolMinesFaster(blockData, toolType))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ToolType.class)));
    }

    private static boolean toolMinesFaster(BlockData blockData, ToolType toolType) {
        return blockData.getDestroySpeed(toolType.sampleToolItem) > blockData.getDestroySpeed(NO_TOOL);
    }

    public enum ToolType {
        SWORD(Material.NETHERITE_SWORD),
        PICKAXE(Material.NETHERITE_PICKAXE),
        AXE(Material.NETHERITE_AXE),
        SHOVEL(Material.NETHERITE_SHOVEL),
        HOE(Material.NETHERITE_HOE),
        SHEARS(Material.SHEARS);

        private final ItemStack sampleToolItem;

        ToolType(Material material) {
            sampleToolItem = ItemStack.of(material);
        }
    }

}
