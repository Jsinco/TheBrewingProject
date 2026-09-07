package dev.jsinco.brewery.bukkit.breweries;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.brew.BrewingStep;
import dev.jsinco.brewery.api.breweries.Cauldron;
import dev.jsinco.brewery.api.breweries.CauldronType;
import dev.jsinco.brewery.api.ingredient.Ingredient;
import dev.jsinco.brewery.api.moment.Interval;
import dev.jsinco.brewery.api.moment.Moment;
import dev.jsinco.brewery.api.recipe.DefaultRecipe;
import dev.jsinco.brewery.api.recipe.Recipe;
import dev.jsinco.brewery.api.recipe.RecipeMatcherResult;
import dev.jsinco.brewery.api.recipe.RecipeResult;
import dev.jsinco.brewery.api.util.BreweryRegistry;
import dev.jsinco.brewery.api.util.CancelState;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.brew.BrewImpl;
import dev.jsinco.brewery.brew.CookStepImpl;
import dev.jsinco.brewery.brew.MixStepImpl;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.animation.AnimationManager;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.api.event.process.BrewCauldronProcessEvent;
import dev.jsinco.brewery.bukkit.api.event.transaction.CauldronInsertEvent;
import dev.jsinco.brewery.bukkit.api.transaction.ItemSource;
import dev.jsinco.brewery.bukkit.brew.BrewAdapterAccess;
import dev.jsinco.brewery.bukkit.ingredient.BukkitIngredientManager;
import dev.jsinco.brewery.bukkit.listener.ListenerUtil;
import dev.jsinco.brewery.bukkit.recipe.RecipeMatcherImpl;
import dev.jsinco.brewery.bukkit.util.BlockUtil;
import dev.jsinco.brewery.bukkit.util.BukkitIngredientUtil;
import dev.jsinco.brewery.bukkit.util.SoundPlayer;
import dev.jsinco.brewery.bukkit.util.color.ColorUtil;
import dev.jsinco.brewery.configuration.AnimationDisplay;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.configuration.ParticleDefinition;
import dev.jsinco.brewery.sound.SoundDefinition;
import dev.jsinco.brewery.util.BrewUtil;
import dev.jsinco.brewery.util.PresetColorsUtil;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.Vibration;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BukkitCauldron implements Cauldron {

    private static final Random RANDOM = new Random();

    private final BreweryLocation location;
    private boolean hot = false;
    private Brew brew;
    private boolean brewExtracted = false;
    private Color particleColor = Color.fromRGB(0x3F76E4);
    private RecipeMatcherResult<ItemStack> matcherResult = null;
    private boolean dirty = true;
    private TextDisplay waterColorer = null;
    private final CauldronType cauldronType;
    private DefaultRecipe<ItemStack> previousDefaultRecipe = null;

    public BukkitCauldron(BreweryLocation location, boolean hot, CauldronType cauldronType) {
        this.location = location;
        this.hot = hot;
        this.brew = new BrewImpl(List.of());
        this.cauldronType = cauldronType;
    }

    public BukkitCauldron(Brew brew, BreweryLocation location, CauldronType cauldronType) {
        this.location = location;
        this.brew = brew;
        this.cauldronType = cauldronType;
    }

    public static Optional<CauldronType> findCauldronType(Block block) {
        return BreweryRegistry.CAULDRON_TYPE.values()
                .stream()
                .filter(cauldronType -> block.getType().getKey().toString().equals(cauldronType.materialKey()))
                .findAny();
    }

    @Override
    public void tick() {
        BukkitAdapter.scheduleIfLoaded(location, TheBrewingProject.getInstance(), bukkitLocation -> {
            if (!Tag.CAULDRONS.isTagged(bukkitLocation.getBlock().getType()) || getBlock().getType() == Material.CAULDRON) {
                ListenerUtil.removeActiveSinglePositionStructure(this);
                return;
            }
            this.hot = isHeatSource(getBlock().getRelative(BlockFace.DOWN));
            recalculateBrewTime();
            if (Config.config().cauldrons().coloredWater() && (waterColorer == null || waterColorer.isDead())) {
                waterColorer = getBlock().getWorld().spawn(bukkitLocation.clone().add(0.5, 0, 0.5), TextDisplay.class, textDisplay -> {
                    setWaterText(textDisplay);
                    textDisplay.setTransformation(compileTransformation(bukkitLocation.getBlock().getBlockData()));
                    textDisplay.setPersistent(false);
                    textDisplay.setBackgroundColor(Color.fromARGB(0, 255, 255, 255));
                });
            }
            if (dirty || getBrewTime() % Config.config().cauldrons().cookingMinuteTicks() == 0) {
                this.matcherResult = RecipeMatcherImpl.builder()
                        .build()
                        .match(brew);
                dirty = false;
            }
            recalculateColor();
            if (waterColorer != null) {
                setWaterText(waterColorer);
            }
            this.playBrewingEffects();
        });
    }

    private void setWaterText(TextDisplay textDisplay) {
        Color newColor = ColorUtil.closestColorLimitedOpacity(particleColor, computeBaseParticleColor(getBlock()), Config.config().cauldrons().waterColorOpacity() & 0xFF);
        textDisplay.text(Component.text("█").color(TextColor.color(newColor.asRGB())));
        textDisplay.setTextOpacity((byte) (newColor.getAlpha() & 0xFF));
    }

    private Transformation compileTransformation(BlockData blockData) {
        float levelOffset;
        if (blockData instanceof Levelled levelled) {
            levelOffset = 6F / 16 + 9F / 16 * levelled.getLevel() / levelled.getMaximumLevel();
        } else {
            levelOffset = 15F / 16;
        }
        return new Transformation(
                new Vector3f(-1F / 16, levelOffset, 8F / 16),
                new AxisAngle4f(),
                new Vector3f(4F, 0, 4F),
                new AxisAngle4f((float) -Math.PI / 2, 1F, 0, 0)
        );
    }

    private long getBrewTime() {
        if (brew.lastStep() instanceof BrewingStep.TimedStep timedStep) {
            return timedStep.time().moment();
        }
        return 0L;
    }

    private Color computeParticleColor(Color baseColor, Color resultColor, Recipe<ItemStack> recipe) {
        List<BrewingStep> matchingSteps = matcherResult.matchingSteps();
        int lastIdx = matchingSteps.size() - 1;
        if (lastIdx >= recipe.getSteps().size()) {
            return baseColor;
        }
        if (brew.lastStep() instanceof BrewingStep.Cook cook && recipe.getSteps().get(lastIdx) instanceof BrewingStep.Cook expectedCook) {
            return ColorUtil.getNextColor(baseColor, resultColor, cook.time().moment(), expectedCook.time().moment());
        } else if (brew.lastStep() instanceof BrewingStep.Mix mix && recipe.getSteps().get(lastIdx) instanceof BrewingStep.Mix expectedMix) {
            return ColorUtil.getNextColor(baseColor, resultColor, mix.time().moment(), expectedMix.time().moment());
        }
        return baseColor;
    }

    private Color computeResultColor(Optional<Recipe<ItemStack>> recipeOptional) {
        if (recipeOptional.isEmpty()) {
            return convert(Config.config().cauldrons().failedParticleColor());
        }
        RecipeResult<ItemStack> recipeResult = matcherResult.recipeResult().orElse(null);
        if (matcherResult.score().completed() && recipeResult != null) {
            return Color.fromRGB(recipeResult.brewColor().getRGB() & 0xFFFFFF);
        }
        List<DefaultRecipe<ItemStack>> defaultRecipes = new ArrayList<>(BrewAdapterAccess.getPossibleDefaultRecipes(
                recipeOptional.orElse(null),
                TheBrewingProject.getInstance().getRecipeRegistry(),
                brew,
                false
        ));
        Optional<DefaultRecipe<ItemStack>> previous = previousDefaultRecipe == null ?
                Optional.empty() : defaultRecipes.stream()
                                   .filter(previousDefaultRecipe::equals)
                                   .findAny();
        Collections.shuffle(defaultRecipes);
        Optional<DefaultRecipe<ItemStack>> bestMatch = defaultRecipes.stream()
                .max(Comparator.comparingInt(DefaultRecipe::complexity));
        Optional<DefaultRecipe<ItemStack>> defaultRecipe;
        if (previous.isPresent()) {
            defaultRecipe = previous.get().complexity() < bestMatch.get().complexity() ? bestMatch : previous;
        } else {
            defaultRecipe = bestMatch;
        }
        Optional<Color> defaultRecipeColor = defaultRecipe
                .map(DefaultRecipe::result)
                .map(RecipeResult::brewColor)
                .map(java.awt.Color::getRGB)
                .map(integer -> integer & 0xFFFFFF)
                .map(Color::fromRGB);
        if (defaultRecipeColor.isPresent()) {
            previousDefaultRecipe = defaultRecipe.get();
            return defaultRecipeColor.get();
        }
        Map<? extends Ingredient, Integer> ingredients = BrewUtil.countIngredientTotal(brew.getCompletedSteps());
        if (ingredients.isEmpty()) {
            return Color.AQUA;
        }
        return BukkitIngredientUtil.ingredientData(ingredients)
                .first();
    }

    public boolean withIngredient(@NonNull ItemStack item, Player player) {
        CauldronInsertEvent insertEvent = new CauldronInsertEvent(this,
                new ItemSource.ItemBasedSource(item),
                player.hasPermission("brewery.cauldron.access") ?
                        new CancelState.Allowed() : new CancelState.PermissionDenied(Component.translatable("tbp.cauldron.access-denied")),
                player
        );
        if (!insertEvent.callEvent()) {
            if (insertEvent.getCancelState() instanceof CancelState.PermissionDenied(Component denyMessage)) {
                player.sendMessage(denyMessage);
            }
            return false;
        }
        this.hot = isHeatSource(getBlock().getRelative(BlockFace.DOWN));
        ItemStack addedItem = insertEvent.getItemSource().get();
        Optional<Brew> optionalAddedBrew = BrewAdapterAccess.fromItem(addedItem);
        if (optionalAddedBrew.isPresent()) {
            return handleBrewReaddition(addedItem, optionalAddedBrew.get(), player);
        }

        incrementLevel(item);
        this.hot = isHeatSource(getBlock().getRelative(BlockFace.DOWN));
        Brew newBrew = withIngredient(BukkitIngredientManager.INSTANCE.getIngredient(addedItem))
                .witModifiedLastStep(step ->
                        step instanceof BrewingStep.AuthoredStep<?> authoredStep
                                ? authoredStep.withBrewer(player.getUniqueId()) : step
                );
        if (!changeBrew(newBrew)) {
            return false;
        }
        playIngredientAddedEffects(addedItem, player);
        return true;
    }

    private Brew withIngredient(Ingredient ingredient) {
        long time = TheBrewingProject.getInstance().getTime();
        Brew newBrew;
        if (hot) {
            newBrew = brew.withLastStep(BrewingStep.Cook.class,
                    cook -> {
                        Map<Ingredient, Integer> ingredients = new HashMap<>(cook.ingredients());
                        int amount = ingredients.computeIfAbsent(ingredient, ignored -> 0);
                        ingredients.put(ingredient, amount + 1);
                        return cook.withIngredients(ingredients);
                    },
                    () -> new CookStepImpl(new Interval(time, time), Map.of(ingredient, 1), cauldronType)
            );
        } else {
            newBrew = brew.withLastStep(BrewingStep.Mix.class,
                    mix -> {
                        Map<Ingredient, Integer> ingredients = new HashMap<>(mix.ingredients());
                        int amount = ingredients.computeIfAbsent(ingredient, ignored -> 0);
                        ingredients.put(ingredient, amount + 1);
                        return mix.withIngredients(ingredients);
                    },
                    () -> new MixStepImpl(new Interval(time, time), Map.of(ingredient, 1), cauldronType)
            );
        }
        return newBrew;
    }

    private void incrementLevel(ItemStack itemStack) {
        if (!brewExtracted && itemStack.getType() == Material.POTION) {
            BukkitCauldron.incrementLevel(getBlock());
            updateLevel(getBlock().getBlockData());
        }
    }

    private boolean handleBrewReaddition(ItemStack item, Brew addedBrew, Player player) {
        if (getLevel() == 0 && changeBrew(addedBrew.withStep(newStep()))) {
            // Empty cauldron: reinsert brew into the cauldron with 1 water level
            incrementLevel(item);
            recalculateBrewTime();
            recalculateColor();
            playIngredientAddedEffects(item, player);
            return true;
        }
        List<BrewingStep> existing = new ArrayList<>(this.brew.getCompletedSteps());
        Optional<Brew> merged;
        if (existing.isEmpty()) {
            merged = Optional.of(addedBrew.withStep(newStep()));
        } else {
            BrewingStep thisStep = existing.removeLast();
            List<BrewingStep> added = new ArrayList<>(addedBrew.getCompletedSteps());
            merged = BrewUtil.mergeSteps(existing, added)
                    .map(steps -> Stream.concat(steps.stream(), Stream.of(thisStep)))
                    .map(Stream::toList)
                    .map(this.brew::withStepsReplaced);
        }
        if (merged.isPresent() && changeBrew(merged.get())) {
            if (getCauldronType() == CauldronType.BREW) incrementLevel(item);
            playIngredientAddedEffects(item, player);
            return true;
        }
        if (!BrewAdapterAccess.isBrew(item)) {
            return false;
        }
        Brew newBrew = withIngredient(BukkitIngredientManager.INSTANCE.getIngredient(item))
                .witModifiedLastStep(step ->
                        step instanceof BrewingStep.AuthoredStep<?> authoredStep
                                ? authoredStep.withBrewer(player.getUniqueId()) : step
                );
        if (!changeBrew(newBrew)) {
            return false;
        }
        if (getCauldronType() == CauldronType.BREW) incrementLevel(item);
        playIngredientAddedEffects(item, player);
        return true;
    }

    /**
     * Trigger an event for changing the brew, return the result
     *
     * @param newBrew The new brew
     * @return True if brew changed
     */
    private boolean changeBrew(Brew newBrew) {
        BrewCauldronProcessEvent brewCauldronProcessEvent = new BrewCauldronProcessEvent(this,
                cauldronType,
                hot,
                brew,
                newBrew
        );
        boolean shouldChange = brewCauldronProcessEvent.callEvent();
        if (shouldChange) {
            brew = brewCauldronProcessEvent.getResult();
            this.matcherResult = RecipeMatcherImpl.builder().build().match(brew);
            this.dirty = true;
        }
        return shouldChange;
    }

    private Color computeBaseParticleColor(Block block) {
        return switch (block.getType()) {
            case WATER_CAULDRON -> {
                java.awt.Color waterColorOverride = Config.config().cauldrons().waterBaseParticleColor();
                if (waterColorOverride == null) {
                    waterColorOverride = PresetColorsUtil.getWaterColor(block.getBiome().key());
                }
                yield convert(waterColorOverride == null ? java.awt.Color.BLUE : waterColorOverride);
            }
            case LAVA_CAULDRON -> convert(Config.config().cauldrons().lavaBaseParticleColor());
            case POWDER_SNOW_CAULDRON -> convert(Config.config().cauldrons().snowBaseParticleColor());
            default -> throw new IllegalStateException("Expected block to be cauldron type");
        };
    }

    private void recalculateColor() {
        Optional<Recipe<ItemStack>> recipeOptional = matcherResult.recipeMatch();
        Color resultColor = computeResultColor(recipeOptional);
        Color baseParticleColor = computeBaseParticleColor(getBlock());
        this.particleColor = recipeOptional.map(recipe -> computeParticleColor(baseParticleColor, resultColor, recipe))
                .orElseGet(() -> ColorUtil.getNextColor(baseParticleColor, convert(Config.config().cauldrons().failedParticleColor()), getBrewTime(), Moment.MINUTE * 3));
    }

    public void playBrewingEffects() {
        Block block = getBlock();
        World world = block.getWorld();
        Supplier<Location> locationSupplier = () ->
                block.getLocation().add(0.5 + (RANDOM.nextDouble() * 0.8 - 0.4), 0.9, 0.5 + (RANDOM.nextDouble() * 0.8 - 0.4));
        double progress;
        if (brew.lastStep() instanceof BrewingStep.TimedStep timedStep) {
            Optional<Recipe<ItemStack>> recipeOptional = matcherResult.recipeMatch();
            if (recipeOptional.isPresent() && brew.stepAmount() - 1 < recipeOptional.get().getSteps().size()
                    && recipeOptional.get().getSteps().get(brew.stepAmount() - 1) instanceof BrewingStep.TimedStep expectedTimed) {
                progress = (double) timedStep.time().moment() / expectedTimed.time().moment();
            } else {
                progress = Math.min((double) timedStep.time().moment() / Moment.MINUTE * 3, 1);
            }
        } else {
            progress = 1D; // Shouldn't happen
        }
        List<ParticleDefinition> definitions = hot ? Config.config().cauldrons().cookParticleDefinitions() : Config.config().cauldrons().mixParticleDefinitions();
        definitions.stream()
                .filter(particleDefinition -> particleDefinition.range() == null || particleDefinition.range().isWithin(progress))
                .filter(particleDefinition -> particleDefinition.quality() == null || particleDefinition.quality().equals(matcherResult.quality().orElse(null)))
                .filter(particleDefinition -> particleDefinition.probability() > RANDOM.nextDouble())
                .map(ParticleDefinition::particleKey)
                .map(BukkitAdapter::toNamespacedKey)
                .filter(Objects::nonNull)
                .map(Registry.PARTICLE_TYPE::get)
                .filter(Objects::nonNull)
                .forEach(particle -> {
                    Class<?> dataType = particle.getDataType();
                    if (dataType == Void.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0);
                    } else if (dataType == Color.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, particleColor);
                    } else if (dataType == Particle.DustOptions.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, new Particle.DustOptions(particleColor, 1.6F));
                    } else if (dataType == BlockData.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, getBlock().getBlockData());
                    } else if (dataType == Float.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, 1F);
                    } else if (dataType == Particle.Spell.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, new Particle.Spell(particleColor, 1F));
                    } else if (dataType == ItemStack.class) {
                        // TODO: Take items from input ingredients (would require conversion)
                    } else if (dataType == Integer.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, 1);
                    } else if (dataType == Particle.Trail.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, new Particle.Trail(locationSupplier.get(), particleColor, 10));
                    } else if (dataType == Vibration.class) {
                        world.spawnParticle(particle, locationSupplier.get(), 0, new Vibration(new Vibration.Destination.BlockDestination(getBlock()), 10));
                    }
                });
    }

    public void playIngredientAddedEffects(ItemStack item, Player player) {
        long delay;
        if (Config.config().cauldrons().ingredientAddedAnimation() != AnimationDisplay.NONE) {
            delay = AnimationManager.playIngredientAddAnimation(item, player, getBlock().getLocation().toCenterLocation());
        } else {
            delay = 1;
        }
        BukkitAdapter.toLocation(this.location)
                .map(Location::toCenterLocation)
                .filter(Location::isChunkLoaded)
                .ifPresent(bukkitLocation -> Bukkit.getRegionScheduler().runDelayed(TheBrewingProject.getInstance(), bukkitLocation, ignored -> {
                    World world = bukkitLocation.getWorld();

                    SoundDefinition sound = item.getType() == Material.POTION ? Config.config().sounds().cauldronIngredientAddBrew() : Config.config().sounds().cauldronIngredientAdd();
                    SoundPlayer.playSoundEffect(sound, Sound.Source.BLOCK, bukkitLocation);

                    if (bukkitLocation.getBlock().getType() == Material.WATER_CAULDRON) {
                        world.spawnParticle(Particle.SPLASH, bukkitLocation.add(0.0, 0.5, 0.0), 50, 0.1, 0.05, 0.1, 1.0);
                    }
                }, delay));
    }

    public void playBrewExtractedEffects() {
        BukkitAdapter.toWorld(location).ifPresent(world ->
                SoundPlayer.playSoundEffect(
                        Config.config().sounds().cauldronBrewExtract(), Sound.Source.BLOCK,
                        world, location.x() + 0.5, location.y() + 1, location.z() + 0.5)
        );
    }

    public static boolean isHeatSource(Block block) {
        if (Config.config().cauldrons().heatSources().isEmpty()) {
            return true;
        }
        Material material = block.getType();
        if (!Config.config().cauldrons().heatSources().contains(BukkitAdapter.toMaterialHolder(material))) {
            return false;
        }
        if (material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE) {
            return BlockUtil.isLitCampfire(block);
        } else if (material == Material.LAVA) {
            return BlockUtil.isSource(block);
        }
        return true;
    }

    @Override
    public BreweryLocation position() {
        return location;
    }

    public Brew getUpdatedBrew() {
        recalculateBrewTime();
        return brew;
    }

    public ItemStack extractBrew(ItemSource itemSource) {
        this.brewExtracted = true;
        playBrewExtractedEffects();
        if (!(itemSource instanceof ItemSource.BrewBasedSource brewBasedSource)) {
            return itemSource.get();
        }
        return RecipeMatcherImpl.builder().build()
                .match(brewBasedSource.brew())
                .toItem(new Brew.State.Other(), previousDefaultRecipe);
    }

    private void recalculateBrewTime() {
        long time = TheBrewingProject.getInstance().getTime();
        if (hot) {
            brew = brew.withLastStep(BrewingStep.Cook.class,
                    cook -> cook.withBrewTime(cook.time().withLastStep(time)),
                    () -> new CookStepImpl(
                            new Interval(time, time),
                            Map.of(),
                            getType()
                                    .orElseThrow(() -> new IllegalStateException("Expected cauldron block type for cauldron"))
                    )
            );
        } else {
            brew = brew.withLastStep(BrewingStep.Mix.class,
                    mix -> mix.withTime(mix.time().withLastStep(time)),
                    () -> new MixStepImpl(new Interval(time, time), Map.of(),
                            getType().orElseThrow(() -> new IllegalStateException("Expected cauldron block type for cauldron")))
            );
        }
    }

    private BrewingStep newStep() {
        long time = TheBrewingProject.getInstance().getTime();
        if (hot) {
            return new CookStepImpl(
                    new Interval(time, time),
                    Map.of(),
                    getType()
                            .orElseThrow(() -> new IllegalStateException("Expected cauldron block type for cauldron"))
            );
        } else {
            return new MixStepImpl(
                    new Interval(time, time),
                    Map.of(),
                    getType().orElseThrow(() -> new IllegalStateException("Expected cauldron block type for cauldron"))
            );
        }
    }

    private Block getBlock() {
        return BukkitAdapter.toBlock(location).orElseThrow(() -> new IllegalStateException("Could not find world for cauldron"));
    }

    public static void incrementLevel(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Levelled levelled) {
            levelled.setLevel(Math.min(levelled.getLevel() + 1, levelled.getMaximumLevel()));
            block.setBlockData(levelled);
        } else {
            Levelled waterCauldron = BlockType.WATER_CAULDRON.createBlockData();
            waterCauldron.setLevel(1);
            block.setBlockData(waterCauldron);
        }
    }

    public boolean decrementLevel() {
        Block block = getBlock();
        if (!Tag.CAULDRONS.isTagged(block.getType())) {
            return true;
        }
        if (!(block.getBlockData() instanceof Levelled levelled)) {
            block.setType(Material.CAULDRON);
            return true;
        }
        if (levelled.getLevel() == 1) {
            block.setType(Material.CAULDRON);
            return true;
        }
        levelled.setLevel(levelled.getLevel() - 1);
        block.setBlockData(levelled);
        updateLevel(levelled);
        return false;
    }

    public void updateLevel(BlockData newLevelData) {
        if (waterColorer != null) {
            waterColorer.setTransformation(compileTransformation(newLevelData));
        }
    }

    @Override
    public long getTime() {
        if (brew.getCompletedSteps().isEmpty()) {
            return 0L;
        }
        if (brew.lastStep() instanceof BrewingStep.Cook cook) {
            return cook.time().moment();
        }
        if (brew.lastStep() instanceof BrewingStep.Mix mix) {
            return mix.time().moment();
        }
        return 0L;
    }

    @NonNull
    @Override
    public Optional<CauldronType> getType() {
        return Optional.of(cauldronType);
    }

    @Override
    public CauldronType getCauldronType() {
        return cauldronType;
    }

    @Override
    public int getLevel() {
        if (getBlock().getType() == Material.LAVA_CAULDRON) {
            return 1;
        }
        if (getBlock().getBlockData() instanceof Levelled levelled) {
            return levelled.getLevel();
        }
        return 0;
    }

    @Override
    public void destroy() {
        if (waterColorer != null) {
            runLocally(() -> waterColorer.remove());
        }
    }

    private Color convert(java.awt.Color awtColor) {
        return Color.fromRGB(awtColor.getRGB() & 0xFFFFFF);
    }

    public boolean isHot() {
        return this.hot;
    }

    public Brew getBrew() {
        return this.brew;
    }

    public void setHot(boolean hot) {
        this.hot = hot;
    }

    @Override
    public CompletableFuture<Void> runLocally(Runnable action) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        Bukkit.getRegionScheduler().run(TheBrewingProject.getInstance(), BukkitAdapter.toLocation(location).orElseThrow(), ignored -> {
            action.run();
            completableFuture.complete(null);
        });
        return completableFuture;
    }
}
