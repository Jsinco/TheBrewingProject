package dev.jsinco.brewery.brew;

import dev.jsinco.brewery.api.brew.BrewingStep;
import dev.jsinco.brewery.api.brew.PartialBrewScore;
import dev.jsinco.brewery.api.brew.ScoreType;
import dev.jsinco.brewery.api.breweries.CauldronType;
import dev.jsinco.brewery.api.ingredient.Ingredient;
import dev.jsinco.brewery.api.moment.Moment;
import dev.jsinco.brewery.api.moment.PassedMoment;
import dev.jsinco.brewery.util.BrewUtil;
import dev.jsinco.brewery.util.CollectionUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record MixStepImpl(Moment time, Map<? extends Ingredient, Integer> ingredients,
                          @Nullable CauldronType cauldronType, SequencedSet<UUID> brewers,
                          int mergeCount) implements BrewingStep.Mix {

    private static final Map<ScoreType, PartialBrewScore> BREW_STEP_MISMATCH = Stream.of(
            new PartialBrewScore(0, ScoreType.TIME),
            new PartialBrewScore(0, ScoreType.INGREDIENTS)
    ).collect(Collectors.toUnmodifiableMap(PartialBrewScore::type, partial -> partial));

    public MixStepImpl(Moment time, Map<? extends Ingredient, Integer> ingredients, CauldronType cauldronType) {
        this(time, ingredients, cauldronType, Collections.emptySortedSet(), 1);
    }

    @Override
    public MixStepImpl withIngredients(Map<? extends Ingredient, Integer> ingredients) {
        return new MixStepImpl(this.time, ingredients, this.cauldronType, this.brewers, this.mergeCount);
    }

    @Override
    public Map<ScoreType, PartialBrewScore> proximityScores(BrewingStep other) {
        if (!(other instanceof MixStepImpl(
                Moment otherTime, Map<? extends Ingredient, Integer> otherIngredients,
                CauldronType otherType, SequencedSet<UUID> ignored, int ignored2
        ))) {
            return BREW_STEP_MISMATCH;
        }
        double cauldronTypeScore = (cauldronType == null || otherType == null) ? 1D : cauldronType.appliesTo(otherType) ? 1D : 0D;
        double timeScore = Math.sqrt(BrewingStepUtil.nearbyValueScore(this.time.moment(), otherTime.moment()));
        double ingredientsScore = BrewingStepUtil.getIngredientsScore((Map<Ingredient, Integer>) this.ingredients, (Map<Ingredient, Integer>) otherIngredients);
        return Stream.of(
                new PartialBrewScore(cauldronTypeScore * timeScore, ScoreType.TIME),
                new PartialBrewScore(ingredientsScore, ScoreType.INGREDIENTS)
        ).collect(Collectors.toUnmodifiableMap(PartialBrewScore::type, partial -> partial));
    }

    @Override
    public StepType stepType() {
        return StepType.MIX;
    }

    @Override
    public Map<ScoreType, PartialBrewScore> maximumScores(BrewingStep other) {
        if (!(other instanceof MixStepImpl(
                Moment ignored1, Map<? extends Ingredient, Integer> otherIngredients,
                CauldronType otherType, SequencedSet<UUID> ignored2, int ignored3
        ))) {
            return BREW_STEP_MISMATCH;
        }
        double cauldronTypeScore = (cauldronType == null || otherType == null) ? 1D : cauldronType.appliesTo(otherType) ? 1D : 0D;
        double timeScore = 1D;
        double ingredientsScore = BrewingStepUtil.getIngredientsScore((Map<Ingredient, Integer>) this.ingredients, (Map<Ingredient, Integer>) otherIngredients);
        return Stream.of(
                new PartialBrewScore(cauldronTypeScore * timeScore, ScoreType.TIME),
                new PartialBrewScore(ingredientsScore, ScoreType.INGREDIENTS)
        ).collect(Collectors.toUnmodifiableMap(PartialBrewScore::type, partial -> partial));
    }

    @Override
    public Map<ScoreType, PartialBrewScore> failedScores() {
        return BREW_STEP_MISMATCH;
    }

    @Override
    public boolean isCompleted() {
        return true;
    }

    @Override
    public int mergeCount() {
        return this.mergeCount > 0 ? this.mergeCount : 1;
    }

    @Override
    public Optional<BrewingStep> merge(BrewingStep otherObject) {
        if (!(otherObject instanceof BrewingStep.Mix other)) {
            return Optional.empty();
        }
        if (other.cauldronType() != this.cauldronType()) {
            return Optional.empty();
        }
        long newElapsedTime = (other.time().moment() * other.mergeCount() + this.time().moment() * this.mergeCount) / (other.mergeCount() + this.mergeCount);
        Map<Ingredient, Integer> newIngredients = BrewUtil.averageIngredients(
                this.ingredients,
                other.ingredients(),
                this.mergeCount,
                other.mergeCount()
        );
        SequencedSet<UUID> newBrewers = new LinkedHashSet<>(this.brewers);
        newBrewers.addAll(other.brewers());
        return Optional.of(new MixStepImpl(
                new PassedMoment(newElapsedTime),
                newIngredients,
                cauldronType,
                newBrewers,
                this.mergeCount + other.mergeCount()
        ));
    }

    @Override
    public MixStepImpl withTime(Moment time) {
        return new MixStepImpl(time, this.ingredients, this.cauldronType, this.brewers, this.mergeCount);
    }

    @Override
    public Mix withBrewersReplaced(SequencedCollection<UUID> brewers) {
        return new MixStepImpl(this.time, this.ingredients, this.cauldronType, new LinkedHashSet<>(brewers), this.mergeCount);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MixStepImpl(
                Moment otherTime, Map<? extends Ingredient, Integer> otherIngredients,
                CauldronType otherType, SequencedSet<UUID> otherBrewers, int otherMergeCount
        ))) {
            return false;
        }
        return Objects.equals(time, otherTime)
                && Objects.equals(ingredients, otherIngredients)
                && cauldronType == otherType
                && CollectionUtil.isEqualWithOrdering(brewers, otherBrewers)
                && mergeCount == otherMergeCount;
    }
}
