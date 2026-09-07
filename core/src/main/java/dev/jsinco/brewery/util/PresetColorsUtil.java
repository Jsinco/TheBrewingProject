package dev.jsinco.brewery.util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PresetColorsUtil {

    // Texture face suffixes ordered by how well they represent the block as a whole
    private static final List<String> FACE_SUFFIXES = List.of("top", "side", "front", "bottom", "back", "north", "east", "south", "west");
    private static final int FRAME_SUFFIX_RANK = FACE_SUFFIXES.size();
    private static final int STAGE_SUFFIX_RANK = FRAME_SUFFIX_RANK + 10_000;
    private static final int UNRANKED = Integer.MAX_VALUE;

    private static final Map<Key, Color> ITEM_COLORS = compileItemColors();
    private static final Map<Key, Color> ITEM_COLOR_FALLBACKS = computeFallbacks(ITEM_COLORS);
    private static final Map<Key, Color> BIOME_WATER_COLORS = compileWaterColors();


    private PresetColorsUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static Map<Key, Color> compileItemColors() {
        JsonObject jsonObject = FileUtil.readJsonResource("/colors.json").getAsJsonObject();
        ImmutableMap.Builder<Key, Color> immutableMapBuilder = ImmutableMap.builder();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            immutableMapBuilder.put(Key.key(entry.getKey()), new Color(Integer.parseInt(entry.getValue().getAsString(), 16)));
        }
        return immutableMapBuilder.build();
    }

    private static Map<Key, Color> computeFallbacks(Map<Key, Color> itemColors) {
        Map<Key, Color> fallbacks = new HashMap<>();
        Map<Key, Integer> ranks = new HashMap<>();
        for (Map.Entry<Key, Color> entry : itemColors.entrySet()) {
            String texture = entry.getKey().value();
            int suffixStart = texture.lastIndexOf('_');
            if (suffixStart < 1) continue;

            int rank = suffixRank(texture.substring(suffixStart + 1));
            if (rank == UNRANKED) continue;

            Key materialKey = Key.key(entry.getKey().namespace(), texture.substring(0, suffixStart));
            if (itemColors.containsKey(materialKey)) continue;

            Integer previousRank = ranks.get(materialKey);
            if (previousRank != null && previousRank <= rank) continue;

            ranks.put(materialKey, rank);
            fallbacks.put(materialKey, entry.getValue());
        }
        return ImmutableMap.copyOf(fallbacks);
    }

    // How well a texture suffix represents the whole block (lower is better)
    private static int suffixRank(String suffix) {
        int faceIndex = FACE_SUFFIXES.indexOf(suffix);
        if (faceIndex >= 0) return faceIndex;

        if (suffix.startsWith("stage")) return frameRank(suffix.substring("stage".length()), STAGE_SUFFIX_RANK);
        return frameRank(suffix, FRAME_SUFFIX_RANK);
    }

    private static int frameRank(String frame, int rankOffset) {
        if (frame.isEmpty() || frame.length() > 4) return UNRANKED;
        for (int i = 0; i < frame.length(); i++) {
            if (!Character.isDigit(frame.charAt(i))) return UNRANKED;
        }
        return rankOffset + Integer.parseInt(frame);
    }

    private static Map<Key, Color> compileWaterColors() {
        JsonObject jsonObject = FileUtil.readJsonResource("/biomes.json").getAsJsonObject();
        ImmutableMap.Builder<Key, Color> immutableMapBuilder = ImmutableMap.builder();
        for (Map.Entry<String, JsonElement> entry : jsonObject.get("water_color").getAsJsonObject().entrySet()) {
            immutableMapBuilder.put(Key.key(entry.getKey()), new Color(Integer.parseInt(entry.getValue().getAsString(), 16)));
        }
        return immutableMapBuilder.build();
    }

    public static @Nullable Color getItemColor(Key itemId) {
        Color color = ITEM_COLORS.get(itemId);
        return color != null ? color : ITEM_COLOR_FALLBACKS.get(itemId);
    }


    public static @Nullable Color getWaterColor(Key biomeId) {
        return BIOME_WATER_COLORS.get(biomeId);
    }
}
