package dev.jsinco.brewery.util;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Util {


    private static final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";
    public static final Map<String, Color> NAME_TO_COLOR_MAP = new HashMap<>();
    static {
        NAME_TO_COLOR_MAP.put("WHITE", Color.WHITE);
        NAME_TO_COLOR_MAP.put("SILVER", Color.SILVER);
        NAME_TO_COLOR_MAP.put("GRAY", Color.GRAY);
        NAME_TO_COLOR_MAP.put("BLACK", Color.BLACK);
        NAME_TO_COLOR_MAP.put("RED", Color.RED);
        NAME_TO_COLOR_MAP.put("MAROON", Color.MAROON);
        NAME_TO_COLOR_MAP.put("YELLOW", Color.YELLOW);
        NAME_TO_COLOR_MAP.put("OLIVE", Color.OLIVE);
        NAME_TO_COLOR_MAP.put("LIME", Color.LIME);
        NAME_TO_COLOR_MAP.put("GREEN", Color.GREEN);
        NAME_TO_COLOR_MAP.put("AQUA", Color.AQUA);
        NAME_TO_COLOR_MAP.put("TEAL", Color.TEAL);
        NAME_TO_COLOR_MAP.put("BLUE", Color.BLUE);
        NAME_TO_COLOR_MAP.put("NAVY", Color.NAVY);
        NAME_TO_COLOR_MAP.put("FUCHSIA", Color.FUCHSIA);
        NAME_TO_COLOR_MAP.put("PURPLE", Color.PURPLE);
        NAME_TO_COLOR_MAP.put("ORANGE", Color.ORANGE);
    }


    public static String colorText(String msg) {
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


    public static Color parseColorString(String hexOrValue) {
        hexOrValue = hexOrValue.replace("&", "").replace("#", "").toUpperCase();
        if (NAME_TO_COLOR_MAP.containsKey(hexOrValue)) {
            return NAME_TO_COLOR_MAP.get(hexOrValue);
        }

        return Color.fromRGB(
                Integer.valueOf(hexOrValue.substring( 1, 3 ), 16),
                Integer.valueOf(hexOrValue.substring( 3, 5 ), 16),
                Integer.valueOf(hexOrValue.substring( 5, 7 ), 16));
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
        if (name == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static <T> T getRandomElement(List<T> list) {
        return list.get((int) (Math.random() * list.size()));
    }
}
