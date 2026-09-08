package dev.jsinco.brewery.bukkit.integration.item;

import dev.jsinco.brewery.api.ingredient.Ingredient;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.ingredient.PluginIngredient;
import dev.jsinco.brewery.bukkit.api.integration.ItemIntegration;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackColors;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackSource;
import dev.jsinco.brewery.util.ClassUtil;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ItemsAdderIntegration implements ItemIntegration, Listener {

    private static final boolean ENABLED = ClassUtil.exists("dev.lone.itemsadder.api.CustomStack");
    private final CompletableFuture<Void> initializedFuture = new CompletableFuture<>();
    private final ResourcePackColors resourcePackColors;

    public ItemsAdderIntegration(ResourcePackColors resourcePackColors) {
        this.resourcePackColors = resourcePackColors;
    }

    @Override
    public Optional<ItemStack> createItem(String id) {
        return Optional.ofNullable(CustomStack.getInstance(id))
                .map(CustomStack::getItemStack);
    }

    @Override
    public boolean isIngredient(String id) {
        return CustomStack.getInstance(id) != null;
    }

    public @Nullable Component displayName(String itemsAdderId) {
        CustomStack customStack = CustomStack.getInstance(itemsAdderId);
        return customStack == null ? null : customStack.getItemStack().effectiveName();
    }

    @Override
    public @Nullable String getItemId(ItemStack itemStack) {
        CustomStack customStack = CustomStack.byItemStack(itemStack);
        if (customStack == null) {
            return null;
        }
        return customStack.getNamespacedID();
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
        return "itemsadder";
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, TheBrewingProject.getInstance());
    }

    @Override
    public Optional<Ingredient> createIngredientUnsafe(String id) {
        CustomStack customStack = CustomStack.getInstance(id);
        if (customStack == null) {
            return Optional.empty();
        }
        return Optional.of(new PluginIngredient(new BreweryKey(getId(), customStack.getNamespacedID()), this));
    }

    @Override
    public void onHotReload() {
        readGeneratedPack();
    }

    @EventHandler
    public void onItemsAdderItemsLoad(ItemsAdderLoadDataEvent loadDataEvent) {
        readGeneratedPack();
    }

    private void readGeneratedPack() {
        File resourcePack = new File(Bukkit.getPluginsFolder(), "ItemsAdder/output/generated.zip");
        if (resourcePack.exists() && resourcePack.isFile()) {
            resourcePackColors.addSource(new ResourcePackSource.FileResourcePackSource(resourcePack));
        }
        initializedFuture.complete(null);
    }

    @Override
    public @Nullable Color color(String id) {
        CustomStack customStack = CustomStack.getInstance(id);
        if (customStack == null) {
            return null;
        }
        String modelPath = customStack.getModelPath();
        if (modelPath == null) {
            return null;
        }
        Key model = NamespacedKey.fromString(modelPath);
        if (model == null) {
            return null;
        }
        return resourcePackColors.modelColor(model);
    }
}
