package dev.jsinco.brewery.bukkit.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.jsinco.brewery.api.effect.DrunkState;
import dev.jsinco.brewery.api.effect.DrunksManager;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.util.BukkitMessageUtil;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.configuration.EventSection;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import dev.jsinco.brewery.effect.DrunkStateImpl;
import dev.jsinco.brewery.util.MessageUtil;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Random;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final static Random RANDOM = new Random();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(PlayerConnectionValidateLoginEvent event) {
        if (!FeaturesConfig.test(FeatureFlag.BREW_EFFECTS, null)) {
            return;
        }
        PlayerProfile profile = null;
        if (event.getConnection() instanceof PlayerLoginConnection connection) {
            profile = connection.getAuthenticatedProfile();
        } else if (event.getConnection() instanceof PlayerConfigurationConnection connection) {
            profile = connection.getProfile();
        }
        if (profile == null) {
            return;
        }
        UUID playerUuid = profile.getId();
        if (playerUuid == null) {
            return;
        }
        DrunksManager drunksManager = TheBrewingProject.getInstance().getDrunksManager();
        DrunkState drunkState = drunksManager.getDrunkState(playerUuid);
        String playerName = profile.getName();
        if (drunksManager.isPassedOut(playerUuid)) {
            String kickEventMessage = EventSection.events().kickEvent().kickEventMessage();
            TagResolver tagResolver = BukkitMessageUtil.getPlayerTagResolver(Bukkit.getOfflinePlayer(profile.getId()));
            Component playerKickMessage = kickEventMessage == null ?
                    Component.translatable("tbp.events.default-kick-event-message", Argument.tagResolver(tagResolver))
                    : MessageUtil.miniMessage(kickEventMessage, tagResolver);
            event.kickMessage(GlobalTranslator.render(playerKickMessage, Config.config().language()));
            return;
        }
        EventSection.DrunkenJoinEvent joinEvent = EventSection.events().drunkenJoinDeny();
        if (joinEvent.enabled() && drunkState != null && joinEvent.probability().evaluate(DrunkStateImpl.compileVariables(drunkState.modifiers(), null, 0D)).probability() > RANDOM.nextDouble(100)) {
            event.kickMessage(
                    GlobalTranslator.render(
                            Component.translatable("tbp.events.drunken-join-deny-message", Argument.tagResolver(Placeholder.unparsed("player_name", playerName == null ? "" : playerName))),
                            Config.config().language()
                    )
            );
        }
    }
}
