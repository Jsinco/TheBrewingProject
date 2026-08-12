package dev.jsinco.brewery.api.ingredient;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;

public enum AlternateIngredientState {
    /**
     * Ingredient was added too late, it's considered raw and will fail the whole brew
     */
    RAW("tbp.ingredient.state.raw");

    private final String translationKey;

    AlternateIngredientState(String translationKey) {
        this.translationKey = translationKey;
    }

    Component toComponent(Component originalName) {
        return Component.translatable(translationKey, Argument.component("ingredient", originalName));
    }
}
