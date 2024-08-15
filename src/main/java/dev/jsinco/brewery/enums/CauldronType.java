package dev.jsinco.brewery.enums;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum CauldronType {
    WATER(Material.WATER_CAULDRON),
    LAVA(Material.LAVA_CAULDRON),
    SNOW(Material.POWDER_SNOW_CAULDRON);

    private final Material material;

    CauldronType(Material material) {
        this.material = material;
    }
}
