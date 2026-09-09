package dev.jsinco.brewery.migrator

import com.google.common.base.Preconditions
import dev.jsinco.brewery.bukkit.TheBrewingProject
import dev.jsinco.brewery.migrator.migration.configuration.ConfigMigration
import dev.jsinco.brewery.migrator.migration.configuration.RecipeMigration
import dev.jsinco.brewery.migrator.migration.world.BarrelMigration
import dev.jsinco.brewery.migrator.migration.world.CauldronMigration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

class TbpMigratorPlugin : JavaPlugin() {
    private var loadSuccess = false
    private var hasMigrated = true

    override fun onLoad() {
        this.hasMigrated = loadMigrator()
        if (hasMigrated) {
            sendFailMessage()
            Bukkit.getServer().shutdown()
            return
        }
        RecipeMigration.migrateRecipes(
            File(dataFolder.parent, "BreweryX"),
            File(dataFolder.parent, "TheBrewingProject")
        )
        ConfigMigration.migrateConfig(
            File(dataFolder.parent, "BreweryX"),
            File(dataFolder.parent, "TheBrewingProject")
        )
        this.loadSuccess = true
    }

    private fun loadMigrator(): Boolean {
        val resource = "migration-state.properties"
        val file = File(dataFolder, resource)
        if (!file.exists()) {
            super.saveResource(resource, false)
        }
        val state = Properties()
        InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8).use {
            state.load(it)
            if (state["has-migrated"]?.equals("true") ?: false) {
                return true
            }
            state["has-migrated"] = "true"
        }
        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use {
            state.store(it, "This represents the state of the migration plugin")
        }
        return false
    }

    override fun onEnable() {
        if (hasMigrated) {
            return
        }
        Preconditions.checkState(loadSuccess, "Failed on load, check on load logs!")
        Bukkit.getGlobalRegionScheduler().run(this) { t ->
            Bukkit.getWorlds().forEach(BarrelMigration::migrateWorld)
            Bukkit.getWorlds().forEach(CauldronMigration::migrateWorld)
            TheBrewingProject.getInstance().database.flush().join()
            TheBrewingProject.getInstance().reload()
            val pluginManager = Bukkit.getPluginManager()
            sendSuccessMessage()
            pluginManager.disablePlugin(pluginManager.getPlugin("BreweryX")!!)
            pluginManager.disablePlugin(this)
        }
    }

    fun sendSuccessMessage() {
        val audience = Bukkit.getServer().consoleSender
        fun send(line: Component) = audience.sendMessage(line)
        send(Component.empty())
        send(
            Component.text("==============================================================")
                .color(NamedTextColor.GREEN)
        )
        send(Component.empty())
        send(
            Component.text("Successfully migrated all data from BreweryX to TheBrewingProject!")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
        )
        send(Component.empty())
        send(
            Component.text("Please remove both BreweryX and the migrator plugin and restart")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
        )
        send(Component.empty())
        send(
            Component.text("==============================================================")
                .color(NamedTextColor.GREEN)
        )
        send(Component.empty())
    }

    fun sendFailMessage() {
        val audience = Bukkit.getServer().consoleSender
        fun send(line: Component) = audience.sendMessage(line)
        send(Component.empty())
        send(
            Component.text("==============================================================")
                .color(NamedTextColor.YELLOW)
        )
        send(Component.empty())
        send(
            Component.text("The migration has already been run, shutting down the server!")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
        )
        send(Component.empty())
        send(
            Component.text("Please remove both BreweryX and the migrator plugin and restart")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
        )
        send(Component.empty())
        send(
            Component.text("==============================================================")
                .color(NamedTextColor.YELLOW)
        )
        send(Component.empty())
    }
}