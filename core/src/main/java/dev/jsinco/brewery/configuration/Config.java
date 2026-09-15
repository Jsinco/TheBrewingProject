package dev.jsinco.brewery.configuration;

import dev.jsinco.brewery.api.config.Configuration;
import dev.jsinco.brewery.api.util.LoggingModule;
import dev.jsinco.brewery.configuration.structure.BarrelSection;
import dev.jsinco.brewery.configuration.structure.CauldronSection;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Exclude;
import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Config extends OkaeriConfig implements Configuration {

    @Comment("Config version. Don't change this")
    @CustomKey("config-version")
    private int configVersion = 1;

    @Comment("What language file should we use? See: /TheBrewingProject/locale")
    @CustomKey("language")
    private Locale language = Locale.US;

    @Comment("Allow hoppers to interact with distilleries and barrels")
    @CustomKey("automation-enabled")
    private boolean automation = true;

    @Comment("Whether an ingredient can be added into a brew regardless if it's not in any of the recipes")
    @CustomKey("allow-unregistered-ingredients")
    private boolean allowUnregisteredIngredients = false;

    @Comment("Whether items should be consumed when in creative mode when using it on tbp structures")
    @CustomKey("consume-items-in-creative")
    private boolean consumeItemsInCreative = false;

    @Comment("Whether everything non-item related should be translated to the players locale")
    @CustomKey("client-side-translations")
    private boolean clientSideTranslations = false;

    @Comment({"Some clients and tools allow players to see the NBT data of an item, which, for our potions, includes",
            "all the brewing steps it went through. Encrypt this data to prevent it from being abused as a recipe?"})
    @CustomKey("encrypt-sensitive-data")
    private boolean encryptSensitiveData = true;

    @Comment("The key that is going to be used for the encryption, this is unique per server")
    private SecretKey encryptionKey = generateAesKey();

    @Comment("A list of previous keys to try when decryption with the current one fails")
    private List<SecretKey> previousEncryptionKeys = List.of();

    @Comment({"Should we re-encrypt all items in opened inventories to use the newest encryption standard and the latest key?",
            "If your encryption key got leaked, use this to prevent anyone from seeing items encrypted with old keys ever again"})
    private boolean reencryptItemsInInventories = false;

    @Comment("Whether we should try to migrate old brews from BreweryX (TBP's predecessor) to the new TBP format")
    private boolean migrateFromBreweryX = true;

    @Comment({"A list of encryption seeds from BreweryX used to convert encrypted BreweryX brews",
            "This only converts brews when a recipe with matching id is configured in TBP"})
    private List<Long> breweryxMigrationSeeds = List.of();

    @CustomKey("empty-any-drink-using-hopper")
    @Comment("Empty any drink when right-clicking a hopper. If false, only applies to failed brews")
    private boolean emptyAnyDrinkUsingHopper = false;

    @CustomKey("brewers-display")
    @Comment({"How a brewer should be displayed on the brew", "Values = [none, first_step, last_step, lead_brewer, all]"})
    private BrewersDisplay brewersDisplay = BrewersDisplay.NONE;

    @CustomKey("brew-tooltip-order")
    @Comment("Define the order to display the brew tooltip in the item lore")
    private List<BrewTooltipType> brewTooltipOrder = List.of(
            BrewTooltipType.RECIPE_LORE,
            BrewTooltipType.EMPTY_LINE,
            BrewTooltipType.SEALED_TEXT,
            BrewTooltipType.SCORE,
            BrewTooltipType.STEPS,
            BrewTooltipType.BREWERS,
            BrewTooltipType.MODIFIER
    );

    @CustomKey("cauldrons")
    private CauldronSection cauldrons = new CauldronSection();

    @CustomKey("barrels")
    private BarrelSection barrels = new BarrelSection();

    @Comment({"This field accepts either a single sound definition or a list of definitions.",
            "If a list is provided, one sound will be chosen randomly.",
            "",
            "A single sound entry is a string with one of the following formats",
            "- <sound_id>",
            "- <sound_id>/<pitch>",
            "- <sound_id>/<min_pitch>;<max_pitch>",
            "",
            "See the default values below for examples"
    })
    @CustomKey("sounds")
    private SoundSection sounds = new SoundSection();

    @CustomKey("command-aliases")
    @Comment("The aliases for the 'tbp' command")
    private List<String> commandAliases = List.of("brewery", "brew");

    @CustomKey("integration-blacklist")
    @Comment({"Disable some integrations. (Only loaded on startup)",
            "Matches against an integration type and integration name.",
            "for example 'item.nexo' for exact match, and just 'nexo' to match with all nexo integrations"})
    private List<String> integrationBlacklist = List.of();

    @CustomKey("verbose-logging")
    @Comment({"What modules should show verbose logs to the console,",
            "can be a list with the allowed [resource_pack_parsing]"})
    private Set<LoggingModule> verboseLogging = Set.of();

    @Exclude
    private static Config instance;

    public static void load(File dataFolder, OkaeriSerdes... packs) {
        Config.instance = ConfigManager.create(Config.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), packs);
            it.withBindFile(new File(dataFolder, "config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });
    }

    public static Config config() {
        return instance;
    }

    public static SecretKey generateAesKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            try {
                kg.init(256, SecureRandom.getInstanceStrong());
            } catch (Exception ignored) {
                kg.init(128, new SecureRandom());
            }
            return kg.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public int configVersion() {
        return this.configVersion;
    }

    public Locale language() {
        return this.language;
    }

    public boolean automation() {
        return this.automation;
    }

    public boolean allowUnregisteredIngredients() {
        return this.allowUnregisteredIngredients;
    }

    public boolean consumeItemsInCreative() {
        return this.consumeItemsInCreative;
    }

    public boolean clientSideTranslations() {
        return this.clientSideTranslations;
    }

    public boolean encryptSensitiveData() {
        return this.encryptSensitiveData;
    }

    public SecretKey encryptionKey() {
        return this.encryptionKey;
    }

    public List<SecretKey> previousEncryptionKeys() {
        return this.previousEncryptionKeys;
    }

    public boolean reencryptItemsInInventories() {
        return this.reencryptItemsInInventories;
    }

    public boolean migrateFromBreweryX() {
        return this.migrateFromBreweryX;
    }

    public List<Long> breweryxMigrationSeeds() {
        return this.breweryxMigrationSeeds;
    }

    public boolean emptyAnyDrinkUsingHopper() {
        return this.emptyAnyDrinkUsingHopper;
    }

    public BrewersDisplay brewersDisplay() {
        return this.brewersDisplay;
    }

    public List<BrewTooltipType> brewTooltipOrder() {
        return this.brewTooltipOrder;
    }

    public CauldronSection cauldrons() {
        return this.cauldrons;
    }

    public BarrelSection barrels() {
        return this.barrels;
    }

    public SoundSection sounds() {
        return this.sounds;
    }

    public List<String> commandAliases() {
        return this.commandAliases;
    }

    public List<String> integrationBlacklist() {
        return this.integrationBlacklist;
    }

    public Set<LoggingModule> verboseLogging() {
        return verboseLogging;
    }
}
