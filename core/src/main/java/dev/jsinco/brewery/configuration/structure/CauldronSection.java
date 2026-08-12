package dev.jsinco.brewery.configuration.structure;

import com.google.common.collect.ImmutableMap;
import dev.jsinco.brewery.api.brew.BrewQuality;
import dev.jsinco.brewery.api.config.Configuration;
import dev.jsinco.brewery.api.effect.modifier.ModifierDisplay;
import dev.jsinco.brewery.api.ingredient.IngredientInput;
import dev.jsinco.brewery.api.ingredient.UncheckedIngredient;
import dev.jsinco.brewery.api.ingredient.WildcardIngredient;
import dev.jsinco.brewery.api.math.RangeD;
import dev.jsinco.brewery.api.moment.Moment;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.Holder;
import dev.jsinco.brewery.configuration.AnimationDisplay;
import dev.jsinco.brewery.configuration.ParticleDefinition;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CauldronSection extends OkaeriConfig implements Configuration.Cauldrons {

    @Comment({"What blocks cauldrons must have below them to be able to brew.",
            "If this list is empty, cauldrons will brew regardless of the block below them.",
            "Campfires must be lit and lava must be a source block."})
    @CustomKey("heat-sources")
    private List<Holder.Material> heatSources = Stream.of("campfire", "soul_campfire", "lava", "fire", "soul_fire", "magma_block")
            .map(Holder.Material::fromMinecraftId)
            .toList();

    @Comment("How many ticks it will take to cook something one minute")
    @CustomKey("cooking-minute-ticks")
    private long cookingMinuteTicks = Moment.MINUTE;

    @Comment("The base color lava cauldrons have")
    @CustomKey("lava-base-particle-color")
    private Color lavaBaseParticleColor = new Color(Integer.parseInt("d45a12", 16));

    @Comment("The base color water cauldrons have. If none is provided, the biome color will be used")
    @CustomKey("water-base-particle-color")
    private @Nullable Color waterBaseParticleColor = null;

    @Comment("The base color snow cauldrons have")
    @CustomKey("snow-base-particle-color")
    private Color snowBaseParticleColor = new Color(Integer.parseInt("f8fdfd", 16));

    @Comment("The base color snow cauldrons have")
    @CustomKey("failed-particle-color")
    private Color failedParticleColor = new Color(NamedTextColor.GRAY.value());

    @Comment("Whether to color the water in cauldrons using a text display")
    @CustomKey("colored-water")
    private boolean coloredWater = true;

    @Comment("The water color is a text display, this defines the opacity of the text display (0 - 255)")
    @CustomKey("water-color-opacity")
    private int waterColorOpacity = (128 & 0xFF);

    @Comment("To whom animations should be rendered when adding items [none, brewer, everyone]")
    @CustomKey("ingredient-added-animation-display")
    private AnimationDisplay ingredientAddedAnimation = AnimationDisplay.NONE;

    @Comment("Reset the ingredient cook time whenever a new ingredient is added")
    @CustomKey("reset-cook-time-on-ingredient-add")
    private boolean resetCookTimeOnIngredientAdd = true;

    @Comment("How to display the time [action_bar, chat, title]")
    @CustomKey("clock-display")
    private ModifierDisplay.DisplayWindow clockDisplay = ModifierDisplay.DisplayWindow.ACTION_BAR;

    @Comment("What items can be used to display time")
    @CustomKey("clock-items")
    private List<IngredientInput> clockItems = List.of(UncheckedIngredient.minecraft("clock"));

    @Comment({"What particles should be displayed when brewing in a cauldron",
            "Allowed particle effects can be found here https://jd.papermc.io/paper/26.1.2/org/bukkit/Particle.html"})
    @CustomKey("cook-particle-definitions")
    private List<ParticleDefinition> cookParticleDefinitions = List.of(
            new ParticleDefinition(BreweryKey.minecraft("crit"), 0.01, new RangeD(0.8, null), BrewQuality.EXCELLENT),
            new ParticleDefinition(BreweryKey.minecraft("entity_effect"), 0.1, new RangeD(0.1, null), null),
            new ParticleDefinition(BreweryKey.minecraft("dust"), 0.2, new RangeD(0.05, 0.1), null)
    );

    @Comment({"What particles should be displayed when brewing in a cauldron",
            "Allowed particle effects can be found here https://jd.papermc.io/paper/26.1.2/org/bukkit/Particle.html"})
    @CustomKey("mix-particle-definitions")
    private List<ParticleDefinition> mixParticleDefinitions = List.of(
            new ParticleDefinition(BreweryKey.minecraft("crit"), 0.01, new RangeD(0.8, null), BrewQuality.EXCELLENT),
            new ParticleDefinition(BreweryKey.minecraft("entity_effect"), 0.05, null, null)
    );

    @Comment("Whether sealed brews can be added as an ingredient to a cauldron brewing a different recipe")
    @CustomKey("allow-sealed-brew-as-ingredient")
    private boolean allowSealedBrewAsIngredient = false;

    @Comment("What items should be transformed into another item when added as an ingredient")
    @CustomKey("ingredient-empty-transforms")
    private Map<IngredientInput, UncheckedIngredient> ingredientEmptyTransforms = new ImmutableMap.Builder<IngredientInput, UncheckedIngredient>()
            .put(WildcardIngredient.get("brewery:*"), UncheckedIngredient.minecraft("glass_bottle"))
            .put(UncheckedIngredient.minecraft("potion"), UncheckedIngredient.minecraft("glass_bottle"))
            .put(UncheckedIngredient.minecraft("lingering_potion"), UncheckedIngredient.minecraft("glass_bottle"))
            .put(UncheckedIngredient.minecraft("honey_bottle"), UncheckedIngredient.minecraft("glass_bottle"))
            .put(UncheckedIngredient.minecraft("ominous_bottle"), UncheckedIngredient.minecraft("glass_bottle"))
            .put(UncheckedIngredient.minecraft("dragons_breath"), UncheckedIngredient.minecraft("glass_bottle"))
            .put(UncheckedIngredient.minecraft("milk_bucket"), UncheckedIngredient.minecraft("bucket"))
            .put(UncheckedIngredient.minecraft("lava_bucket"), UncheckedIngredient.minecraft("bucket"))
            .put(UncheckedIngredient.minecraft("water_bucket"), UncheckedIngredient.minecraft("bucket"))
            .put(UncheckedIngredient.minecraft("powder_snow_bucket"), UncheckedIngredient.minecraft("bucket"))
            .put(UncheckedIngredient.minecraft("beetroot_soup"), UncheckedIngredient.minecraft("bowl"))
            .put(UncheckedIngredient.minecraft("mushroom_stew"), UncheckedIngredient.minecraft("bowl"))
            .put(UncheckedIngredient.minecraft("rabbit_stew"), UncheckedIngredient.minecraft("bowl"))
            .put(UncheckedIngredient.minecraft("suspicious_stew"), UncheckedIngredient.minecraft("bowl"))
            .build();

    public boolean allowSealedBrewAsIngredient() {
        return this.allowSealedBrewAsIngredient;
    }

    public List<Holder.Material> heatSources() {
        return this.heatSources;
    }

    public long cookingMinuteTicks() {
        return this.cookingMinuteTicks;
    }

    public Color lavaBaseParticleColor() {
        return this.lavaBaseParticleColor;
    }

    public @Nullable Color waterBaseParticleColor() {
        return this.waterBaseParticleColor;
    }

    public Color snowBaseParticleColor() {
        return this.snowBaseParticleColor;
    }

    public Color failedParticleColor() {
        return this.failedParticleColor;
    }

    public boolean coloredWater() {
        return this.coloredWater;
    }

    public int waterColorOpacity() {
        return this.waterColorOpacity;
    }

    public AnimationDisplay ingredientAddedAnimation() {
        return this.ingredientAddedAnimation;
    }

    public Map<IngredientInput, UncheckedIngredient> ingredientEmptyTransforms() {
        return this.ingredientEmptyTransforms;
    }

    public ModifierDisplay.DisplayWindow clockDisplay() {
        return clockDisplay;
    }

    public List<IngredientInput> clockItems() {
        return clockItems;
    }

    public List<ParticleDefinition> cookParticleDefinitions() {
        return cookParticleDefinitions;
    }

    public List<ParticleDefinition> mixParticleDefinitions() {
        return mixParticleDefinitions;
    }

    public boolean resetCookTimeOnIngredientAdd() {
        return resetCookTimeOnIngredientAdd;
    }
}
