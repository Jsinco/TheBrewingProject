package dev.jsinco.brewery.util;

import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Timer;
import java.util.TimerTask;

public final class Util {


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

    public static Material getMaterial(String material) {
        return Material.matchMaterial(material);
    }
}
