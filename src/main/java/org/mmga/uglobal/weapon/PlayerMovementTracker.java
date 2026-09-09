package org.mmga.uglobal.weapon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks accepted horizontal movement instead of server-applied velocity. */
public final class PlayerMovementTracker implements Listener {
    private static final long MOVEMENT_GRACE_NANOS = 150_000_000L;
    private static final Map<UUID, Long> LAST_MOVEMENT = new HashMap<>();

    public static boolean isMoving(UUID playerId) {
        return isMoving(playerId, System.nanoTime());
    }

    static boolean isMoving(UUID playerId, long now) {
        Long last = LAST_MOVEMENT.get(playerId);
        return last != null && now - last < MOVEMENT_GRACE_NANOS;
    }

    static void recordMovement(UUID playerId, double dx, double dz, long now) {
        if (dx * dx + dz * dz > 1.0e-8) LAST_MOVEMENT.put(playerId, now);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || event.getTo() == null) return;
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        recordMovement(event.getPlayer().getUniqueId(), dx, dz, System.nanoTime());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        LAST_MOVEMENT.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        LAST_MOVEMENT.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        LAST_MOVEMENT.remove(event.getPlayer().getUniqueId());
    }

    public static void clear() {
        LAST_MOVEMENT.clear();
    }
}
