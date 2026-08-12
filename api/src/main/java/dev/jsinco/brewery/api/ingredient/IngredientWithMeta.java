package dev.jsinco.brewery.api.ingredient;

import com.google.common.base.Preconditions;
import dev.jsinco.brewery.api.util.BreweryKey;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record IngredientWithMeta(Ingredient ingredient,
                                 Map<IngredientMeta<?>, Object> meta) implements Ingredient {

    public IngredientWithMeta {
        for (Map.Entry<IngredientMeta<?>, Object> entry : meta.entrySet()) {
            Preconditions.checkArgument(entry.getKey().serializer().appliesTo(entry.getValue()), "Invalid meta ingredient data '" + entry.getKey().key().minimalized() + "' for: " + entry.getValue());
        }
    }

    @Override
    public @NonNull BreweryKey key() {
        return ingredient.key();
    }

    @Override
    public @NonNull Component displayName() {
        Component override = get(IngredientMeta.DISPLAY_NAME);
        Component output;
        if (override == null) {
            output = ingredient.displayName();
        } else {
            output = override;
        }
        if (get(IngredientMeta.ALTERNATE_STATE) instanceof AlternateIngredientState alternateIngredientState) {
            output = alternateIngredientState.toComponent(output);
        }
        return output;
    }

    @Override
    public Optional<? extends Ingredient> findMatch(Set<BaseIngredient> baseIngredientSet) {
        return ingredient.findMatch(baseIngredientSet)
                .map(this::applyTo);
    }

    @Override
    public BaseIngredient toBaseIngredient() {
        return ingredient.toBaseIngredient();
    }

    /**
     * Wrap this ingredient with an ingredient with meta instance
     *
     * @param ingredient Ingredient to wrap
     * @return Ingredient with meta wrapping the ingredient
     */
    public IngredientWithMeta applyTo(Ingredient ingredient) {
        return new IngredientWithMeta(ingredient, meta);
    }

    /**
     * @param metaKey ingredientMetaKey
     * @param <T>     Ingredient meta value type
     * @return Ingredient meta value, or null if not present
     */
    public <T> @Nullable T get(IngredientMeta<T> metaKey) {
        return (T) meta.get(metaKey);
    }

    /**
     * @param metaKey      ingredient meta key
     * @param defaultValue the default value
     * @param <T>          Ingredient meta value type
     * @return The ingredient meta value, or the default value if not present
     */
    public <T> @Nullable T getOrDefault(IngredientMeta<T> metaKey, T defaultValue) {
        return (T) meta.getOrDefault(metaKey, defaultValue);
    }

    public <T> IngredientWithMeta withMeta(IngredientMeta<T> ingredientMeta, T value) {
        Preconditions.checkArgument(ingredientMeta.serializer().appliesTo(value), "Invalid ingredient meta value: %s".formatted(value));
        Map<IngredientMeta<?>, Object> all = new HashMap<>(meta);
        all.put(ingredientMeta, value);
        return new IngredientWithMeta(this.ingredient, Map.copyOf(all));
    }
}
