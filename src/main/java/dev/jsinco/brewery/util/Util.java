package dev.jsinco.brewery.util;

import org.bukkit.Material;

public final class Util {



    public static int getInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static Material getMaterial(String material) {
        return Material.matchMaterial(material);
    }
}
