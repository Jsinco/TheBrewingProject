package dev.jsinco.brewery.bukkit.recipe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import dev.jsinco.brewery.api.effect.DrunkState;
import dev.jsinco.brewery.api.effect.ModifierConsume;
import dev.jsinco.brewery.api.effect.modifier.DrunkenModifier;
import dev.jsinco.brewery.api.effect.modifier.ModifierDisplay;
import dev.jsinco.brewery.api.event.DrunkEvent;
import dev.jsinco.brewery.api.event.EventData;
import dev.jsinco.brewery.api.recipe.RecipeEffect;
import dev.jsinco.brewery.api.recipe.RecipeEffects;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.Holder;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.effect.ConsumedModifierDisplay;
import dev.jsinco.brewery.bukkit.effect.ModifierConsumePdcType;
import dev.jsinco.brewery.bukkit.util.BukkitMessageUtil;
import dev.jsinco.brewery.bukkit.util.EventUtil;
import dev.jsinco.brewery.bukkit.util.ListPersistentDataType;
import dev.jsinco.brewery.configuration.DrunkenModifierSection;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import dev.jsinco.brewery.effect.DrunksManagerImpl;
import dev.jsinco.brewery.util.MessageUtil;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecipeEffectsImpl implements RecipeEffects {

    public static final NamespacedKey COMMANDS = TheBrewingProject.key("commands");
    public static final NamespacedKey MESSAGE = TheBrewingProject.key("message");
    public static final NamespacedKey ACTION_BAR = TheBrewingProject.key("action_bar");
    public static final NamespacedKey TITLE = TheBrewingProject.key("titles");
    public static final NamespacedKey ALCOHOL = TheBrewingProject.key("alcohol");
    public static final NamespacedKey TOXINS = TheBrewingProject.key("toxins");
    public static final NamespacedKey MODIFIERS = TheBrewingProject.key("modifiers");
    public static final NamespacedKey EVENTS = TheBrewingProject.key("events");
    public static final NamespacedKey EFFECTS = TheBrewingProject.key("effects");
    private static final List<NamespacedKey> PDC_TYPES = List.of(COMMANDS, MESSAGE, ACTION_BAR, TITLE, ALCOHOL, TOXINS, EVENTS);

    public static final RecipeEffectsImpl GENERIC = new Builder()
            .effects(List.of())
            .build();

    private final @NonNull List<RecipeEffectImpl> effects;
    private final @Nullable String title;
    private final @Nullable String message;
    private final @Nullable String actionBar;
    private final @NonNull Map<DrunkenModifier, Double> modifiers;
    private final @NonNull List<EventData> events;

    private RecipeEffectsImpl(@NonNull List<RecipeEffectImpl> effects, @Nullable String title, @Nullable String message,
                              @Nullable String actionBar, @NonNull List<EventData> events, @NonNull Map<DrunkenModifier, Double> modifiers) {
        this.effects = effects;
        this.title = title;
        this.message = message;
        this.actionBar = actionBar;
        this.modifiers = modifiers;
        this.events = events;
    }

    public List<DrunkEvent> getEvents() {
        return events
                .stream()
                .map(EventUtil::fromData)
                .flatMap(Optional::stream)
                .toList();
    }

    public void applyTo(ItemStack itemStack) {
        itemStack.editPersistentDataContainer(this::applyTo);
    }

    private void applyTo(PersistentDataContainer container) {
        PDC_TYPES.forEach(container::remove);
        if (title != null) {
            container.set(TITLE, PersistentDataType.STRING, title);
        }
        if (message != null) {
            container.set(MESSAGE, PersistentDataType.STRING, message);
        }
        if (actionBar != null) {
            container.set(ACTION_BAR, PersistentDataType.STRING, actionBar);
        }
        if (!modifiers.isEmpty()) {
            container.set(MODIFIERS, ModifierConsumePdcType.LIST_INSTANCE, modifiers.entrySet()
                    .stream()
                    .map(entry -> new ModifierConsume(entry.getKey(), entry.getValue()))
                    .toList()
            );
        }
        if (!events.isEmpty()) {
            container.set(EVENTS, ListPersistentDataType.STRING_LIST, events.stream().map(EventData::serialized).toList());
        }
        if (!effects.isEmpty()) {
            container.set(EFFECTS, RecipeEffectPersistentDataType.INSTANCE, effects);
        }
    }

    public static Optional<RecipeEffectsImpl> fromEntity(Entity entity) {
        return fromPdc(entity.getPersistentDataContainer());
    }

    private static Optional<RecipeEffectsImpl> fromPdc(PersistentDataContainerView persistentDataContainer) {
        RecipeEffectsImpl.Builder builder = new RecipeEffectsImpl.Builder();
        Map<DrunkenModifier, Double> modifiers = new HashMap<>();
        DrunkenModifierSection.modifiers()
                .optionalModifier("alcohol")
                .filter(modifier -> persistentDataContainer.has(ALCOHOL, PersistentDataType.INTEGER))
                .ifPresent(modifier -> modifiers.put(modifier, (double) persistentDataContainer.get(ALCOHOL, PersistentDataType.INTEGER)));
        DrunkenModifierSection.modifiers()
                .optionalModifier("toxins")
                .filter(modifier -> persistentDataContainer.has(TOXINS, PersistentDataType.INTEGER))
                .ifPresent(modifier -> modifiers.put(modifier, (double) persistentDataContainer.get(TOXINS, PersistentDataType.INTEGER)));
        if (persistentDataContainer.has(TITLE)) {
            builder.title(persistentDataContainer.get(TITLE, PersistentDataType.STRING));
        }
        if (persistentDataContainer.has(MESSAGE)) {
            builder.message(persistentDataContainer.get(MESSAGE, PersistentDataType.STRING));
        }
        if (persistentDataContainer.has(ACTION_BAR)) {
            builder.actionBar(persistentDataContainer.get(ACTION_BAR, PersistentDataType.STRING));
        }
        List<ModifierConsume> consumeModifiers = persistentDataContainer.get(MODIFIERS, ModifierConsumePdcType.LIST_INSTANCE);
        if (consumeModifiers != null) {
            consumeModifiers.stream().filter(Objects::nonNull).forEach(modifierConsume -> modifiers.put(modifierConsume.modifier(), modifierConsume.value()));
        }
        builder.addModifiers(modifiers);
        builder.eventData(persistentDataContainer.getOrDefault(EVENTS, ListPersistentDataType.STRING_LIST, List.of())
                .stream()
                .map(EventData::deserialize)
                .collect(Collectors.toList())
        );
        if (persistentDataContainer.has(EFFECTS, RecipeEffectPersistentDataType.INSTANCE)) {
            builder.effects(persistentDataContainer.get(EFFECTS, RecipeEffectPersistentDataType.INSTANCE));
        }
        RecipeEffectsImpl output = builder.build();
        if (output.effects.isEmpty() && output.modifiers.isEmpty() && output.events.isEmpty()
                && output.actionBar == null && output.message == null && output.title == null) {
            return Optional.empty();
        }
        return Optional.of(builder.build());
    }

    public static Optional<RecipeEffectsImpl> fromItem(@NonNull ItemStack item) {
        return fromPdc(item.getPersistentDataContainer());
    }

    public void applyTo(Player player) {
        if (!FeaturesConfig.test(FeatureFlag.BREW_EFFECTS, player.getWorld().getName())) {
            return;
        }
        DrunksManagerImpl<?> drunksManager = TheBrewingProject.getInstance().getDrunksManager();
        DrunkState beforeState = drunksManager.getDrunkState(player.getUniqueId());
        DrunkState afterState = null;
        if (!player.hasPermission("brewery.override.drunk") && FeaturesConfig.test(FeatureFlag.MODIFIER_CHANGE, player.getWorld().getName())) {
            afterState = drunksManager.consume(player.getUniqueId(),
                    modifiers.entrySet().stream()
                            .map(entry -> new ModifierConsume(entry.getKey(), entry.getValue(), true))
                            .toList()
            );
        }
        if (title != null) {
            player.showTitle(Title.title(
                    MessageUtil.miniMessage(title,
                            BukkitMessageUtil.getPlayerTagResolver(player),
                            MessageUtil.numberedModifierTagResolver(modifiers, "consumed")
                    ),
                    Component.empty())
            );
        } else {
            ConsumedModifierDisplay.renderConsumeDisplay(player, ModifierDisplay.DisplayWindow.TITLE,
                    beforeState, afterState, modifiers);
        }
        if (message != null) {
            player.sendMessage(MessageUtil.miniMessage(message,
                    BukkitMessageUtil.getPlayerTagResolver(player),
                    MessageUtil.numberedModifierTagResolver(modifiers, "consumed")));
        } else {
            ConsumedModifierDisplay.renderConsumeDisplay(player, ModifierDisplay.DisplayWindow.CHAT,
                    beforeState, afterState, modifiers);
        }
        if (actionBar != null) {
            player.sendActionBar(MessageUtil.miniMessage(actionBar,
                    BukkitMessageUtil.getPlayerTagResolver(player),
                    MessageUtil.numberedModifierTagResolver(modifiers, "consumed")
            ));
        } else {
            ConsumedModifierDisplay.renderConsumeDisplay(player, ModifierDisplay.DisplayWindow.ACTION_BAR,
                    beforeState, afterState, modifiers);
        }
        if (player.hasPermission("brewery.override.effect")) {
            return;
        }
        getEvents().forEach(drunkEvent -> TheBrewingProject.getInstance().getDrunkEventExecutor().doDrunkEvent(player.getUniqueId(), drunkEvent));
        getEffects().stream()
                .map(RecipeEffectImpl::newPotionEffect)
                .forEach(player::addPotionEffect);
    }

    public void applyTo(Projectile projectile) {
        PersistentDataContainer persistentDataContainer = projectile.getPersistentDataContainer();
        applyTo(persistentDataContainer);
    }

    @Override
    public List<? extends RecipeEffect> effects() {
        return effects;
    }

    @Nullable
    @Override
    public String titleMessage() {
        return title;
    }

    @Nullable
    @Override
    public String chatMessage() {
        return message;
    }

    @Nullable
    @Override
    public String actionBarMessage() {
        return actionBar;
    }

    @Override
    public Map<DrunkenModifier, Double> modifiersChange() {
        return modifiers;
    }

    @Override
    public List<BreweryKey> events() {
        return events.stream().map(EventData::key).toList();
    }

    @Override
    public @Nullable Component formatMessage(String message, Holder.Player playerHolder) {
        return BukkitAdapter.toPlayer(playerHolder)
                .map(player ->
                        MessageUtil.miniMessage(message,
                                BukkitMessageUtil.getPlayerTagResolver(player),
                                MessageUtil.numberedModifierTagResolver(modifiers, "consumed"))
                ).orElse(null);
    }

    @NonNull
    public List<RecipeEffectImpl> getEffects() {
        return this.effects;
    }

    @Nullable
    public String getTitle() {
        return this.title;
    }

    @Nullable
    public String getMessage() {
        return this.message;
    }

    @Nullable
    public String getActionBar() {
        return this.actionBar;
    }

    @NonNull
    public Map<DrunkenModifier, Double> getModifiers() {
        return this.modifiers;
    }

    public static class Builder implements dev.jsinco.brewery.api.util.Builder<RecipeEffectsImpl> {

        private List<RecipeEffectImpl> effects = List.of();
        private @Nullable String title;
        private @Nullable String message;
        private @Nullable String actionBar;
        private @NonNull List<EventData> events = List.of();
        private final ImmutableMap.Builder<DrunkenModifier, Double> modifiers = new ImmutableMap.Builder<>();

        public Builder effects(@NonNull List<RecipeEffectImpl> effects) {
            Preconditions.checkNotNull(effects);
            this.effects = effects;
            return this;
        }

        public Builder events(@NonNull List<BreweryKey> events) {
            Preconditions.checkNotNull(events);
            this.events = events.stream().map(EventData::new).toList();
            return this;
        }

        public Builder eventData(@NonNull List<EventData> events) {
            Preconditions.checkNotNull(events);
            this.events = List.copyOf(events);
            return this;
        }

        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        public Builder message(@Nullable String message) {
            this.message = message;
            return this;
        }

        public Builder actionBar(@Nullable String actionBar) {
            this.actionBar = actionBar;
            return this;
        }

        public Builder addModifiers(Map<DrunkenModifier, Double> modifiers) {
            this.modifiers.putAll(modifiers);
            return this;
        }

        public RecipeEffectsImpl build() {
            return new RecipeEffectsImpl(effects, title, message, actionBar, events, modifiers.build());
        }

    }
}
