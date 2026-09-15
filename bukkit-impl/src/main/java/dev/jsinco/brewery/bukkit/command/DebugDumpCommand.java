package dev.jsinco.brewery.bukkit.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.jsinco.brewery.api.effect.DrunkState;
import dev.jsinco.brewery.api.effect.modifier.DrunkenModifier;
import dev.jsinco.brewery.api.event.DrunkEvent;
import dev.jsinco.brewery.api.integration.Integration;
import dev.jsinco.brewery.api.structure.StructureType;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.configuration.DrunkenModifierSection;
import dev.jsinco.brewery.util.FileUtil;
import dev.jsinco.brewery.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.potion.PotionEffect;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DebugDumpCommand {

    private static final List<String> GENERATED_FILES = List.of(
            "system.yml", "server.yml", "tbp.yml", "players.yml"
    );
    private static final List<String> CONFIG_FILES = List.of(
            "config.yml", "modifiers.yml", "events.yml", "recipes.yml",
            "incomplete-recipes.yml", "ingredients.yml"
    );
    private static final String LOG_FILE = "latest.log";
    private static final String DB_FILE = "brewery.db";

    private static List<String> defaultFiles() {
        List<String> files = new ArrayList<>(GENERATED_FILES);
        files.addAll(CONFIG_FILES);
        files.add(LOG_FILE);
        return files;
    }

    private static List<String> allFiles(File dataFolder) {
        List<String> files = new ArrayList<>(defaultFiles());
        files.addAll(listDirectory(new File(dataFolder, "locale")));
        files.addAll(listDirectory(new File(dataFolder, "structures")));
        files.add(DB_FILE);
        return files;
    }

    private static List<String> listDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return List.of();
        File[] files = dir.listFiles(File::isFile);
        if (files == null) return List.of();
        String prefix = dir.getName() + "/";
        return Arrays.stream(files).map(f -> prefix + f.getName()).toList();
    }

    private static boolean isAllFlag(String token) {
        return token.equals("-a") || token.equals("-all");
    }

    private static boolean isNoneFlag(String token) {
        return token.equals("-n") || token.equals("-none");
    }

    private static boolean isIncludeFlag(String token) {
        return token.equals("-i") || token.equals("-include");
    }

    private static boolean isExcludeFlag(String token) {
        return token.equals("-e") || token.equals("-exclude");
    }

    private static boolean isValueFlag(String token) {
        return isIncludeFlag(token) || isExcludeFlag(token);
    }

    private static boolean isSafeSpec(String spec, File dataFolder) {
        if (spec.equals("*")) return true;
        String pathPart = spec.endsWith("/*") ? spec.substring(0, spec.length() - 2) : spec;
        try {
            File resolved = new File(dataFolder, pathPart).getCanonicalFile();
            File base = dataFolder.getCanonicalFile();
            String resolvedPath = resolved.getPath();
            String basePath = base.getPath();
            return resolvedPath.equals(basePath) || resolvedPath.startsWith(basePath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private static List<String> expandSpec(String spec, File dataFolder) {
        if (spec.equals("*")) return allFiles(dataFolder);
        if (spec.endsWith("/*")) {
            return listDirectory(new File(dataFolder, spec.substring(0, spec.length() - 2)));
        }
        return List.of(spec);
    }

    private static LinkedHashSet<String> resolveFileSet(String argsString, File dataFolder) {
        LinkedHashSet<String> files = new LinkedHashSet<>(defaultFiles());
        if (argsString.isBlank()) return files;

        String[] tokens = argsString.trim().split("\\s+");
        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];
            if (isAllFlag(token)) {
                files.addAll(allFiles(dataFolder));
                i++;
            } else if (isNoneFlag(token)) {
                files.clear();
                i++;
            } else if (isIncludeFlag(token) && i + 1 < tokens.length) {
                for (String spec : tokens[++i].split(",")) {
                    String trimmed = spec.trim();
                    if (isSafeSpec(trimmed, dataFolder)) {
                        files.addAll(expandSpec(trimmed, dataFolder));
                    }
                }
                i++;
            } else if (isExcludeFlag(token) && i + 1 < tokens.length) {
                for (String spec : tokens[++i].split(",")) {
                    String trimmed = spec.trim();
                    if (isSafeSpec(trimmed, dataFolder)) {
                        expandSpec(trimmed, dataFolder).forEach(files::remove);
                    }
                }
                i++;
            } else {
                i++;
            }
        }
        return files;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> command() {
        return Commands.literal("dump")
                .executes(context -> execute(context, ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests(DebugDumpCommand::suggest)
                        .executes(context -> execute(context,
                                context.getArgument("args", String.class)))
                );
    }

    private static int execute(CommandContext<CommandSourceStack> context, String argsString) {
        CommandSender sender = context.getSource().getSender();
        MessageUtil.message(sender, "tbp.command.dump.pending");
        Bukkit.getAsyncScheduler().runNow(TheBrewingProject.getInstance(), scheduledTask -> {
            File zipFile = createDebugDump(argsString);
            if (zipFile == null) {
                MessageUtil.message(sender, "tbp.command.dump.failure");
            } else {
                MessageUtil.message(sender, "tbp.command.dump.success",
                        Placeholder.unparsed("file", zipFile.getAbsolutePath()));
            }
        });
        return 1;
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        File dataFolder = FileUtil.getWorkFolder();
        String argString = builder.getRemaining();
        String[] tokens = argString.split(" ", -1);
        String current = tokens[tokens.length - 1];
        String prev = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
        SuggestionsBuilder currentBuilder = builder.createOffset(
                builder.getStart() + argString.length() - current.length());

        if (isValueFlag(prev)) {
            int commaIdx = current.lastIndexOf(',');
            String prefix = commaIdx >= 0 ? current.substring(0, commaIdx + 1) : "";
            String partial = current.substring(commaIdx + 1);
            suggestFileSpecs(currentBuilder, prefix, partial, dataFolder);
        } else {
            for (String flag : List.of("-i", "-include", "-e", "-exclude", "-a", "-all", "-n", "-none")) {
                if (flag.startsWith(current)) {
                    currentBuilder.suggest(flag);
                }
            }
        }
        return currentBuilder.buildFuture();
    }

    private static void suggestFileSpecs(SuggestionsBuilder builder, String prefix, String partial, File dataFolder) {
        List<String> candidates = new ArrayList<>();
        candidates.add("*");
        candidates.add("locale/*");
        candidates.add("structures/*");
        candidates.add(DB_FILE);
        candidates.addAll(defaultFiles());
        candidates.addAll(listDirectory(new File(dataFolder, "locale")));
        candidates.addAll(listDirectory(new File(dataFolder, "structures")));
        for (String candidate : candidates) {
            if (candidate.startsWith(partial)) {
                builder.suggest(prefix + candidate);
            }
        }
    }

    private static File createDebugDump(String argsString) {
        File dataFolder = FileUtil.getWorkFolder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        File outputDir = new File(dataFolder, "output");
        if (!outputDir.exists() && !outputDir.mkdirs()) return null;

        LinkedHashSet<String> filesToInclude = resolveFileSet(argsString, dataFolder);

        File zipFile = new File(outputDir, "DebugDump_" + timestamp + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            for (String spec : filesToInclude) {
                addSpecToZip(zos, spec, dataFolder);
            }
        } catch (IOException e) {
            Logger.logAndTrackErr("Failed to create debug dump: " + e.getMessage());
            return null;
        }
        return zipFile;
    }

    private static void addSpecToZip(ZipOutputStream zos, String spec, File dataFolder) throws IOException {
        switch (spec) {
            case "system.yml" -> addYaml(zos, spec, createSystemDump());
            case "server.yml" -> addYaml(zos, spec, createServerDump());
            case "tbp.yml" -> addYaml(zos, spec, createTbpDump());
            case "players.yml" -> addYaml(zos, spec, createPlayerDump());
            case "config.yml" -> addRedactedConfig(zos, new File(dataFolder, spec));
            case LOG_FILE -> addFile(zos, new File("logs/latest.log"), spec);
            default -> addFile(zos, new File(dataFolder, spec), spec);
        }
    }

    private static void addYaml(ZipOutputStream zos, String entryName, YamlConfiguration yaml) throws IOException {
        byte[] data = yaml.saveToString().getBytes(StandardCharsets.UTF_8);
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(data);
        zos.closeEntry();
    }

    private static void addRedactedConfig(ZipOutputStream zos, File configFile) {
        if (!configFile.exists() || !configFile.isFile()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String key : List.of("encryptionKey", "previousEncryptionKeys", "breweryxMigrationSeeds")) {
            if (config.contains(key)) {
                config.set(key, "[REDACTED]");
            }
        }
        try {
            addYaml(zos, "config.yml", config);
        } catch (IOException e) {
            Logger.logAndTrackErr("Failed to add redacted config to dump: " + e.getMessage());
        }
    }

    private static void addFile(ZipOutputStream zos, File file, String entryName) {
        if (file == null || !file.exists() || !file.isFile()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            zos.putNextEntry(new ZipEntry(entryName));
            fis.transferTo(zos);
            zos.closeEntry();
        } catch (IOException e) {
            Logger.logAndTrackErr("Failed to add file to dump: " + entryName + " -> " + e.getMessage());
        }
    }

    private static YamlConfiguration createSystemDump() {
        YamlConfiguration system = new YamlConfiguration();
        system.set("version", 1);

        Properties sp = System.getProperties();
        Map<String, Object> sys = new LinkedHashMap<>();
        sys.put("java.version", sp.getProperty("java.version"));
        sys.put("java.vendor", sp.getProperty("java.vendor"));
        sys.put("java.vm.name", sp.getProperty("java.vm.name"));
        sys.put("os.name", sp.getProperty("os.name"));
        sys.put("os.arch", sp.getProperty("os.arch"));
        sys.put("os.version", sp.getProperty("os.version"));

        Runtime rt = Runtime.getRuntime();
        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("max", rt.maxMemory());
        mem.put("total", rt.totalMemory());
        mem.put("free", rt.freeMemory());
        mem.put("used", rt.totalMemory() - rt.freeMemory());
        sys.put("memory", mem);

        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        sys.put("jvm.uptime.millis", mx.getUptime());
        sys.put("jvm.inputArgs", mx.getInputArguments());
        system.set("system", sys);
        return system;
    }

    private static YamlConfiguration createServerDump() {
        YamlConfiguration server = new YamlConfiguration();
        server.set("version", 1);

        server.set("server.name", Bukkit.getName());
        server.set("server.version", Bukkit.getVersion());
        server.set("server.bukkitVersion", Bukkit.getBukkitVersion());
        server.set("server.address", (Bukkit.getIp().isBlank() ? "localhost" : Bukkit.getIp()) + ":" + Bukkit.getPort());
        server.set("server.onlineMode", Bukkit.getServer().getOnlineMode());
        server.set("server.onlinePlayers", Bukkit.getOnlinePlayers().size());
        server.set("server.maxPlayers", Bukkit.getMaxPlayers());
        server.set("server.motd", Bukkit.getServer().getMotd());

        Map<String, Object> plugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            PluginDescriptionFile d = p.getDescription();
            Map<String, Object> pInfo = new LinkedHashMap<>();
            pInfo.put("version", d.getVersion());
            pInfo.put("enabled", p.isEnabled());
            pInfo.put("main", d.getMain());
            if (!d.getAuthors().isEmpty()) pInfo.put("authors", d.getAuthors());
            if (d.getWebsite() != null) pInfo.put("website", d.getWebsite());
            if (!d.getDepend().isEmpty()) pInfo.put("depend", d.getDepend());
            if (!d.getSoftDepend().isEmpty()) pInfo.put("softdepend", d.getSoftDepend());
            if (!d.getLoadBefore().isEmpty()) pInfo.put("loadbefore", d.getLoadBefore());
            plugins.put(d.getName(), pInfo);
        }
        server.set("server.plugins", plugins);
        return server;
    }

    private static YamlConfiguration createTbpDump() {
        TheBrewingProject plugin = TheBrewingProject.getInstance();
        YamlConfiguration tbp = new YamlConfiguration();
        tbp.set("version", 1);

        tbp.set("tbp.pluginVersion", plugin.getPluginMeta().getVersion());
        tbp.set("tbp.internalTime", plugin.getTime());
        tbp.set("tbp.recipeCount", plugin.getRecipeRegistry().getRecipes().size());
        tbp.set("tbp.activeIntegrations", plugin.getIntegrationManager().getIntegrationRegistry().getAllIntegrations().stream()
                .map(Integration::getId)
                .sorted()
                .toList());
        tbp.set("tbp.activeCauldrons", plugin.getBreweryRegistry().getActiveSinglePositionStructure().size());
        tbp.set("tbp.placedBarrels", plugin.getPlacedStructureRegistry().getStructures(StructureType.BARREL).size());
        tbp.set("tbp.placedDistilleries", plugin.getPlacedStructureRegistry().getStructures(StructureType.DISTILLERY).size());
        tbp.set("tbp.openedBarrels", plugin.getBreweryRegistry().countOpened(StructureType.BARREL));
        tbp.set("tbp.openedDistilleries", plugin.getBreweryRegistry().countOpened(StructureType.DISTILLERY));
        tbp.set("tbp.drunkPlayers", Bukkit.getOnlinePlayers().stream()
                .filter(p -> plugin.getDrunksManager().getDrunkState(p.getUniqueId()) != null)
                .count());
        return tbp;
    }

    private static YamlConfiguration createPlayerDump() {
        TheBrewingProject plugin = TheBrewingProject.getInstance();
        YamlConfiguration players = new YamlConfiguration();
        players.set("version", 1);

        for (Player player : Bukkit.getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();

            // Basic info
            players.set("players." + uuid + ".name", player.getName());
            players.set("players." + uuid + ".locale", player.getLocale());
            players.set("players." + uuid + ".op", player.isOp());
            players.set("players." + uuid + ".ping", player.getPing());
            players.set("players." + uuid + ".dead", player.isDead());
            players.set("players." + uuid + ".health", player.getHealth());
            players.set("players." + uuid + ".maxHealth", player.getMaxHealth());
            players.set("players." + uuid + ".gameMode", player.getGameMode().name());
            players.set("players." + uuid + ".world", player.getWorld().getName());
            players.set("players." + uuid + ".location",
                    String.format("%.2f, %.2f, %.2f", player.getX(), player.getY(), player.getZ()));

            // TBP drunk state
            DrunkState drunkState = plugin.getDrunksManager().getDrunkState(player.getUniqueId());
            if (drunkState != null) {
                for (DrunkenModifier modifier : DrunkenModifierSection.modifiers().drunkenModifiers()) {
                    players.set("players." + uuid + ".tbp.modifiers." + modifier.name(),
                            drunkState.modifierValue(modifier));
                }
                players.set("players." + uuid + ".tbp.stateTimestamp", drunkState.timestamp());
            }
            players.set("players." + uuid + ".tbp.passedOut",
                    plugin.getDrunksManager().isPassedOut(player.getUniqueId()));

            // Planned drunk event
            Pair<DrunkEvent, Long> plannedEvent = plugin.getDrunksManager().getPlannedEvent(player.getUniqueId());
            if (plannedEvent != null) {
                players.set("players." + uuid + ".tbp.plannedEvent.key",
                        plannedEvent.first().key().toString());
                players.set("players." + uuid + ".tbp.plannedEvent.ticksUntil",
                        plannedEvent.second() - plugin.getTime());
            }

            // TBP bypass permissions
            List<String> overrides = new ArrayList<>();
            for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
                if (info.getPermission().startsWith("brewery.override.")) {
                    overrides.add(info.getPermission() + ": " + info.getValue());
                }
            }
            if (!overrides.isEmpty()) {
                players.set("players." + uuid + ".tbp.overridePermissions", overrides);
            }

            // Active attribute modifiers
            for (Attribute attribute : Registry.ATTRIBUTE) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance == null) continue;
                for (AttributeModifier modifier : instance.getModifiers()) {
                    String key = "players." + uuid + ".attributes." + attribute.getKey().getKey() + ".modifiers." + modifier.getName();
                    players.set(key + ".operation", modifier.getOperation().name());
                    players.set(key + ".amount", modifier.getAmount());
                }
            }

            // Active potion effects
            for (PotionEffect effect : player.getActivePotionEffects()) {
                String key = "players." + uuid + ".effects." + effect.getType();
                players.set(key + ".amplifier", effect.getAmplifier());
                players.set(key + ".duration", effect.getDuration());
                players.set(key + ".particles", effect.hasParticles());
                players.set(key + ".ambient", effect.isAmbient());
                players.set(key + ".icon", effect.hasIcon());
            }
        }
        return players;
    }
}
