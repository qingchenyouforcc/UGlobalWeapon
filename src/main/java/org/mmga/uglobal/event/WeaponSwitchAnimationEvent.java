package org.mmga.uglobal.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.utils.RpgAnimation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Plays the raise animation without cancelling vanilla hotbar switching. */
public final class WeaponSwitchAnimationEvent implements Listener {
    private final Map<UUID, BukkitRunnable> pendingRaises = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        cancelPending(player);
        RpgAnimation.cancel(player);
        RpgAnimation.setReady(player, false);

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (!RpgAnimation.isRpg(newItem)) return;

        scheduleRaise(player);
    }

    private void scheduleRaise(Player player) {
        cancelPending(player);

        // Do not cancel the event or call setHeldItemSlot: the client must receive one
        // authoritative slot change, otherwise it briefly snaps back to the old item.
        BukkitRunnable raise = new BukkitRunnable() {
            @Override
            public void run() {
                pendingRaises.remove(player.getUniqueId());
                if (!player.isOnline()) return;
                ItemStack selected = player.getInventory().getItemInMainHand();
                if (RpgAnimation.isRpg(selected)) RpgAnimation.raise(player, selected);
            }
        };
        pendingRaises.put(player.getUniqueId(), raise);
        raise.runTaskLater(Weapon.getInstance(), 1L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Weapon.getInstance().getLogger().info("Resource pack status for "
                + event.getPlayer().getName() + ": " + event.getStatus());
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            scheduleRaise(event.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRaise(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelPending(event.getPlayer());
        RpgAnimation.cancel(event.getPlayer());
        RpgAnimation.setReady(event.getPlayer(), false);
    }

    private void cancelPending(Player player) {
        BukkitRunnable pending = pendingRaises.remove(player.getUniqueId());
        if (pending != null) pending.cancel();
    }

}
