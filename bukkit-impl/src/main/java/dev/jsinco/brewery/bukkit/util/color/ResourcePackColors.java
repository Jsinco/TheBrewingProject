package dev.jsinco.brewery.bukkit.util.color;

import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.util.LoggingModule;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.item.CompositeItemModel;
import team.unnamed.creative.item.ConditionItemModel;
import team.unnamed.creative.item.EmptyItemModel;
import team.unnamed.creative.item.Item;
import team.unnamed.creative.item.ItemModel;
import team.unnamed.creative.item.RangeDispatchItemModel;
import team.unnamed.creative.item.ReferenceItemModel;
import team.unnamed.creative.item.SelectItemModel;
import team.unnamed.creative.item.SpecialItemModel;
import team.unnamed.creative.item.special.HeadSpecialRender;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.texture.Texture;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ResourcePackColors {

    private final Map<Key, Color> itemModelColors = new ConcurrentHashMap<>();
    private final Map<Key, Map<Float, Color>> customModelDataColors = new ConcurrentHashMap<>();
    private final List<ResourcePackSource> sources = new ArrayList<>();

    public void init() {
        if (!FeaturesConfig.test(FeatureFlag.RESOURCE_PACK_COLORS, null)) {
            return;
        }
        List<ResourcePack> resourcePacks;
        try {
            resourcePacks = readResourcePacks();
        } catch (IOException | InterruptedException e) {
            Logger.logAndTrackErr(e);
            return;
        }
        if (resourcePacks.isEmpty()) {
            return;
        }
        for (ResourcePack resourcePack : resourcePacks) {
            readResourcePackContent(resourcePack);
        }
        Logger.log("Successfully read serverside resource packs.");
    }

    private void readResourcePackContent(ResourcePack resourcePack) {
        ResourceResolver resolver = new ResourceResolver(resourcePack);
        readContainerContent(resourcePack.items(), resolver);
        resourcePack.overlays()
                .forEach(overlay -> readContainerContent(overlay.items(), resolver));
    }

    private void readContainerContent(Collection<Item> containerContent, ResourceResolver resolver) {
        if (containerContent.isEmpty()) {
            return;
        }
        for (Item item : containerContent) {
            ItemModel itemModel = item.model();
            if (itemModel instanceof RangeDispatchItemModel rangeDispatchItemModel) {
                for (RangeDispatchItemModel.Entry entry : rangeDispatchItemModel.entries()) {
                    BufferedImage modelImage = readItemModel(entry.model(), resolver);
                    if (modelImage == null) {
                        continue;
                    }
                    customModelDataColors.computeIfAbsent(item.key(), ignored -> new HashMap<>())
                            .put(entry.threshold(), ColorUtil.getDistinctColor(modelImage));
                }
                continue;
            }
            BufferedImage modelImage = readItemModel(item.model(), resolver);
            if (modelImage == null) {
                Logger.logDev("Could not read item model '%s'".formatted(item.key().asMinimalString()), LoggingModule.RESOURCE_PACK_PARSING);
                continue;
            }
            itemModelColors.put(item.key(), ColorUtil.getDistinctColor(modelImage));
        }
    }

    private List<ResourcePack> readResourcePacks() throws IOException, InterruptedException {
        if (sources.isEmpty()) {
            org.bukkit.packs.ResourcePack bukkitPack = Bukkit.getServerResourcePack();
            if (bukkitPack == null) {
                return List.of();
            }
            sources.add(new ResourcePackSource.HttpResourcePackSource(bukkitPack.getUrl(), false, null));
        }
        List<ResourcePack> output = new ArrayList<>();
        for (ResourcePackSource source : List.copyOf(sources)) {
            try {
                output.add(source.readPack());
            } catch (Exception e) {
                Logger.logDev("Could not read resource pack '%s', invalid format".formatted(source.asString()), LoggingModule.RESOURCE_PACK_PARSING);
            }
        }
        return output;
    }

    private @Nullable BufferedImage readItemModel(ItemModel itemModel, ResourceResolver resolver) {
        return switch (itemModel) {
            case CompositeItemModel compositeItemModel -> readCompositeModel(compositeItemModel, resolver);
            case SpecialItemModel specialItemModel -> readSpecialRender(specialItemModel, resolver);
            case RangeDispatchItemModel rangeDispatchItemModel ->
                    readItemModel(rangeDispatchItemModel.fallback(), resolver);
            case ReferenceItemModel referenceItemModel ->
                    readModel(resolver.resolveModel(referenceItemModel.model()), resolver);
            case ConditionItemModel conditionItemModel -> readItemModel(conditionItemModel.onFalse(), resolver);
            case SelectItemModel selectItemModel -> readItemModel(
                    selectItemModel.fallback(), resolver
            );
            case EmptyItemModel ignored -> new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            case null -> null;
            default -> {
                Logger.logDev("Unsupported item model format '%s'".formatted(itemModel.getClass().getSimpleName()), LoggingModule.RESOURCE_PACK_PARSING);
                yield null;
            }
        };
    }

    private @Nullable BufferedImage readSpecialRender(SpecialItemModel specialItemModel, ResourceResolver resolver) {
        Texture texture = switch (specialItemModel.render()) {
            case HeadSpecialRender headSpecialRender -> {
                Key textureKey = headSpecialRender.texture();
                if (textureKey == null) {
                    yield null;
                }
                yield resolver.resolveTexture(textureKey);
            }
            default -> null;
        };
        if (texture == null) {
            return readModel(resolver.resolveModel(specialItemModel.base()), resolver);
        }
        return readTexture(texture);
    }

    private BufferedImage readModel(Model model, ResourceResolver resolver) {
        if (model == null) {
            return null;
        }
        List<ItemOverride> overrides = model.overrides();
        if (!overrides.isEmpty()) {
            for (int i = 0; i < overrides.size(); i++) {
                ItemOverride override = overrides.get(i);
                if (override.model().equals(model.key())) {
                    continue;
                }
                BufferedImage image = readModel(resolver.resolveModel(override.model()), resolver);
                if (image == null) {
                    Logger.logDev("Could not resolve model override '%s' for model '%s'".formatted(i, model.key().asMinimalString()), LoggingModule.RESOURCE_PACK_PARSING);
                    continue;
                }
                customModelDataColors.computeIfAbsent(model.key(), ignored -> new HashMap<>())
                        .put((float) i, ColorUtil.getDistinctColor(image));
            }
        }
        List<BufferedImage> layers = model.textures().layers().stream()
                .map(modelTexture -> readModelTexture(resolver, modelTexture))
                .toList();
        if (layers.isEmpty()) {
            return mergeImages(model.textures().variables().entrySet().stream()
                    .filter(entry -> entry.getKey().matches("\\d+"))
                    .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getKey())))
                    .map(Map.Entry::getValue)
                    .map(modelTexture -> readModelTexture(resolver, modelTexture))
                    .toList()
            );
        }
        return mergeImages(layers);
    }

    private @Nullable BufferedImage readCompositeModel(CompositeItemModel compositeItemModel, ResourceResolver resolver) {
        List<BufferedImage> layers = compositeItemModel.models()
                .stream()
                .map(modelTexture -> readItemModel(modelTexture, resolver))
                .filter(Objects::nonNull)
                .toList();
        if (layers.isEmpty()) {
            return null;
        }
        return mergeImages(layers);
    }

    private @Nullable BufferedImage mergeImages(List<BufferedImage> images) {
        BufferedImage output = null;
        for (BufferedImage layer : images) {
            if (output == null) {
                output = layer;
                continue;
            }
            output = merge(output, layer);
        }
        return output;
    }

    public @Nullable Color modelColor(Key modelKey) {
        return itemModelColors.get(modelKey);
    }

    private static BufferedImage merge(BufferedImage first, BufferedImage override) {
        int height = Math.max(first.getHeight(), override.getHeight());
        int width = Math.max(first.getWidth(), override.getWidth());
        BufferedImage output = new BufferedImage(width, height, first.getType());
        writeTo(output, first);
        writeTo(output, override);
        return output;
    }

    private static void writeTo(BufferedImage to, BufferedImage content) {
        for (int x = 0; x < content.getWidth(); x++) {
            for (int y = 0; y < content.getHeight(); y++) {
                to.setRGB(x, y, content.getRGB(x, y));
            }
        }
    }

    private static @Nullable BufferedImage readModelTexture(ResourceResolver resolver, ModelTexture model) {
        Key key = model.key();
        if (key == null) {
            return null;
        }
        return readTexture(resolver.resolveTexture(key));
    }

    private static @Nullable BufferedImage readTexture(@Nullable Texture texture) {
        if (texture == null) {
            return null;
        }
        try (InputStream inputStream = new ByteArrayInputStream(texture.data().toByteArray())) {
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            Logger.logAndTrackErr(e);
        }
        return null;
    }

    public void addSource(ResourcePackSource source) {
        this.sources.add(source);
    }

    public @Nullable Color customModelDataColor(@NotNull Key key, int customModelData) {
        return customModelDataColors.getOrDefault(key, Map.of())
                .get((float) customModelData);
    }
}
