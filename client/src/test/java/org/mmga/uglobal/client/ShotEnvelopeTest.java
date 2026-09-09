package org.mmga.uglobal.client;

public final class ShotEnvelopeTest {
    public static void main(String[] args) {
        if (HeatLight.minimumBlockLight("uglobalweapon","item/palette/olive")!=0
                || HeatLight.minimumBlockLight("minecraft","item/heat/3/olive")!=0
                || HeatLight.minimumBlockLight("uglobalweapon","item/heat/3/olive")!=12
                || HeatLight.minimumBlockLight("uglobalweapon","item/heat/1/olive")!=7)
            throw new AssertionError("Cold/unrelated surfaces must not glow");
        for (int ms = 0; ms <= 1000; ms++) {
            double time = ms / 1000.0;
            float yaw = ShotEnvelope.yaw(time), pitch = ShotEnvelope.pitch(time);
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(yaw) > 0.28 || Math.abs(pitch) > 0.78)
                throw new AssertionError("Shake exceeds limit");
            if (ms >= 240 && (yaw != 0 || pitch != 0)) throw new AssertionError("Shake did not end");
        }
        if (ShotEnvelope.yaw(-1) != 0 || ShotEnvelope.pitch(-1) != 0) throw new AssertionError("Negative time");
        System.out.println("PASS: visual shake bounded and fully settled after 240 ms");
    }
}
