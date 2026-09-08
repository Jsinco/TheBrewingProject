package dev.jsinco.brewery.bukkit.integration.item;

import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.integration.ItemIntegration;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackColors;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackSource;
import dev.jsinco.brewery.util.ClassUtil;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CraftEngineIntegration implements ItemIntegration, Listener {

    private static final boolean ENABLED = ClassUtil.exists("net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine");
    private final CompletableFuture<Void> itemsLoadedFuture = new CompletableFuture<>();
    private final ResourcePackColors resourcePackColors;

    public CraftEngineIntegration(ResourcePackColors resourcePackColors) {
        this.resourcePackColors = resourcePackColors;
    }

    public @Nullable String getItemId(ItemStack itemStack) {
        BukkitItem customItem = BukkitCraftEngine.instance().itemManager().wrap(itemStack);
        return customItem.customId()
                .map(Key::toString)
                .orElse(null);
    }

    @Override
    public @NonNull CompletableFuture<Void> initialized() {
        return itemsLoadedFuture;
    }

    @Override
    public Optional<ItemStack> createItem(String id) {
        return Optional.ofNullable(BukkitCraftEngine.instance().itemManager().createWrappedItem(Key.from(id), null))
                .map(BukkitItem::getBukkitItem);
    }

    @Override
    public boolean isIngredient(String id) {
        return Optional.ofNullable(BukkitCraftEngine.instance().itemManager().createWrappedItem(Key.from(id), null))
                .isPresent();
    }

    public @Nullable Component displayName(String id) {
        return createItem(id)
                .map(ItemStack::effectiveName)
                .orElse(null);

    }

    @Override
    public boolean isEnabled() {
        return ENABLED;
    }

    @Override
    public String getId() {
        return "craftengine";
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, TheBrewingProject.getInstance());
    }

    @Override
    public void onHotReload() {
        readGeneratedPack();
    }

    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        readGeneratedPack();
    }

    private void readGeneratedPack() {
        File target = Config.resourcePackPath().toFile();
        if (target.exists()) {
            resourcePackColors.addSource(new ResourcePackSource.FileResourcePackSource(target));
        }
        itemsLoadedFuture.complete(null);
    }

    @Override
    public @Nullable Color color(String id) {
        BukkitItem item = BukkitCraftEngine.instance().itemManager().createWrappedItem(Key.from(id), null);
        if (item == null) {
            return null;
        }
        return item.itemModel()
                .map(net.kyori.adventure.key.Key::key)
                .flatMap(key -> Optional.ofNullable(resourcePackColors.modelColor(key)))
                .or(() -> item.customModelData()
                        .flatMap(integer -> Optional.ofNullable(
                                resourcePackColors.customModelDataColor(net.kyori.adventure.key.Key.key(item.id().toString()), integer)
                        ))
                )
                .orElse(null);
    }
}
