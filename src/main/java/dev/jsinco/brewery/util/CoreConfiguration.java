package dev.jsinco.brewery.util;

import dev.jsinco.abstractjavafilelib.schemas.SnakeYamlConfig;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

public class CoreConfiguration {


    public static boolean cauldronMinimalParticles = false;
    public static List<Material> cauldronHeatSources = List.of(Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK);



    @Getter
    private static SnakeYamlConfig config;


    public static boolean loadConfigurations() {
        config = new SnakeYamlConfig("config.yml");
        return true;
    }
}
