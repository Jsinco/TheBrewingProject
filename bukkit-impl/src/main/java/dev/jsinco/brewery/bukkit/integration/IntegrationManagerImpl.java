package dev.jsinco.brewery.bukkit.integration;

import dev.jsinco.brewery.api.integration.Integration;
import dev.jsinco.brewery.api.integration.IntegrationManager;
import dev.jsinco.brewery.api.integration.IntegrationType;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.bukkit.api.integration.IntegrationTypes;
import dev.jsinco.brewery.bukkit.integration.chest_shop.QuickShopHikariIntegration;
import dev.jsinco.brewery.bukkit.integration.event.BodyHealthIntegration;
import dev.jsinco.brewery.bukkit.integration.event.GSitIntegration;
import dev.jsinco.brewery.bukkit.integration.item.CraftEngineIntegration;
import dev.jsinco.brewery.bukkit.integration.item.ItemsAdderIntegration;
import dev.jsinco.brewery.bukkit.integration.item.MmoItemsIntegration;
import dev.jsinco.brewery.bukkit.integration.item.MythicIntegration;
import dev.jsinco.brewery.bukkit.integration.item.NexoIntegration;
import dev.jsinco.brewery.bukkit.integration.item.OraxenIntegration;
import dev.jsinco.brewery.bukkit.integration.placeholder.MiniPlaceholdersIntegration;
import dev.jsinco.brewery.bukkit.integration.placeholder.PlaceholderApiIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.BoltIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.GriefDefenderIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.GriefPreventionIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.HuskClaimsIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.LandsIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.TownyIntegration;
import dev.jsinco.brewery.bukkit.integration.structure.WorldGuardIntegration;
import dev.jsinco.brewery.bukkit.util.color.ResourcePackColors;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.util.ClassUtil;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class IntegrationManagerImpl implements IntegrationManager {
    private final IntegrationRegistry integrationRegistry = new IntegrationRegistry();

    public void registerIntegrations(ResourcePackColors resourcePackColors) {
        /*
        Don't replace these with a method reference. Class loading issues otherwise
         */
        register(IntegrationTypes.STRUCTURE, "com.sk89q.worldguard.WorldGuard", () -> new WorldGuardIntegration());
        register(IntegrationTypes.STRUCTURE, "org.popcraft.bolt.BoltAPI", () -> new BoltIntegration());
        register(IntegrationTypes.STRUCTURE, "me.ryanhamshire.GriefPrevention.GriefPrevention", () -> new GriefPreventionIntegration());
        register(IntegrationTypes.STRUCTURE, "net.william278.huskclaims.api.BukkitHuskClaimsAPI", () -> new HuskClaimsIntegration());
        register(IntegrationTypes.STRUCTURE, "me.angeschossen.lands.api.LandsIntegration", () -> new LandsIntegration());
        register(IntegrationTypes.STRUCTURE, "com.palmergames.bukkit.towny.utils.PlayerCacheUtil", () -> new TownyIntegration());
        register(IntegrationTypes.STRUCTURE, "com.griefdefender.api.GriefDefender", () -> new GriefDefenderIntegration());
        register(IntegrationTypes.ITEM, "net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine", () -> new CraftEngineIntegration(resourcePackColors));
        register(IntegrationTypes.ITEM, "dev.lone.itemsadder.api.CustomStack", () -> new ItemsAdderIntegration(resourcePackColors));
        register(IntegrationTypes.ITEM, "com.nexomc.nexo.api.NexoItems", () -> new NexoIntegration(resourcePackColors));
        register(IntegrationTypes.ITEM, "io.th0rgal.oraxen.api.OraxenItems", () -> new OraxenIntegration(resourcePackColors));
        register(IntegrationTypes.ITEM, "net.Indyuce.mmoitems.MMOItems", () -> new MmoItemsIntegration(resourcePackColors));
        register(IntegrationTypes.ITEM, "io.lumine.mythic.bukkit.MythicBukkit", () -> new MythicIntegration(resourcePackColors));
        register(IntegrationTypes.PLACEHOLDER, "me.clip.placeholderapi.expansion.PlaceholderExpansion", () -> new PlaceholderApiIntegration());
        register(IntegrationTypes.PLACEHOLDER, "io.github.miniplaceholders.api.utils.TagsUtils", () -> new MiniPlaceholdersIntegration());
        register(IntegrationTypes.CHEST_SHOP, "com.ghostchu.quickshop.api.event.general.ShopItemMatchEvent", () -> new QuickShopHikariIntegration());
        register(IntegrationTypes.EVENT, "dev.geco.gsit.api.GSitAPI", () -> new GSitIntegration());
        register(IntegrationTypes.EVENT, "bodyhealth.api.BodyHealthAPI", () -> new BodyHealthIntegration());
    }

    public void loadIntegrations() {
        integrationRegistry.getAllIntegrations()
                .forEach(integration -> {
                    try {
                        integration.onLoad();
                    } catch (Throwable e) {
                        Logger.logErr("Failed loading integration: %s".formatted(integration.getId()));
                        Logger.logAndTrackErr(e);
                    }
                });
    }

    public void enableIntegrations() {
        integrationRegistry.getAllIntegrations()
                .forEach(integration -> {
                    try {
                        integration.onEnable();
                    } catch (Throwable e) {
                        Logger.logErr("Failed loading integration: %s".formatted(integration.getId()));
                        Logger.logAndTrackErr(e);
                    }
                });
    }

    @Override
    public <T extends Integration> void register(IntegrationType<? extends T> type, T integration) {
        String fullId = (type.name() + "." + integration.getId()).toLowerCase(Locale.ROOT);
        if (!integration.isEnabled() || Config.config().integrationBlacklist()
                .stream()
                .filter(Objects::nonNull)
                .map(string -> string.toLowerCase(Locale.ROOT))
                .anyMatch(fullId::contains)) {
            return;
        }

        Logger.log("Registering integration " + integration.getId() + " with type " + type.name() + " integration");
        integrationRegistry.register(type, integration);
    }

    /**
     * Use this one when loading internally. By some reason you can get class loading issues even though this should be dynamic
     *
     * @param type               The integration type
     * @param classNamePredicate A class name to check if it exists
     * @param tSupplier          Constructor for integration
     * @param <T>                The integration type
     */
    private <T extends Integration> void register(IntegrationType<? extends T> type, String classNamePredicate, Supplier<T> tSupplier) {
        if (!ClassUtil.exists(classNamePredicate)) {
            return;
        }
        register(type, tSupplier.get());
    }

    public void clear() {
        integrationRegistry.clear();
    }

    public <T extends Integration> Set<T> retrieve(IntegrationType<T> type) {
        Set<T> integrations = integrationRegistry.getIntegrations(type);
        if (integrations.isEmpty()) {
            return Set.of();
        }
        return integrations;
    }

    public IntegrationRegistry getIntegrationRegistry() {
        return this.integrationRegistry;
    }
}
