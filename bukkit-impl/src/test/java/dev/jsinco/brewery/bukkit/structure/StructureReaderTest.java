package dev.jsinco.brewery.bukkit.structure;

import dev.jsinco.brewery.bukkit.structure.serializer.BreweryVectorListSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.BreweryVectorSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.MaterialHolderSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.MaterialTagSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.MaterialsSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.StructureMetaSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.StructureTypeSerializer;
import dev.jsinco.brewery.bukkit.structure.serializer.Vector3iSerializer;
import dev.jsinco.brewery.configuration.OkaeriSerdesBuilder;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockBukkitExtension.class)
class StructureReaderTest {

    @MockBukkitInject
    ServerMock serverMock;
    WorldMock worldMock;
    @TempDir
    File tempDirectory;

    @BeforeEach
    void setUp() {
        this.worldMock = serverMock.addSimpleWorld("test_world");
    }

    @ParameterizedTest
    @MethodSource("getSchemFormatPaths")
    void fromJson_names(String pathString) throws StructureReadException, IOException, URISyntaxException {
        String structureName = pathString.replace("/structures/", "").replace(".json", "");
        URL url = PlacedBreweryStructure.class.getResource(pathString);
        BreweryStructure structure;
        File jsonFile = new File(tempDirectory, "structure.json");
        try (InputStream inputStream = PlacedBreweryStructure.class.getResourceAsStream(pathString)) {
            Files.copy(inputStream, new File(tempDirectory, "structure.json").toPath());
        }
        try {
            structure = readStructure(Paths.get(url.toURI()), jsonFile);
        } catch (FileSystemNotFoundException e) {
            try (FileSystem ignored = FileSystems.newFileSystem(url.toURI(), Map.of())) {
                structure = readStructure(Paths.get(url.toURI()), jsonFile);
            }
        }
        assertEquals(structureName, structure.getName());
    }

    BreweryStructure readStructure(Path internalPath, File jsonFile) throws IOException {
        OkaeriSerdes pack = new OkaeriSerdesBuilder()
                .add(new BreweryVectorSerializer())
                .add(new BreweryVectorListSerializer())
                .add(new MaterialHolderSerializer())
                .add(new MaterialTagSerializer())
                .add(new StructureMetaSerializer())
                .add(new Vector3iSerializer())
                .add(new MaterialsSerializer())
                .add(new StructureTypeSerializer())
                .build();
        try (InputStream inputStream = new FileInputStream(jsonFile)) {
            return ConfigManager.create(BreweryStructureConfig.class, it -> {
                it.configure(options -> options.configurer(new YamlSnakeYamlConfigurer(), pack));
                it.load(inputStream);
            }).toStructure(internalPath, StructurePlacerUtils.matchers());
        }
    }

    static Stream<Arguments> getSchemFormatPaths() {
        return Stream.of("/structures/large_barrel.json", "/structures/small_barrel.json").map(Arguments::of);
    }
}