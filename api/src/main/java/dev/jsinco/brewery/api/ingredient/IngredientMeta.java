package dev.jsinco.brewery.api.ingredient;

import dev.jsinco.brewery.api.serialize.EnumSerializer;
import dev.jsinco.brewery.api.serialize.Serializer;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.BreweryKeyed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Range;

public record IngredientMeta<T>(BreweryKey key, Serializer<T> serializer) implements BreweryKeyed {

    /**
     * A score for the ingredient within the range 0 to 1
     */
    @Range(from = 0, to = 1)
    public static IngredientMeta<Double> SCORE = new IngredientMeta<>(
            BreweryKey.parse("score"),
            Serializer.compile(String::valueOf, Double::parseDouble, object -> object instanceof Double aDouble && aDouble >= 0D && aDouble <= 1D)
    );
    /**
     * The display name to use for this ingredient, if none is present, then it will use default display names
     */
    public static IngredientMeta<Component> DISPLAY_NAME = new IngredientMeta<>(
            BreweryKey.parse("display_name"),
            Serializer.fork(MiniMessage.miniMessage()::serialize, MiniMessage.miniMessage()::deserialize, Component.class::isInstance,
                    new Serializer.StringMetaSerializer())
    );

    /**
     * Alternate indications for ingredients, for example ingredient was added too late; "it's fucking raw" (Gordon Ramsay)
     */
    public static IngredientMeta<AlternateIngredientState> ALTERNATE_STATE = new IngredientMeta<>(
            BreweryKey.parse("alternate_state"),
            new EnumSerializer<>(AlternateIngredientState.class)
    );
}
