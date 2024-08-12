package dev.jsinco.brewery;

import org.bukkit.plugin.java.JavaPlugin;

public class TheBrewingProject extends JavaPlugin {

    private static TheBrewingProject instance;

    @Override
    public void onLoad() {
        instance = this;
    }





    public static TheBrewingProject getInstance() {
        return instance;
    }
}