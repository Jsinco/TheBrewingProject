package dev.jsinco.brewery;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class TheBrewingProject extends JavaPlugin {

    @Getter
    private static TheBrewingProject instance;

    @Override
    public void onLoad() {
        instance = this;
    }


}