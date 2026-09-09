package org.mmga.uglobal.client;

/** Short visual-only kick, expressed in degrees; zero after 240 ms. */
public final class ShotEnvelope {
    private ShotEnvelope() {}
    public static float yaw(double seconds) {
        return (float) (Math.sin(seconds * 95) * 0.28 * fade(seconds));
    }
    public static float pitch(double seconds) {
        return (float) ((-0.6 + Math.sin(seconds * 80) * 0.18) * fade(seconds));
    }
    private static double fade(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0 || seconds >= 0.24) return 0;
        double remaining = 1 - seconds / 0.24;
        return remaining * remaining;
    }
}
