package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.World;

import java.util.Random;
import java.util.UUID;

/** Standalone regression checks; run with tools/test-spread.ps1 from the workspace. */
public final class SpreadRegressionTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        float lastPrimary = 0;
        for (float power : new float[]{3, 5, 7, 15, 1000}) {
            BlastProfile blast = BlastProfile.forPower(power);
            check(blast.primary() >= lastPrimary && blast.primary() <= 18, "Blast strength ordering/cap");
            check(blast.secondary() > 0 && blast.secondary() < blast.primary(), "Secondary strength");
            check(blast.radius() <= 24 && blast.radius() >= 5, "Wave radius cap");
            check(blast.impulse(0) > blast.impulse(blast.radius() / 2), "Wave must weaken with distance");
            check(blast.impulse(blast.radius()) == 0 && blast.impulse(blast.radius() + 1) == 0, "Wave outside radius");
            lastPrimary = blast.primary();
        }
        for (float invalidPower : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            boolean rejected = false;
            try { BlastProfile.forPower(invalidPower); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Invalid blast accepted");
        }
        Vector[] axes = {new Vector(1, 0, 0), new Vector(0, 1, 0), new Vector(0, -1, 0),
                new Vector(1e-10, 1, 1e-10), new Vector(2, -3, 4)};
        Random random = new Random(9371);
        for (Vector axis : axes) {
            Vector original = axis.clone();
            Vector forward = axis.clone().normalize();
            double limit = Math.cos(Math.toRadians(12));
            double sum = 0;
            Vector lateral = new Vector();
            for (int i = 0; i < 20000; i++) {
                Vector result = Gun.sampleCone(axis, 12, random.nextDouble(), random.nextDouble());
                double dot = result.dot(forward);
                check(Math.abs(result.length() - 1) < 1e-12, "Non-unit direction");
                check(dot >= limit - 1e-12, "Shot outside cone");
                sum += dot;
                lateral.add(result.clone().subtract(forward.clone().multiply(dot)));
            }
            check(Math.abs(sum / 20000 - (1 + limit) / 2) < 0.0002, "Biased radial distribution");
            check(lateral.multiply(1.0 / 20000).length() < 0.003, "Biased lateral distribution");
            check(axis.equals(original), "Input direction mutated");
            check(Gun.sampleCone(axis, 0, 0.5, 0.5).distance(forward) < 1e-12, "Zero spread changed aim");
            check(Math.abs(Gun.sampleCone(axis, 12, 1, 0.3).dot(forward) - limit) < 1e-12,
                    "Configured cone boundary is not exact");
        }
        for (Vector invalid : new Vector[]{new Vector(), new Vector(Double.NaN, 0, 1),
                new Vector(Double.POSITIVE_INFINITY, 1, 0)}) {
            boolean rejected = false;
            try { Gun.sampleCone(invalid, 12, 0.5, 0.5); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Invalid aim accepted");
        }
        check(Gun.movementState(false, false, false) == Gun.MovementState.STAND, "Stand");
        check(Gun.movementState(false, false, true) == Gun.MovementState.WALK, "Walk");
        check(Gun.movementState(false, true, true) == Gun.MovementState.RUN, "Run");
        check(Gun.movementState(false, true, false) == Gun.MovementState.STAND, "Stationary sprint flag");
        check(Gun.movementState(true, true, true) == Gun.MovementState.CROUCH, "Crouch priority");
        UUID id = UUID.randomUUID();
        PlayerMovementTracker.recordMovement(id, 0, 0, 0);
        check(!PlayerMovementTracker.isMoving(id, 0), "Rotation-only event counted as movement");
        PlayerMovementTracker.recordMovement(id, 0.02, 0, 0);
        check(PlayerMovementTracker.isMoving(id, 100_000_000), "Slow movement missed");
        PlayerMovementTracker.recordMovement(id, 0, 0, 140_000_000);
        check(!PlayerMovementTracker.isMoving(id, 150_000_000), "Movement remained after stopping");
        PlayerMovementTracker.clear();
        check(!PlayerMovementTracker.isMoving(id, 0), "Cleanup failed");
        Location eye = new Location(null, 10, 20, 30);
        Vector direction = new Vector(2, 0, 0);
        RPG capture = new RPG() {
            @Override public void summonFireball(World world, Location spawn, Vector aim, float yield) {
                check(Math.abs(spawn.getX() - 11.5) < 1e-12, "Wrong muzzle offset");
                check(Math.abs(aim.length() - 1) < 1e-12, "Muzzle offset scaled aim");
            }
        };
        capture.summonRanged(null, eye, direction, 5);
        check(eye.getX() == 10 && eye.getY() == 20 && eye.getZ() == 30, "Eye location mutated");
        check(direction.equals(new Vector(2, 0, 0)), "Projectile direction mutated");
        System.out.println("PASS: 100000 cone samples, movement, projectile inputs, blast caps and wave attenuation");
    }
}
