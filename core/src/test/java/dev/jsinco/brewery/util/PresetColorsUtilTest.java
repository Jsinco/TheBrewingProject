package dev.jsinco.brewery.util;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetColorsUtilTest {

    @ParameterizedTest
    @CsvSource({
            "acacia_slab, acacia_planks",
            "oak_stairs, oak_planks",
            "cobblestone_wall, cobblestone",
            "bamboo_button, bamboo_planks",
            "black_carpet, black_wool",
            "oak_wood, oak_log",
            "smooth_stone_slab, smooth_stone",
            "glass_pane, glass"
    })
    void getItemColor_variantBorrowsTheColorOfWhatItIsMadeOf(String material, String source) {
        Color expected = PresetColorsUtil.getItemColor(Key.key(source));
        assertNotNull(expected, source + " is missing from colors.json");
        assertEquals(expected, PresetColorsUtil.getItemColor(Key.key(material)));
    }

    @ParameterizedTest
    @CsvSource({
            "grass_block, grass_block_top",
            "piston, piston_top",
            "furnace, furnace_top",
            "clock, clock_00",
            "carrots, carrots_stage0",
            "sunflower, sunflower_front"
    })
    void getItemColor_multiTextureMaterialResolvesToItsMostRepresentativeFace(String material, String texture) {
        Color expected = PresetColorsUtil.getItemColor(Key.key(texture));
        assertNotNull(expected, texture + " is missing from colors.json");
        assertEquals(expected, PresetColorsUtil.getItemColor(Key.key(material)));
    }

    @ParameterizedTest
    @CsvSource({
            "melon, melon_seeds",
            "grass_block, grass_block_snow",
            "furnace, furnace_minecart",
            "piston, piston_top_sticky",
            "fire, firework_star_overlay"
    })
    void getItemColor_doesNotBorrowFromUnrelatedTextures(String material, String unrelatedTexture) {
        Color unrelated = PresetColorsUtil.getItemColor(Key.key(unrelatedTexture));
        assertNotNull(unrelated, unrelatedTexture + " is missing from colors.json");
        assertNotEquals(unrelated, PresetColorsUtil.getItemColor(Key.key(material)));
    }

    @Test
    void getItemColor_unknownMaterialIsNull() {
        assertNull(PresetColorsUtil.getItemColor(Key.key("not_a_real_material")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"grass_block", "tall_grass", "large_fern", "fern", "short_grass", "water", "oak_leaves"})
    void getItemColor_biomeTintedMaterialsAreNotGrayscale(String material) {
        Color color = PresetColorsUtil.getItemColor(Key.key(material));
        assertNotNull(color);
        int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        int min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        assertTrue(max - min > 20, material + " resolved to the grayscale mask " + Integer.toHexString(color.getRGB() & 0xffffff));
    }

    @ParameterizedTest
    @ValueSource(strings = {"coal_block", "black_wool", "black_concrete", "black_carpet"})
    void getItemColor_darkMaterialsStayDark(String material) {
        Color color = PresetColorsUtil.getItemColor(Key.key(material));
        assertNotNull(color);
        assertTrue(color.getRed() + color.getGreen() + color.getBlue() < 180,
                material + " resolved to " + Integer.toHexString(color.getRGB() & 0xffffff));
    }

    @ParameterizedTest
    @ValueSource(strings = {"apple", "wheat", "sugar_cane", "cocoa_beans", "honey_bottle", "glass_bottle",
            "brewing_stand", "cauldron", "oak_slab", "stone_brick_stairs", "white_bed", "chest", "clock"})
    void getItemColor_commonMaterialsResolve(String material) {
        assertNotNull(PresetColorsUtil.getItemColor(Key.key(material)), material + " has no color");
    }
}
