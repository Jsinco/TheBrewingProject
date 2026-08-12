package dev.jsinco.brewery.bukkit.recipe;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.brew.BrewQuality;
import dev.jsinco.brewery.api.brew.BrewScore;
import dev.jsinco.brewery.api.brew.BrewingStep;
import dev.jsinco.brewery.api.brew.IncompleteBehavior;
import dev.jsinco.brewery.api.ingredient.Ingredient;
import dev.jsinco.brewery.api.ingredient.IngredientManager;
import dev.jsinco.brewery.api.recipe.DefaultRecipe;
import dev.jsinco.brewery.api.recipe.Recipe;
import dev.jsinco.brewery.api.recipe.RecipeMatcherResult;
import dev.jsinco.brewery.api.recipe.RecipeResult;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.brew.BrewImpl;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.brew.BrewAdapterAccess;
import dev.jsinco.brewery.bukkit.util.BukkitIngredientUtil;
import dev.jsinco.brewery.bukkit.util.BukkitMessageUtil;
import dev.jsinco.brewery.configuration.BrewTooltipType;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.configuration.DrunkenModifierSection;
import dev.jsinco.brewery.effect.DrunkStateImpl;
import dev.jsinco.brewery.recipes.RecipeRegistryImpl;
import dev.jsinco.brewery.util.BrewUtil;
import dev.jsinco.brewery.util.MessageUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.PotionContents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class RecipeMatcherResultImpl implements RecipeMatcherResult<ItemStack> {

    private final @Nullable Recipe<ItemStack> recipe;
    private final List<BrewingStep> matchingSteps;
    private final Brew brew;
    private final BrewScore score;

    public RecipeMatcherResultImpl(@Nullable Recipe<ItemStack> recipe, List<BrewingStep> matchingSteps, Brew brew, BrewScore brewScore) {
        this.recipe = recipe;
        this.matchingSteps = matchingSteps;
        this.brew = brew;
        this.score = brewScore;
    }

    @Override
    public ItemStack toItem(Brew.State state) {
        return toItem(state, quality().orElse(null));
    }

    @Override
    public ItemStack toItem(Brew.State state, @Nullable BrewQuality overrideQuality) {
        RecipeRegistryImpl<ItemStack> recipeRegistry = TheBrewingProject.getInstance().getRecipeRegistry();
        ItemStack itemStack;
        if (overrideQuality == null || recipe == null
                || (!score.completed()
                && recipe.getSteps().size() == matchingSteps.size()
                && recipe.getSteps().getLast().incompleteBehavior() == IncompleteBehavior.FAIL)
        ) {
            itemStack = fromDefaultRecipe(recipe, recipeRegistry, brew, state, true);
            itemStack.editPersistentDataContainer(pdc -> {
                pdc.set(BrewAdapterAccess.BREWERY_SCORE, PersistentDataType.DOUBLE, 0D);
            });
        } else if (!score.completed()) {
            Optional<DefaultRecipe<ItemStack>> defaultRecipeOptional = BrewAdapterAccess.getDefaultRecipe(recipe, recipeRegistry, brew, false);
            itemStack = defaultRecipeOptional
                    .map(DefaultRecipe::result)
                    .map(RecipeResult::newLorelessItem)
                    .orElse(incompletePotion(brew));
            defaultRecipeOptional.map(DefaultRecipe::result)
                    .ifPresent(result -> applyLore(itemStack, result, state));
        } else {
            RecipeResult<ItemStack> recipeResult = recipe.getRecipeResult(overrideQuality);
            itemStack = recipeResult.newLorelessItem();
            applyLore(itemStack, recipeResult, state);
            itemStack.editPersistentDataContainer(pdc -> BrewAdapterAccess.applyBrewTags(
                    pdc,
                    recipe,
                    Objects.equals(quality().orElse(null), overrideQuality) ? score.score() : BrewQuality.maxScore(overrideQuality),
                    MiniMessage.miniMessage().serialize(recipeResult.displayName())
            ));
        }
        if (!(state instanceof BrewImpl.State.Seal)) {
            itemStack.editPersistentDataContainer(pdc ->
                    BrewAdapterAccess.applyBrewData(pdc, brew)
            );
        }
        return itemStack;
    }

    @Override
    public ItemStack toItem(Brew.State state, @Nullable DefaultRecipe<ItemStack> preferredDefaultRecipe) {
        if (recipe == null || score.completed() || preferredDefaultRecipe == null) {
            return toItem(state);
        }
        List<DefaultRecipe<ItemStack>> defaultRecipes = BrewAdapterAccess.getPossibleDefaultRecipes(
                recipe,
                TheBrewingProject.getInstance().getRecipeRegistry(),
                brew,
                false
        );
        Optional<DefaultRecipe<ItemStack>> previous = defaultRecipes.stream()
                .filter(preferredDefaultRecipe::equals)
                .findAny();
        Optional<DefaultRecipe<ItemStack>> bestMatch = defaultRecipes.stream()
                .max(Comparator.comparingInt(DefaultRecipe::complexity));
        Optional<DefaultRecipe<ItemStack>> defaultRecipe;
        if (previous.isPresent()) {
            defaultRecipe = previous.get().complexity() < bestMatch.get().complexity() ? bestMatch : previous;
        } else {
            defaultRecipe = bestMatch;
        }
        ItemStack itemStack = defaultRecipe
                .map(DefaultRecipe::result)
                .map(RecipeResult::newLorelessItem)
                .orElse(incompletePotion(brew));
        defaultRecipe.map(DefaultRecipe::result)
                .ifPresent(result -> applyLore(itemStack, result, state));
        if (!(state instanceof BrewImpl.State.Seal)) {
            itemStack.editPersistentDataContainer(pdc ->
                    BrewAdapterAccess.applyBrewData(pdc, brew)
            );
        }
        return itemStack;
    }

    @Override
    public ItemStack toLorelessItem(Brew.State state) {
        return toLorelessItem(state, quality().orElse(null));
    }

    @Override
    public ItemStack toLorelessItem(Brew.State state, @Nullable BrewQuality overrideQuality) {
        RecipeRegistryImpl<ItemStack> recipeRegistry = TheBrewingProject.getInstance().getRecipeRegistry();
        ItemStack itemStack;
        if (overrideQuality == null || recipe == null) {
            itemStack = fromDefaultRecipe(recipe, recipeRegistry, brew, state, true);
            itemStack.editPersistentDataContainer(pdc -> {
                pdc.set(BrewAdapterAccess.BREWERY_SCORE, PersistentDataType.DOUBLE, 0D);
            });
        } else if (!score.completed()) {
            Optional<DefaultRecipe<ItemStack>> defaultRecipeOptional = BrewAdapterAccess.getDefaultRecipe(recipe, recipeRegistry, brew, false);
            itemStack = defaultRecipeOptional
                    .map(DefaultRecipe::result)
                    .map(RecipeResult::newLorelessItem)
                    .orElse(incompletePotion(brew));
        } else {
            RecipeResult<ItemStack> recipeResult = recipe.getRecipeResult(overrideQuality);
            itemStack = recipeResult.newLorelessItem();
            itemStack.editPersistentDataContainer(pdc -> BrewAdapterAccess.applyBrewTags(
                    pdc,
                    recipe,
                    Objects.equals(quality().orElse(null), overrideQuality) ? score.score() : BrewQuality.maxScore(overrideQuality),
                    MiniMessage.miniMessage().serialize(recipeResult.displayName())
            ));
        }
        if (!(state instanceof BrewImpl.State.Seal)) {
            itemStack.editPersistentDataContainer(pdc ->
                    BrewAdapterAccess.applyBrewData(pdc, brew)
            );
        }
        return itemStack;
    }

    @Override
    public Optional<Recipe<ItemStack>> recipeMatch() {
        return Optional.ofNullable(recipe);
    }

    @Override
    public Optional<BrewQuality> quality() {
        return Optional.ofNullable(score.brewQuality());
    }

    @Override
    public BrewScore score() {
        return score;
    }

    @Override
    public Optional<RecipeResult<ItemStack>> recipeResult() {
        BrewQuality brewQuality = quality().orElse(null);
        if (recipe != null && brewQuality != null) {
            return Optional.of(recipe.getRecipeResult(brewQuality));
        }
        return Optional.empty();
    }

    @Override
    public List<BrewingStep> matchingSteps() {
        return matchingSteps;
    }

    private static ItemStack incompletePotion(Brew brew) {
        ItemStack itemStack = new ItemStack(Material.POTION);
        BrewAdapterAccess.hideTooltips(itemStack);
        Map<Ingredient, Integer> ingredients = new HashMap<>();
        for (BrewingStep brewingStep : brew.getCompletedSteps()) {
            if (brewingStep instanceof BrewingStep.Cook cook) {
                IngredientManager.merge(ingredients, (Map<Ingredient, Integer>) cook.ingredients());
            }
            if (brewingStep instanceof BrewingStep.Mix mix) {
                IngredientManager.merge(ingredients, (Map<Ingredient, Integer>) mix.ingredients());
            }
        }
        Pair<Color, Ingredient> itemsInfo = BukkitIngredientUtil.ingredientData(ingredients);
        Ingredient topIngredient = itemsInfo.second();
        final Map<BrewingStep.StepType, String> displayNameByStep = Map.of(
                BrewingStep.StepType.COOK, "unfinished-fermented",
                BrewingStep.StepType.DISTILL, "unfinished-distilled",
                BrewingStep.StepType.AGE, "unfinished-aged",
                BrewingStep.StepType.MIX, "unfinished-mixed"
        );

        BrewingStep.StepType lastStep = brew.getCompletedSteps().getLast().stepType();
        String translationKey = "tbp.brew.display-name." + displayNameByStep.get(lastStep);
        Component displayName = topIngredient == null
                ? Component.translatable(translationKey + "-unknown")
                : Component.translatable(translationKey, Argument.tagResolver(Placeholder.component("ingredient", topIngredient.displayName())));

        itemStack.setData(DataComponentTypes.CUSTOM_NAME, GlobalTranslator
                .render(displayName, Config.config().language()).decoration(TextDecoration.ITALIC, false));
        itemStack.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents()
                .customColor(itemsInfo.first()).build());
        return itemStack;
    }


    private ItemStack fromDefaultRecipe(@Nullable Recipe<ItemStack> recipe, RecipeRegistryImpl<ItemStack> recipeRegistry, Brew brew, Brew.State state, boolean ruinedOnly) {
        Optional<DefaultRecipe<ItemStack>> defaultRecipe = BrewAdapterAccess.getDefaultRecipe(recipe, recipeRegistry, brew, ruinedOnly);
        if (defaultRecipe.isEmpty()) {
            ItemStack itemStack = new ItemStack(Material.POTION);
            itemStack.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Placeholder"));
            itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(
                    List.of(Component.text("you don't have any default/incomplete recipes!"),
                            Component.text("Contact admin, or if you're admin look into incomplete-recipes.yml"))
            ));
            return itemStack;
        }
        RecipeResult<ItemStack> recipeResult = defaultRecipe.get().result();
        ItemStack itemStack = recipeResult.newLorelessItem();
        applyLore(itemStack, recipeResult, state);
        return itemStack;
    }

    private void applyLore(ItemStack itemStack, RecipeResult<ItemStack> recipeResult, Brew.State state) {
        Stream.Builder<Component> fullLoreBuilder = Stream.builder();
        TagResolver resolver = TagResolver.resolver(MessageUtil.recipeEffectsResolver(recipeResult.recipeEffects()), MessageUtil.brewScoreResolver(score));
        for (BrewTooltipType tooltipType : Config.config().brewTooltipOrder()) {
            if (!recipeResult.appendBrewInfoLore() && BrewTooltipType.RECIPE_LORE != tooltipType) {
                continue;
            }
            switch (tooltipType) {
                case RECIPE_LORE -> recipeResult.lore().stream()
                        .map(line -> MessageUtil.miniMessage(line, MessageUtil.getScoreTagResolver(score)))
                        .forEach(fullLoreBuilder);
                case SCORE -> {
                    switch (state) {
                        case Brew.State.Brewing() ->
                                fullLoreBuilder.add(Component.translatable("tbp.brew.tooltip.quality-brewing", Argument.tagResolver(resolver)));
                        case Brew.State.Other() ->
                                fullLoreBuilder.add(Component.translatable("tbp.brew.tooltip.quality", Argument.tagResolver(resolver)));
                        case Brew.State.Seal(String ignored) ->
                                fullLoreBuilder.add(Component.translatable("tbp.brew.tooltip.quality-sealed", Argument.tagResolver(resolver)));
                    }
                }
                case MODIFIER -> applyDrunkenTooltips(state, fullLoreBuilder, resolver, recipeResult);
                case SEALED_TEXT -> {
                    if (state instanceof Brew.State.Seal(String message) && message != null) {
                        fullLoreBuilder.add(Component.translatable("tbp.brew.tooltip.volume", Argument.tagResolver(
                                TagResolver.resolver(resolver, Placeholder.parsed("volume", message)))
                        ));
                    }
                }
                case BREWERS -> applyBrewersTooltip(brew, fullLoreBuilder);
                case STEPS -> {
                    switch (state) {
                        case Brew.State.Brewing ignored -> {
                            MessageUtil.compileBrewInfo(matchingSteps, score, false).forEach(fullLoreBuilder::add);
                        }
                        case Brew.State.Other ignored -> {
                            addLastStepLore(fullLoreBuilder, score, state);
                        }
                        case Brew.State.Seal ignored -> {
                            addLastStepLore(fullLoreBuilder, score, state);
                        }
                    }
                }
                case EMPTY_LINE -> fullLoreBuilder.add(Component.empty());
            }
        }
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(
                fullLoreBuilder.build()
                        .map(component -> component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                        .map(component -> component.colorIfAbsent(NamedTextColor.GRAY))
                        .map(component -> GlobalTranslator.render(component, Config.config().language()))
                        .toList()
        ));
    }

    private void addLastStepLore(Stream.Builder<Component> streamBuilder, BrewScore score, Brew.State state) {
        int lastIndex = matchingSteps.size() - 1;
        BrewingStep lastCompleted = matchingSteps.getLast();
        streamBuilder.add(lastCompleted.infoDisplay(state,
                        MessageUtil.getBrewStepTagResolver(
                                lastCompleted,
                                score.getPartialScores(lastIndex),
                                score.brewDifficulty(),
                                BrewUtil.hasPreviousIngredientStep(brew.getCompletedSteps(), lastIndex))
                )
        );
    }

    private void applyDrunkenTooltips(Brew.State state, Stream.Builder<Component> streamBuilder, TagResolver resolver, RecipeResult<ItemStack> recipeResult) {
        DrunkenModifierSection.modifiers().drunkenTooltips()
                .stream()
                .filter(modifierTooltip -> modifierTooltip.filter().evaluate(DrunkStateImpl.compileVariables(recipeResult.recipeEffects().getModifiers(), null, 0D)) > 0)
                .map(modifierTooltip -> modifierTooltip.getTooltip(state))
                .filter(Objects::nonNull)
                .map(miniMessage -> MessageUtil.miniMessage(miniMessage, resolver))
                .forEach(streamBuilder::add);
    }


    private void applyBrewersTooltip(Brew brew, Stream.Builder<Component> streamBuilder) {
        Collection<UUID> brewers = switch (Config.config().brewersDisplay()) {
            case NONE -> List.of();
            case FIRST_STEP -> brew.getCompletedSteps().stream().findFirst()
                    .map(BrewingStep::brewers)
                    .orElseGet(LinkedHashSet::new);
            case LEAD_BREWER -> brew.leadBrewer().stream().toList();
            case LAST_STEP -> brew.lastCompletedStep()
                    .brewers();
            case ALL -> brew.getBrewers();
        };
        if (!brewers.isEmpty()) {
            streamBuilder.add(
                    MessageUtil.translated("tbp.brew.tooltip.brewer",
                            Placeholder.component("brewers", brewers.stream()
                                    .map(BukkitMessageUtil::uuidToPlayerName)
                                    .collect(Component.toComponent()))
                    ));
        }
    }
}
