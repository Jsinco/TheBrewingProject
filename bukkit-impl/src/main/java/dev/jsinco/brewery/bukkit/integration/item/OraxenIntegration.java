package dev.jsinco.brewery.bukkit.integration.item;

import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.integration.ItemIntegration;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackColors;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackSource;
import dev.jsinco.brewery.util.ClassUtil;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.items.ItemBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OraxenIntegration implements ItemIntegration, Listener {

    private static final boolean ENABLED = ClassUtil.exists("io.th0rgal.oraxen.api.OraxenItems");
    private final CompletableFuture<Void> itemsLoadedFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> packGeneratedFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> initializedFuture = CompletableFuture.allOf(itemsLoadedFuture, packGeneratedFuture);
    private final ResourcePackColors resourcePackColors;

    public OraxenIntegration(ResourcePackColors resourcePackColors) {
        this.resourcePackColors = resourcePackColors;
    }

    @Override
    public Optional<ItemStack> createItem(String id) {
        return Optional.ofNullable(OraxenItems.getItemById(id))
                .map(ItemBuilder::build);
    }

    @Override
    public boolean isIngredient(String id) {
        return OraxenItems.getItemById(id) != null;
    }

    public @Nullable Component displayName(String oraxenId) {
        return OraxenItems.getOptionalItemById(oraxenId)
                .map(ItemBuilder::getDisplayName)
                .map(Component::text)
                .orElse(null);
    }

    @Override
    public @Nullable String getItemId(ItemStack itemStack) {
        return OraxenItems.getIdByItem(itemStack);
    }

    @Override
    public @NonNull CompletableFuture<Void> initialized() {
        return initializedFuture;
    }

    @Override
    public boolean isEnabled() {
        return ENABLED;
    }

    @Override
    public String getId() {
        return "oraxen";
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, TheBrewingProject.getInstance());
    }

    @Override
    public void onHotReload() {
        // TODO: Assume pack location and load resource pack colors here
        itemsLoadedFuture.complete(null);
        packGeneratedFuture.complete(null);
    }

    @EventHandler
    public void onOraxenItemsLoaded(OraxenItemsLoadedEvent event) {
        itemsLoadedFuture.complete(null);
    }

    @EventHandler
    public void onPackGenerate(OraxenPackGeneratedEvent event) {
        event.getOutput()
                .stream()
                .map(virtualFile -> new ResourcePackSource.InputStreamResourcePackSource(virtualFile::getInputStream, virtualFile.getPath()))
                .forEach(resourcePackColors::addSource);
        packGeneratedFuture.complete(null);
    }

    @Override
    public @Nullable Color color(String id) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) {
            return null;
        }
        Key key = builder.getItemModel();
        if (key == null) {
            return null;
        }
        return resourcePackColors.modelColor(key);
    }
}
