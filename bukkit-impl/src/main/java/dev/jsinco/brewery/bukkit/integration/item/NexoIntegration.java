package dev.jsinco.brewery.bukkit.integration.item;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.api.events.resourcepack.NexoPackUploadEvent;
import com.nexomc.nexo.items.ItemBuilder;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.integration.ItemIntegration;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackColors;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackSource;
import dev.jsinco.brewery.util.ClassUtil;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class NexoIntegration implements ItemIntegration, Listener {

    private static final boolean ENABLED = ClassUtil.exists("com.nexomc.nexo.api.NexoItems");
    private final CompletableFuture<Void> itemsLoaded = new CompletableFuture<>();
    private final CompletableFuture<Void> packLoaded = new CompletableFuture<>();
    private final CompletableFuture<Void> initialized = CompletableFuture.allOf(itemsLoaded, packLoaded);
    private final ResourcePackColors resourcePackColors;

    public NexoIntegration(ResourcePackColors resourcePackColors) {
        this.resourcePackColors = resourcePackColors;
    }

    @Override
    public Optional<ItemStack> createItem(String id) {
        ItemBuilder itemBuilder = NexoItems.itemFromId(id);
        if (itemBuilder == null) {
            return Optional.empty();
        }
        return Optional.of(itemBuilder.build());
    }

    @Override
    public boolean isIngredient(String id) {
        return NexoItems.itemFromId(id) != null;
    }

    public @Nullable Component displayName(String nexoId) {
        if (!ENABLED) {
            return null;
        }
        ItemBuilder itemBuilder = NexoItems.itemFromId(nexoId);
        if (itemBuilder == null) {
            return null;
        }
        return Optional.ofNullable(itemBuilder.getCustomName())
                .or(() -> Optional.ofNullable(itemBuilder.getItemName()))
                .orElseGet(() -> Component.text(nexoId));
    }

    @Override
    public @Nullable String getItemId(ItemStack itemStack) {
        return NexoItems.idFromItem(itemStack);
    }

    @Override
    public @NonNull CompletableFuture<Void> initialized() {
        return initialized;
    }

    @Override
    public boolean isEnabled() {
        return ENABLED;
    }

    @Override
    public String getId() {
        return "nexo";
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, TheBrewingProject.getInstance());
    }

    @Override
    public void onHotReload() {
        File resourcePack = new File(Bukkit.getPluginsFolder(), "Nexo/pack/pack.zip");
        if (resourcePack.isFile()) resourcePackColors.addSource(new ResourcePackSource.FileResourcePackSource(resourcePack));
        itemsLoaded.complete(null);
        packLoaded.complete(null);
    }

    @EventHandler
    public void onNexoItemsLoaded(NexoItemsLoadedEvent event) {
        itemsLoaded.completeAsync(() -> null);
    }

    @EventHandler
    public void onResourcePackLoaded(NexoPackUploadEvent packUploadEvent) {
        try {
            URL url = URI.create(packUploadEvent.getUrl()).toURL();
            resourcePackColors.addSource(new ResourcePackSource.InputStreamResourcePackSource(url::openStream, packUploadEvent.getUrl()));
        } catch (MalformedURLException e) {
            Logger.logAndTrackErr(e);
        } finally {
            packLoaded.complete(null);
        }
    }

    @Override
    public @Nullable Color color(String id) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) {
            return null;
        }
        Key model = builder.getItemModel();
        if (model == null) {
            CustomModelData customModelData = builder.getCustomModelData();
            if (customModelData == null) {
                return null;
            }
            if (customModelData.floats().isEmpty()) {
                return null;
            }
            return resourcePackColors.customModelDataColor(builder.build().getType().key(), customModelData.floats().getFirst().intValue());
        }
        return resourcePackColors.modelColor(model);
    }
}
