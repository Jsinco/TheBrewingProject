package dev.jsinco.brewery.datagenerator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DataGenerator {

    private static final Map<String, Integer> BIOME_COLORED_BLOCKS = biomeColoredBlocks();
    private static Map<String, Integer> biomeColoredBlocks() {
        Map<String, Integer> blocks = new LinkedHashMap<>();
        blocks.put("water_still", 0x3f76e4);
        blocks.put("water_flow", 0x3f76e4);
        blocks.put("redstone_dust", 0xfc3100);
        blocks.put("melon_stem", 0x8ab348);
        blocks.put("pumpkin_stem", 0x8ab348);
        blocks.put("grass_block_top", 0x7cbd6b);
        blocks.put("grass_block_side_overlay", 0x7cbd6b);
        blocks.put("short_grass", 0x7cbd6b);
        blocks.put("tall_grass", 0x7cbd6b);
        blocks.put("fern", 0x7cbd6b);
        blocks.put("_leaves", 0x71a74d);
        blocks.put("vine", 0x48b518);
        blocks.put("sugar_cane", 0x8eb971);
        blocks.put("lily_pad", 0x208030);
        blocks.put("seagrass", 0x4d9e3f);
        blocks.put("kelp", 0x4d9e3f);
        blocks.put("dry_grass", 0xa89060);
        blocks.put("dry_bush", 0x946b44);
        blocks.put("bush", 0x71a74d);
        return Collections.unmodifiableMap(blocks);
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        if (args.length != 1) {
            System.out.print("Usage: <target folder>");
            return;
        }
        File outputFolder = new File(args[0]);
        URL url1 = ClassLoader.getSystemResource("assets/minecraft/textures/item/apple.png");
        URL url2 = ClassLoader.getSystemResource("assets/minecraft/textures/block/acacia_log.png");
        URI uri1 = url1.toURI();
        URI uri2 = url2.toURI();
        try {
            generateColorData(List.of(Paths.get(uri1), Paths.get(uri2)), outputFolder);
        } catch (FileSystemNotFoundException e) {
            try (FileSystem ignored = FileSystems.newFileSystem(uri1, Collections.emptyMap())) {
                generateColorData(List.of(Paths.get(uri1), Paths.get(uri2)), outputFolder);
            }
        }
        URL url = ClassLoader.getSystemResource("data/minecraft/worldgen/biome/desert.json");
        try {
            generateBiomeData(Paths.get(url.toURI()), outputFolder);
        } catch (FileSystemNotFoundException e) {
            try (FileSystem ignored = FileSystems.newFileSystem(uri1, Collections.emptyMap())) {
                generateBiomeData(Paths.get(url.toURI()), outputFolder);
            }
        }
    }

    private static void generateBiomeData(Path path, File outputFolder) throws IOException {
        JsonObject waterColor = new JsonObject();
        Path directory = path.getParent();
        try (Stream<Path> walk = Files.walk(directory)) {
            Iterator<Path> walkIterator = walk.iterator();
            while (walkIterator.hasNext()) {
                Path next = walkIterator.next();
                if (!next.toString().endsWith(".json")) {
                    continue;
                }
                String name = next.getFileName().toString().replace(".json", "");
                try (InputStream inputStream = Files.newInputStream(next); Reader reader = new InputStreamReader(inputStream)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonElement effectsElement = object.get("effects");
                    if (!(effectsElement instanceof JsonObject effects)) {
                        continue;
                    }
                    if (effects.has("water_color")) {
                        waterColor.addProperty(name, effects.get("water_color").getAsString().substring(1));
                    }
                }
            }
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("water_color", waterColor);
        JsonUtil.dump(jsonObject, new File(outputFolder, "biomes.json"));
    }

    private static void generateColorData(List<Path> paths, File outputFolder) throws IOException {
        JsonObject jsonObject = new JsonObject();
        for (Path path : paths) {
            Path directory = path.getParent();
            try (Stream<Path> walk = Files.walk(directory)) {
                Iterator<Path> walkIterator = walk.iterator();
                while (walkIterator.hasNext()) {
                    Path next = walkIterator.next();
                    if (!next.toString().endsWith(".png")) {
                        continue;
                    }
                    String name = next.getFileName().toString().replace(".png", "");
                    if (jsonObject.has(name)) {
                        continue;
                    }
                    try (InputStream inputStream = Files.newInputStream(next)) {
                        BufferedImage image = ImageIO.read(inputStream);
                        Color color = replaceBiomeColors(ColorUtil.getDistinctColor(image), name);
                        jsonObject.addProperty(name, Integer.toHexString(color.getRGB() & 0x00ffffff));
                    }
                }
            }
        }
        addRegistryColors(paths.get(0), jsonObject);
        JsonUtil.dump(jsonObject, new File(outputFolder, "colors.json"));
    }

    private static void addRegistryColors(Path texturePath, JsonObject jsonObject) throws IOException {
        Path assetsRoot = texturePath.getParent().getParent().getParent();
        ModelResolver modelResolver = new ModelResolver(assetsRoot);
        Map<String, String> colors = new HashMap<>();
        for (String registryName : modelResolver.registryNames()) {
            String texture = modelResolver.resolve(registryName);
            if (texture == null) continue;
            String color = colors.get(texture);
            if (color == null) {
                color = readColor(assetsRoot.resolve("textures/" + texture + ".png"), ModelResolver.fileName(texture));
                if (color == null) continue;
                colors.put(texture, color);
            }
            jsonObject.addProperty(registryName, color);
        }
    }

    private static @Nullable String readColor(Path texture, String textureName) throws IOException {
        if (!Files.isRegularFile(texture)) return null;
        try (InputStream inputStream = Files.newInputStream(texture)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) return null;
            Color color = replaceBiomeColors(ColorUtil.getDistinctColor(image), textureName);
            return Integer.toHexString(color.getRGB() & 0x00ffffff);
        }
    }

    private static Color replaceBiomeColors(Color initial, String name) {
        for (String edgeCase : BIOME_COLORED_BLOCKS.keySet()) {
            if (name.contains(edgeCase)) {
                return new Color(BIOME_COLORED_BLOCKS.get(edgeCase));
            }
        }
        return initial;
    }
}
