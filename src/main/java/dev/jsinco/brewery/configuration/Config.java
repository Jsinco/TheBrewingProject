package dev.jsinco.brewery.configuration;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.util.FileUtil;
import org.bukkit.Material;
import org.simpleyaml.configuration.file.YamlFile;

import java.nio.file.Path;
import java.util.List;

public final class Config extends AbstractConfig {

    @Key("config-version")
    @Comment("""
            Config version. Don't change this""")
    public static String CONFIG_VERSION = "1.0";

    @Key("language")
    @Comment("""
            What language file should we use? See: /TheBrewingProject/languages""")
    public static String LANGUAGE = "en-us";

    @Key("update-check")
    @Comment("""
            Resolved the latest version of TheBrewingProject and let's
            players with the permission node know when an update is available.""")
    public static boolean UPDATE_CHECK = true;


    @Key("auto-saving")
    @Comment("""
            Auto saving interval in minutes""")
    public static int AUTO_SAVE_INTERVAL = 9;

    @Key("verbose-logging")
    @Comment("""
            Enable verbose/debug logging""")
    public static boolean VERBOSE_LOGGING = false;


    // Brewing Settings



    // Storage Settings


    // Cauldron Settings
    @Key("cauldrons.minimal-particles")
    @Comment("""
            Reduce the number of particles that spawn while cauldrons brew.
            This won't affect performance, but it will make the particles less obtrusive.""")
    public static boolean MINIMAL_PARTICLES = false;

    @Key("cauldrons.heat-sources")
    @Comment("""
            What blocks cauldrons must have below them to be able to brew.
            If this list is empty, cauldrons will brew regardless of the block below them.
            Campfires must be lit and lava must be a source block.""")
    public static List<Material> HEAT_SOURCES = List.of(Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK);




    private static final Config CONFIG = new Config();

    public static void reload() {
        Path mainDir = TheBrewingProject.getInstance().getDataFolder().toPath();

        // extract default config from jar
        FileUtil.extractFile(Config.class, "config.yml", mainDir, false);

        CONFIG.reload(mainDir.resolve("config.yml"), Config.class);
    }

    private static void tryRenamePath(String oldPath, String newPath) {
        YamlFile config = CONFIG.getConfig();
        Object oldValue = config.get(oldPath);
        if (oldValue == null) {
            return; // old default doesn't exist; do nothing
        }
        if (config.get(newPath) != null) {
            return; // new default already set; do nothing
        }
        config.set(newPath, oldValue);
        config.set(oldPath, null);
        CONFIG.save();
    }
}