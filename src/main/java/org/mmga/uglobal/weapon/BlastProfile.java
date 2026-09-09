package org.mmga.uglobal.weapon;

/** Bounded impact tuning shared by the blast and its visual pressure wave. */
public record BlastProfile(float primary, float secondary, double radius) {
    public static BlastProfile forPower(float power) {
        if (!Float.isFinite(power) || power <= 0) throw new IllegalArgumentException("Invalid blast power");
        float primary = Math.min(18F, power * 1.1F);
        return new BlastProfile(primary, primary * 0.4F, Math.min(24, Math.max(5, power * 1.6)));
    }
    public double impulse(double distance) {
        return 1.25 * Math.max(0, 1 - Math.max(0, distance) / radius);
    }
}
