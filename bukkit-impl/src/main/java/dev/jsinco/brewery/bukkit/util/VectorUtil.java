package dev.jsinco.brewery.bukkit.util;

import dev.jsinco.brewery.api.vector.BreweryVector;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.Random;

public class VectorUtil {

    public static Vector lerp(Vector from, Vector to, double t) {
        return from.clone().multiply(1.0 - t).add(to.clone().multiply(t));
    }

    public static Vector randomUnitVector(Random random) {
        double yaw = random.nextDouble(Math.PI * 2);
        double pitch = random.nextDouble(-Math.PI / 2, Math.PI / 2);
        return toUnitVector(yaw, pitch);
    }
    /**
     * @param yaw Minecraft yaw, clockwise angle from +Z
     * @param pitch Minecraft pitch, upwards angle from horizon
     * @return Equivalent unit vector
     */
    public static Vector toUnitVector(double yaw, double pitch) {
        return new Vector(
                -Math.sin(yaw) * Math.cos(pitch),
                Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        );
    }
    public static Vector horizontalScaledBy(Vector unit, double magnitude) {
        return unit.clone().setY(0).normalize().multiply(magnitude);
    }
    public static Vector omnidirectionalScaledBy(Vector unit, double magnitude) {
        return unit.clone().multiply(magnitude);
    }

    /**
     * Will not mutate the initial vector
     *
     * @param initial
     * @param transformation
     * @return A new transformed vector
     */
    public static Vector3i transform(Vector3i initial, Matrix3d transformation) {
        Vector3d transformedVector = transformation.transform(new Vector3d(initial));
        return new Vector3i(
                (int) Math.round(transformedVector.x()),
                (int) Math.round(transformedVector.y()),
                (int) Math.round(transformedVector.z())
        );
    }

    public static Vector3i toJoml(BreweryVector vector) {
        return new Vector3i(vector.x(), vector.y(), vector.z());
    }

    public static BreweryVector toBreweryVector(Vector3i vector) {
        return new BreweryVector(vector.x(), vector.y(), vector.z());
    }

}
