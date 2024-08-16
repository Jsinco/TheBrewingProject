package dev.jsinco.brewery.util;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public final class Util {


    private static final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";


    public static String color(String msg) {
        if (msg == null) return null;
        String[] texts = msg.split(String.format(WITH_DELIMITER, "&"));

        StringBuilder finalText = new StringBuilder();

        for (int i = 0; i < texts.length; i++) {
            if (texts[i].equalsIgnoreCase("&")) {
                //get the next string
                i++;
                if (texts[i].charAt(0) == '#') {
                    finalText.append(net.md_5.bungee.api.ChatColor.of(texts[i].substring(0, 7))).append(texts[i].substring(7));
                } else {
                    finalText.append(ChatColor.translateAlternateColorCodes('&', "&" + texts[i]));
                }
            } else {
                finalText.append(texts[i]);
            }
        }
        return finalText.toString();
    }


    // Returns a color closer to the destination color based on the interval and totalDuration
    public static Color getNextColor(Color current, Color destination, int step, int duration) {
        float ratio = (float) step / (duration - 1);
        int red = (int) (current.getRed() + ratio * (destination.getRed() - current.getRed()));
        int green = (int) (current.getGreen() + ratio * (destination.getGreen() - current.getGreen()));
        int blue = (int) (current.getBlue() + ratio * (destination.getBlue() - current.getBlue()));

        return Color.fromRGB(red, green, blue);
    }

    public static int getInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Nullable
    public static <E extends Enum<E>> E getEnumByName(Class<E> enumClass, String name) {
        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
