package dev.jsinco.brewery.bukkit.effect;

import dev.jsinco.brewery.bukkit.util.VectorUtil;
import dev.jsinco.brewery.configuration.EventSection;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.Random;

public record DrunkenImpulse(
        Vector pushDirection,
        double magnitude,
        double yawRate,
        double pitchRate
) {
    private static final EnumSet<Material> HARNESSES = EnumSet.of(
            Material.WHITE_HARNESS,
            Material.LIGHT_GRAY_HARNESS,
            Material.GRAY_HARNESS,
            Material.BLACK_HARNESS,
            Material.BROWN_HARNESS,
            Material.RED_HARNESS,
            Material.ORANGE_HARNESS,
            Material.YELLOW_HARNESS,
            Material.LIME_HARNESS,
            Material.GREEN_HARNESS,
            Material.CYAN_HARNESS,
            Material.LIGHT_BLUE_HARNESS,
            Material.BLUE_HARNESS,
            Material.PURPLE_HARNESS,
            Material.MAGENTA_HARNESS,
            Material.PINK_HARNESS
    );

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
    private boolean isSaddled(Mob entity) {
        return entity.getEquipment().getItem(EquipmentSlot.SADDLE).getType() == Material.SADDLE;
    }
    private boolean isHarnessed(HappyGhast entity) {
        return HARNESSES.contains(entity.getEquipment().getItem(EquipmentSlot.BODY).getType());
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
