package dev.jsinco.brewery.recipes;

import lombok.Getter;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulation of a default recipe used when no recipe is found while brewing
 */
@Getter
public class DefaultRecipe {

    private final String name;
    private final List<String> lore;
    private final Color color;
    private final int customModelData;
    private final boolean glint;

    public DefaultRecipe(String name, List<String> lore, Color color, int customModelData, boolean glint) {
        this.name = name == null ? "Cauldron Brew" : name;
        this.lore = lore == null ? new ArrayList<>() : lore;
        this.color = color == null ? Color.BLUE : color;
        this.customModelData = customModelData;
        this.glint = glint;
    }


    public static class Builder {
        private String name = "Cauldron Brew";
        private List<String> lore = new ArrayList<>();
        private Color color = Color.BLUE;
        private int customModelData = -1;
        private boolean glint = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        public Builder customModelData(int customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public Builder glint(boolean glint) {
            this.glint = glint;
            return this;
        }

        public DefaultRecipe build() {
            return new DefaultRecipe(name, lore, color, customModelData, glint);
        }
    }
}
