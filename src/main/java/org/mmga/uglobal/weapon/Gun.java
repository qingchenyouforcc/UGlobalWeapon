package org.mmga.uglobal.weapon;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Shared, data-driven weapon spread values. Values are degrees of random cone spread. */
public final class Gun {
    public enum MovementState { CROUCH, STAND, WALK, RUN }

    public record SpreadConfig(double crouch, double stand, double walk, double run) {
        public double forState(MovementState state) {
            return switch (state) {
                case CROUCH -> crouch;
                case STAND -> stand;
                case WALK -> walk;
                case RUN -> run;
            };
        }
    }

    private static final Map<String, SpreadConfig> CONFIG = Map.of(
            ChatColor.RED + "AK-47", new SpreadConfig(1.2, 2.5, 7.0, 12.0),
            ChatColor.RED + "M4A1", new SpreadConfig(0.8, 1.8, 5.5, 10.0),
            ChatColor.RED + "RPG_null", new SpreadConfig(0.4, 1.0, 4.0, 8.0),
            ChatColor.RED + "RPG_normal", new SpreadConfig(0.4, 1.0, 4.0, 8.0),
            ChatColor.RED + "RPG_small", new SpreadConfig(0.3, 0.8, 3.0, 7.0),
            ChatColor.RED + "RPG_medium", new SpreadConfig(0.5, 1.2, 4.5, 9.0),
            ChatColor.RED + "RPG_large", new SpreadConfig(0.8, 1.8, 6.0, 12.0)
    );

    private Gun() {}

    public static SpreadConfig configFor(String weaponName) {
        return CONFIG.getOrDefault(weaponName, new SpreadConfig(0.0, 0.0, 0.0, 0.0));
    }

    public static MovementState movementState(Player player) {
        return movementState(player.isSneaking(), player.isSprinting(),
                PlayerMovementTracker.isMoving(player.getUniqueId()));
    }

    static MovementState movementState(boolean sneaking, boolean sprinting, boolean moving) {
        if (sneaking) return MovementState.CROUCH;
        if (!moving) return MovementState.STAND;
        return sprinting ? MovementState.RUN : MovementState.WALK;
    }

    /** Applies a uniformly distributed random cone offset around the supplied aim direction. */
    public static Vector applySpread(Player player, String weaponName, Vector direction) {
        double degrees = configFor(weaponName).forState(movementState(player));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return sampleCone(direction, degrees, random.nextDouble(), random.nextDouble());
    }

    /** Degrees is the cone half-angle; uniform cos(theta) gives uniform solid angle. */
    static Vector sampleCone(Vector direction, double degrees, double radialSample, double azimuthSample) {
        if (direction == null || !Double.isFinite(direction.getX())
                || !Double.isFinite(direction.getY()) || !Double.isFinite(direction.getZ())
                || !Double.isFinite(degrees) || degrees < 0.0 || degrees > 180.0
                || !(radialSample >= 0.0 && radialSample <= 1.0)
                || !(azimuthSample >= 0.0 && azimuthSample <= 1.0)) {
            throw new IllegalArgumentException("Invalid spread direction, angle or sample");
        }
        double max = Math.max(Math.abs(direction.getX()),
                Math.max(Math.abs(direction.getY()), Math.abs(direction.getZ())));
        if (max == 0.0) throw new IllegalArgumentException("Aim direction must be nonzero");
        Vector forward = new Vector(direction.getX() / max, direction.getY() / max,
                direction.getZ() / max).normalize();
        if (degrees == 0.0) return forward;
        double cosTheta = 1.0 - radialSample * (1.0 - Math.cos(Math.toRadians(degrees)));
        double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        double angle = azimuthSample * Math.PI * 2.0;
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0e-8) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = right.clone().crossProduct(forward).normalize();
        return forward.multiply(cosTheta).add(right.multiply(Math.cos(angle) * sinTheta))
                .add(up.multiply(Math.sin(angle) * sinTheta)).normalize();
    }
}
