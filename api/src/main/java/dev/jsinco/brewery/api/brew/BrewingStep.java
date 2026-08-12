package dev.jsinco.brewery.api.brew;

import dev.jsinco.brewery.api.breweries.BarrelType;
import dev.jsinco.brewery.api.breweries.CauldronType;
import dev.jsinco.brewery.api.ingredient.Ingredient;
import dev.jsinco.brewery.api.moment.Moment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface BrewingStep {

    /**
     * Calculate how close this brewing step is to another brewing step.
     *
     * @param other The other step to compare to
     * @return A proximity index in the range [0, 1], where 1 is 100% match
     */
    Map<ScoreType, PartialBrewScore> proximityScores(BrewingStep other);

    /**
     * @return The type of the step
     */
    StepType stepType();

    /**
     * @param other Another brewing step
     * @return The maximum scores "other" can get for this brewing step
     */
    Map<ScoreType, PartialBrewScore> maximumScores(BrewingStep other);

    /**
     * @return The scores for a failed step of this type
     */
    Map<ScoreType, PartialBrewScore> failedScores();

    /**
     * All players who contributed to this brewing step, in their order of contribution.
     *
     * @return All brewers, may be empty
     */
    SequencedSet<UUID> brewers();

    /**
     * Example of incomplete steps is distilling step with 0 runs and age step with less than half
     * an aging year of aging.
     *
     * @return True if the step is completed
     */
    boolean isCompleted();

    /**
     *
     * @return The amount of times this step has been merged with another step of same type
     */
    int mergeCount();

    /**
     * @param other The other step to merge with
     * @return An optional value with a new merged step
     */
    Optional<BrewingStep> merge(BrewingStep other);

    /**
     * @param state    The state of the brew
     * @param resolver A tag resolver for this step
     * @return A translatable component for displaying this step
     */
    default Component infoDisplay(Brew.State state, TagResolver resolver) {
        return Component.translatable(switch (state) {
            case Brew.State.Other ignored -> "tbp.brew.tooltip." + stepType().name().toLowerCase(Locale.ROOT);
            case Brew.State.Seal ignored -> "tbp.brew.tooltip-sealed." + stepType().name().toLowerCase(Locale.ROOT);
            case Brew.State.Brewing ignored -> "tbp.brew.tooltip-brewing." + stepType().name().toLowerCase(Locale.ROOT);
        }, Argument.tagResolver(resolver));
    }

    /**
     * How the brew will outcome be handled, whenever the brew is incomplete
     *
     * @return The behavior for handling incomplete brews
     */
    IncompleteBehavior incompleteBehavior();

    interface TimedStep extends BrewingStep {
        /**
         * @return The time for this step (ticks)
         */
        Moment time();
    }

    interface IngredientsStep extends BrewingStep {
        /**
         * @return The ingredients for this step
         */
        Map<? extends Ingredient, Integer> ingredients();

        /**
         * @param ingredients The new ingredient content
         * @return A new ingredient step with the changed ingredients
         */
        IngredientsStep withIngredients(Map<? extends Ingredient, Integer> ingredients);
    }

    interface AuthoredStep<SELF extends AuthoredStep<SELF>> extends BrewingStep {

        /**
         * @param brewer A brewer's UUID
         * @return A new instance of this step with the specified brewer
         */
        default SELF withBrewer(UUID brewer) {
            if (brewers().contains(brewer)) {
                return (SELF) this;
            }
            return withBrewersReplaced(Stream.concat(
                    brewers().stream(),
                    Stream.of(brewer)
            ).collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        /**
         * @param brewers A collection of brewer UUIDs
         * @return A new instance of this step with the specified brewers
         */
        default SELF withBrewers(SequencedCollection<UUID> brewers) {
            return withBrewersReplaced(Stream.concat(
                    brewers().stream(),
                    brewers.stream()
            ).collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        SELF withBrewersReplaced(SequencedCollection<UUID> brewers);
    }

    interface CauldronStep<SELF extends CauldronStep<SELF>> extends TimedStep, IngredientsStep, AuthoredStep<SELF> {
        /**
         * @return The type of the cauldron, or null for non-first recipe steps
         */
        @Nullable CauldronType cauldronType();

        /**
         * @param ingredients A map of ingredients with amount
         * @return A new instance of this step with specified ingredients
         */
        CauldronStep<SELF> withIngredients(Map<? extends Ingredient, Integer> ingredients);

        /**
         * @param time A time (ticks)
         * @return A new instance of this step with specified time
         */
        CauldronStep<SELF> withTime(Moment time);

        default CauldronStep<SELF> withBrewTime(Moment time) {
            return withTime(time);
        }
    }

    interface Distill extends AuthoredStep<Distill> {

        /**
         * @return The amount of distill runs for this step
         */
        @Range(from = 0, to = Integer.MAX_VALUE)
        int runs();

        /**
         * @return A new instance of this step with distill runs incremented by 1
         */
        Distill incrementRuns();

        @Override
        default IncompleteBehavior incompleteBehavior() {
            return IncompleteBehavior.INCOMPLETE;
        }
    }

    interface Age extends TimedStep, AuthoredStep<Age> {

        /**
         * @return The type of the barrel
         */
        BarrelType barrelType();

        /**
         * @param age An aging time (ticks)
         * @return A new instance of this step with specified aging time
         */
        Age withAge(Moment age);

        @Override
        default IncompleteBehavior incompleteBehavior() {
            return IncompleteBehavior.INCOMPLETE;
        }
    }

    interface Mix extends CauldronStep<Mix> {

        @Override
        @Nullable CauldronType cauldronType();


        @Override
        Mix withIngredients(Map<? extends Ingredient, Integer> ingredients);


        @Override
        Mix withTime(Moment time);

        @Override
        default IncompleteBehavior incompleteBehavior() {
            return IncompleteBehavior.FAIL;
        }
    }

    interface Cook extends CauldronStep<Cook> {
        @Override
        @Nullable CauldronType cauldronType();


        @Override
        Cook withIngredients(Map<? extends Ingredient, Integer> ingredients);


        @Override
        Cook withTime(Moment time);

        default Cook withBrewTime(Moment time) {
            return withTime(time);
        }

        @Override
        default IncompleteBehavior incompleteBehavior() {
            return IncompleteBehavior.FAIL;
        }
    }


    enum StepType {
        COOK,
        DISTILL,
        AGE,
        MIX
    }

}
