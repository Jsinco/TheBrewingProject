package dev.jsinco.brewery.datagenerator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ModelResolver {

    // Model texture slots ordered by how well they represent the whole thing
    private static final List<String> SLOTS = List.of(
            "layer0", "all", "top", "texture", "cross", "plant", "crop", "side", "end", "wall",
            "pane", "bars", "rail", "fan", "wool", "stem", "candle", "torch", "lantern", "fire",
            "up", "front", "back", "north", "east", "south", "west", "bottom", "down", "edge",
            "inside", "slab", "lit", "unlit", "dirt", "pattern", "flowerpot"
    );
    private static final int UNNAMED_SLOT_RANK = SLOTS.size();
    private static final int PARTICLE_SLOT_RANK = SLOTS.size() + 1;
    private static final int MAX_DEPTH = 16;
    private static final String COLOR_PREFIX = "__color__/";
    private static final String OVERLAY_SUFFIX = "_overlay";

    private final Path assetsRoot;
    private final Map<String, Map<String, String>> modelTextures = new HashMap<>();

    public ModelResolver(Path assetsRoot) {
        this.assetsRoot = assetsRoot;
    }

    public List<String> registryNames() throws IOException {
        TreeSet<String> names = new TreeSet<>();
        for (String directory : List.of("items", "blockstates")) {
            Path path = assetsRoot.resolve(directory);
            if (!Files.isDirectory(path)) continue;
            try (Stream<Path> list = Files.list(path)) {
                list.map(file -> file.getFileName().toString())
                    .filter(file -> file.endsWith(".json"))
                    .map(file -> file.substring(0, file.length() - ".json".length()))
                    .forEach(names::add);
            }
        }
        return List.copyOf(names);
    }

    public @Nullable String resolve(String registryName) {
        List<String> models = new ArrayList<>();
        JsonObject definition = readJson(assetsRoot.resolve("items/" + registryName + ".json"));
        if (definition != null) collectModels(definition.get("model"), models, 0);
        if (models.isEmpty()) collectBlockStateModels(registryName, models);
        for (String model : models) {
            if (model.startsWith(COLOR_PREFIX)) return "block/" + model.substring(COLOR_PREFIX.length()) + "_wool";
            String texture = bestTexture(textures(model, 0), registryName);
            if (texture != null) return texture;
        }
        return null;
    }

    private @Nullable String bestTexture(Map<String, String> textures, String registryName) {
        return textures.entrySet().stream()
                .filter(slot -> !slot.getValue().endsWith(OVERLAY_SUFFIX)) // Overlays are grayscale
                .map(slot -> new Candidate(slotRank(slot.getKey()), !fileName(slot.getValue()).equals(registryName), slot.getKey(), slot.getValue()))
                .min(Comparator.comparingInt(Candidate::rank)
                    .thenComparing(Candidate::unnamed)
                    .thenComparing(Candidate::slot))
                .map(Candidate::texture)
                .orElse(null);
    }

    private static int slotRank(String slot) {
        int index = SLOTS.indexOf(slot);
        if (index >= 0) return index;
        return "particle".equals(slot) ? PARTICLE_SLOT_RANK : UNNAMED_SLOT_RANK;
    }

    private Map<String, String> textures(String model, int depth) {
        String name = withoutNamespace(model);
        Map<String, String> cached = modelTextures.get(name);
        if (cached != null) return cached;
        if (depth > MAX_DEPTH) return Map.of();
        JsonObject json = readJson(assetsRoot.resolve("models/" + name + ".json"));
        if (json == null) return Map.of();
        Map<String, String> merged = new LinkedHashMap<>();
        if (json.get("parent") instanceof JsonPrimitive parent) merged
                .putAll(textures(parent.getAsString(), depth + 1));
        if (json.get("textures") instanceof JsonObject slots) {
            for (Map.Entry<String, JsonElement> slot : slots.entrySet()) {
                merged.put(slot.getKey(), slot.getValue().toString());
            }
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> slot : merged.entrySet()) {
            String texture = dereference(slot.getValue(), merged);
            if (texture != null) resolved.put(slot.getKey(), withoutNamespace(texture));
        }
        modelTextures.put(name, resolved);
        return resolved;
    }

    private @Nullable String dereference(@Nullable String value, Map<String, String> slots) {
        String current = value;
        for (int i = 0; i <= MAX_DEPTH; i++) {
            if (current == null || current.isEmpty()) return null;
            switch (current.charAt(0)) {
                case '{' -> {
                    JsonElement sprite = JsonParser.parseString(current).getAsJsonObject().get("sprite");
                    current = sprite instanceof JsonPrimitive primitive ? primitive.getAsString() : null;
                }
                case '"' -> current = JsonParser.parseString(current).getAsString();
                case '#' -> current = slots.get(current.substring(1));
                default -> {
                    return current;
                }
            }
        }
        return null;
    }

    private void collectModels(@Nullable JsonElement node, List<String> models, int depth) {
        if (depth > MAX_DEPTH || node == null) return;
        if (node instanceof JsonArray array) {
            array.forEach(element -> collectModels(element, models, depth + 1));
            return;
        }
        if (!(node instanceof JsonObject object)) return;
        JsonElement model = object.get("model");
        if (model instanceof JsonPrimitive primitive)  models.add(primitive.getAsString());
        if (object.get("color") instanceof JsonPrimitive color) models.add(COLOR_PREFIX + color.getAsString());
        if (model instanceof JsonObject) collectModels(model, models, depth + 1);
        if (object.get("base") instanceof JsonPrimitive base) models.add(base.getAsString());
        collectGuiCases(object, models, depth);
        for (String key : List.of("on_false", "fallback", "models", "cases", "entries", "on_true", "base")) {
            collectModels(object.get(key), models, depth + 1);
        }
    }

    private void collectGuiCases(JsonObject object, List<String> models, int depth) {
        if (!(object.get("property") instanceof JsonPrimitive property)
                || !"minecraft:display_context".equals(property.getAsString())
                || !(object.get("cases") instanceof JsonArray cases)) {
            return;
        }
        for (JsonElement element : cases) {
            if (element instanceof JsonObject caseObject && isGuiCase(caseObject.get("when")))
                collectModels(caseObject, models, depth + 1);
        }
    }

    private boolean isGuiCase(@Nullable JsonElement when) {
        if (when instanceof JsonPrimitive primitive) return "gui".equals(primitive.getAsString());
        if (when instanceof JsonArray array) {
            for (JsonElement element : array) {
                if (element instanceof JsonPrimitive primitive && "gui".equals(primitive.getAsString())) return true;
            }
        }
        return false;
    }

    private void collectBlockStateModels(String registryName, List<String> models) {
        JsonObject json = readJson(assetsRoot.resolve("blockstates/" + registryName + ".json"));
        if (json == null) return;

        collectBlockStateModels(json.get("variants"), models, 0);
        collectBlockStateModels(json.get("multipart"), models, 0);
    }

    private void collectBlockStateModels(@Nullable JsonElement node, List<String> models, int depth) {
        if (depth > MAX_DEPTH || node == null) return;
        if (node instanceof JsonArray array) {
            array.forEach(element -> collectBlockStateModels(element, models, depth + 1));
        } else if (node instanceof JsonObject object) {
            if (object.get("model") instanceof JsonPrimitive model) models.add(model.getAsString());
            object.asMap().values().forEach(element -> collectBlockStateModels(element, models, depth + 1));
        }
    }

    private @Nullable JsonObject readJson(Path path) {
        if (!Files.isRegularFile(path)) return null;
        try (InputStream inputStream = Files.newInputStream(path);
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader) instanceof JsonObject object ? object : null;
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    static String fileName(String texture) {
        return texture.substring(texture.lastIndexOf('/') + 1);
    }

    private static String withoutNamespace(String reference) {
        int namespaceEnd = reference.indexOf(':');
        return namespaceEnd < 0 ? reference : reference.substring(namespaceEnd + 1);
    }

    private record Candidate(int rank, boolean unnamed, String slot, String texture) {
    }
}
