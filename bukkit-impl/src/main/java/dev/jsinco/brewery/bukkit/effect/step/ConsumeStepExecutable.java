package dev.jsinco.brewery.bukkit.effect.step;

import dev.jsinco.brewery.api.effect.ModifierConsume;
import dev.jsinco.brewery.api.effect.modifier.DrunkenModifier;
import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.ExecutionOutcome;
import dev.jsinco.brewery.api.event.step.ConsumeStep;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConsumeStepExecutable implements EventPropertyExecutable {

    private final Map<DrunkenModifier, Double> consumeModifiers;

    public ConsumeStepExecutable(Map<DrunkenModifier, Double> consumeModifiers) {
        this.consumeModifiers = consumeModifiers;
    }

    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        executeFor(contextPlayer);
        return ExecutionResult.CONTINUE;
    }

    @Override
    public ExecutionOutcome executeFor(UUID contextPlayer) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null || !FeaturesConfig.test(FeatureFlag.MODIFIER_CHANGE, player.getWorld().getName())) {
            return new ExecutionOutcome.Continue();
        }
        TheBrewingProject.getInstance().getDrunksManager().consume(contextPlayer, consumeModifiers.entrySet().stream()
                .map(entry -> new ModifierConsume(entry.getKey(), entry.getValue()))
                .toList()
        );
        return new ExecutionOutcome.Continue();
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public EventStepProperty toProperty() {
        return new ConsumeStep(consumeModifiers);
    }

    @Override
    public ExecutionContext context() {
        return ExecutionContext.PLAYER;
    }

    @Override
    public EventPropertyExecutable withSkipPoint(@Nullable EventPropertyExecutable point) {
        return this; // NO-OP
    }

}
