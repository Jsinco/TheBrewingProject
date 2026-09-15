package dev.jsinco.brewery.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ServiceLoader;

public final class FileUtil {

    private static WorkFolderProvider workFolderProvider;

    public static void extractFile(@NonNull Class<?> clazz, @NonNull String filename, @NonNull Path outDir, boolean replace) {
        try (InputStream in = clazz.getResourceAsStream("/" + filename)) {
            if (in == null) {
                throw new RuntimeException("Could not read file from jar! (" + filename + ")");
            }
            Path path = outDir.resolve(filename);
            if (!Files.exists(path) || replace) {
                Files.createDirectories(path.getParent());
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readInternalResource(String path) {
        try (InputStream inputStream = FileUtil.class.getResourceAsStream(path)) {
            if (inputStream == null) return "";
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonElement readJsonResource(String path) {
        try (InputStream inputStream = FileUtil.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FileNotFoundException(path);
            }
            return JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getWorkFolder() {
        if (workFolderProvider == null) {
            workFolderProvider = ServiceLoader.load(WorkFolderProvider.class, WorkFolderProvider.class.getClassLoader()).findFirst()
                    .orElseThrow();
        }
        return workFolderProvider.getWorkFolder();
    }

}
