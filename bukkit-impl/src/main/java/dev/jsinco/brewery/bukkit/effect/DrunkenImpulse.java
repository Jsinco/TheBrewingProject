package dev.jsinco.brewery.bukkit.effect;

import dev.jsinco.brewery.bukkit.util.VectorUtil;
import dev.jsinco.brewery.configuration.EventSection;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Random;

public record DrunkenImpulse(
        Vector pushDirection,
        double magnitude,
        double yawRate,
        double pitchRate
) {
    public static DrunkenImpulse generate(Random random,
                                          double minMagnitude, double maxMagnitude,
                                          double minTurnRate, double maxTurnRate
    ) {
        boolean invertYaw = random.nextBoolean();
        boolean invertPitch = random.nextBoolean();
        return new DrunkenImpulse(
                VectorUtil.randomUnitVector(random),
                random.nextDouble(minMagnitude, maxMagnitude),
                (invertYaw ? -1.0 : 1.0) * random.nextDouble(minTurnRate, maxTurnRate),
                (invertPitch ? -1.0 : 1.0) * random.nextDouble(minTurnRate, maxTurnRate)
        );
    }

    public static DrunkenImpulse lerp(DrunkenImpulse from, DrunkenImpulse to, double t) {
        return new DrunkenImpulse(
                VectorUtil.lerp(from.pushDirection, to.pushDirection, t),
                org.joml.Math.lerp(from.magnitude, to.magnitude, t),
                org.joml.Math.lerp(from.yawRate, to.yawRate, t),
                org.joml.Math.lerp(from.pitchRate, to.pitchRate, t)
        );
    }

    public void applyTo(Player player, boolean applyToElytra) {
        if (!player.isOnline() || player.isDead() || player.hasNoPhysics()) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            if (EventSection.events().stumbleInVehicles()) {
                applyToVehicle(vehicle);
            }
        } else if (player.isGliding()) {
            if (EventSection.events().stumbleWithElytra() && applyToElytra) {
                turnRandomlyOmnidirectional(player);
            }
        } else if (player.isOnGround()) {
            moveRandomlyHorizontal(player, 1.0);
        }
    }
    private void applyToVehicle(Entity vehicle) {
        if (vehicle.isDead() || !vehicle.isTicking() || vehicle.isInsideVehicle() || vehicle.hasNoPhysics()) {
            return;
        }
        switch (vehicle) {
            // Boat movement code is horrendously jank and controlled almost entirely client-side, and thanks to
            // MC-104494 it is impossible to move or rotate a boat without stopping it in place.
            case Minecart minecart ->
                    moveRandomlyHorizontal(minecart, 2.0);
            case Llama llama when llama.isTamed() && llama.isOnGround() ->
                    moveRandomlyHorizontal(llama, 2.0);
            case Camel camel when isSaddled(camel) && !camel.isSitting() && camel.isOnGround() ->
                    moveRandomlyHorizontal(camel, 2.0);
            case AbstractHorse horseLike when isSaddled(horseLike) && horseLike.isOnGround() ->
                    moveRandomlyHorizontal(horseLike, 2.0);
            case AbstractNautilus nautilus when isSaddled(nautilus) ->
                    moveRandomlyOmnidirectional(nautilus, 2.5);
            case Pig pig when isSaddled(pig) && pig.isOnGround() ->
                    moveRandomlyHorizontal(pig, 2.0);
            case Strider strider when isSaddled(strider) && !strider.isShivering() ->
                    moveRandomlyHorizontal(strider, 1.0);
            case HappyGhast happyGhast when isHarnessed(happyGhast) ->
                    moveRandomlyOmnidirectional(happyGhast, 2.0);
            default -> {}
        }
    }
    private static boolean isSaddled(Mob entity) {
        return hasEquippable(entity, EquipmentSlot.SADDLE);
    }
    private static boolean isHarnessed(HappyGhast entity) {
        return hasEquippable(entity, EquipmentSlot.BODY);
    }
    private static boolean hasEquippable(Mob entity, EquipmentSlot slot) {
        Equippable equippable = entity.getEquipment().getItem(slot).getData(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && equippable.slot() == slot) {
            RegistryKeySet<EntityType> allowed = equippable.allowedEntities();
            return allowed == null || allowed.contains(TypedKey.create(RegistryKey.ENTITY_TYPE, entity.getType().key()));
        }
        return false;
    }

    private void moveRandomlyHorizontal(Entity entity, double scale) {
        entity.setVelocity(VectorUtil.horizontalScaledBy(pushDirection, magnitude * scale));
    }

    private void moveRandomlyOmnidirectional(Entity entity, double scale) {
        entity.setVelocity(VectorUtil.omnidirectionalScaledBy(pushDirection, magnitude * scale));
    }

    private void turnRandomlyOmnidirectional(Entity entity) {
        entity.setRotation(
                entity.getYaw() + (float) Math.toDegrees(yawRate),
                Math.clamp(entity.getPitch() + (float) Math.toDegrees(pitchRate), -90.0f, 90.0f)
        );
    }

}
