package dev.jsinco.brewery.util;

import org.bukkit.Bukkit;

public final class Logging {

    public static void log(String m) {
        Bukkit.getConsoleSender().sendMessage(m);
    }

    public static void logError(String m, Throwable throwable) {
        Bukkit.getConsoleSender().sendMessage(m);
    }
}
