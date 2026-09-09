package org.mmga.uglobal.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mmga.uglobal.Weapon;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Small client-visible animation demo driven by CustomModelData frames. */
public final class RpgAnimation {
    public static final int IDLE = 1001;
    private static final int[] RAISE = {1002, 1003, 1004, 1005, 1006};
    private static final int[] RECOIL = {1020, 1021, 1022, 1023};
    private static final Map<UUID, BukkitTask> ACTIVE_ANIMATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Runnable> RESETS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> READY = new ConcurrentHashMap<>();

    private RpgAnimation() {}

    public static boolean isRpg(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = key();
        if (key == null) return false;
        String value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return "RPG".equals(value);
    }

    private static NamespacedKey key() {
        return Weapon.getInstance() == null ? null : new NamespacedKey(Weapon.getInstance(), "special_item");
    }

    public static void setModel(ItemStack item, int model) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta=item.getItemMeta();
        int heat=meta.hasCustomModelData() ? WeaponModel.heat(meta.getCustomModelData()) : 0;
        setRawModel(item,WeaponModel.withHeat(model,heat));
    }

    private static void setRawModel(ItemStack item, int model) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (org.mmga.uglobal.weapon.Annihilation.isWeapon(item) && model < 3000) model=3000+Math.floorMod(model,100);
        meta.setCustomModelData(model);
        meta.setEnchantmentGlintOverride(false);
        item.setItemMeta(meta);
    }

    public static void setHeat(ItemStack item,int heat) {
        if (!isRpg(item)) return;
        if (org.mmga.uglobal.weapon.Annihilation.isWeapon(item)) return;
        ItemMeta meta=item.getItemMeta();
        int model=meta.hasCustomModelData() ? meta.getCustomModelData() : IDLE;
        int updated=WeaponModel.withHeat(model,heat);
        if (model!=updated) setRawModel(item,updated);
    }

    public static void playFrames(Player player, ItemStack item, int[] frames, int finalFrame) {
        playFrames(player, item, frames, finalFrame, null);
    }

    private static void playFrames(Player player, ItemStack item, int[] frames, int finalFrame, Runnable onComplete) {
        cancel(player);
        final int slot = player.getInventory().getHeldItemSlot();
        final ItemStack expected = item.clone();
        setRawModel(expected, IDLE);
        RESETS.put(player.getUniqueId(), () -> {
            ItemStack current = player.getInventory().getItem(slot);
            ItemStack identity = current == null ? null : current.clone();
            setRawModel(identity, IDLE);
            if (expected.equals(identity)) {
                setModel(current,IDLE);player.getInventory().setItem(slot,current);
            }
        });
        BukkitTask task = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                ItemStack current = player.getInventory().getItem(slot);
                ItemStack identity = current == null ? null : current.clone();
                setRawModel(identity, IDLE);
                if (!player.isOnline() || player.getInventory().getHeldItemSlot() != slot
                        || !isRpg(current) || !expected.equals(identity)) {
                    ACTIVE_ANIMATIONS.remove(player.getUniqueId());
                    Runnable reset = RESETS.remove(player.getUniqueId());
                    if (reset != null) reset.run();
                    setReady(player, false);
                    cancel();
                    return;
                }
                // Fetch and write the actual slot on every frame; do not keep mutating
                // an ItemStack captured before inventory updates replaced it.
                boolean finished = index >= frames.length;
                ItemStack frame = current.clone();
                setModel(frame, finished ? finalFrame : frames[index++]);
                player.getInventory().setItem(slot, frame);
                if (finished) {
                    ACTIVE_ANIMATIONS.remove(player.getUniqueId());
                    RESETS.remove(player.getUniqueId());
                    if (onComplete != null) onComplete.run();
                    cancel();
                }
            }
        }.runTaskTimer(Weapon.getInstance(), 1L, onComplete == null ? 1L : 2L);
        ACTIVE_ANIMATIONS.put(player.getUniqueId(), task);
    }

    public static void raise(Player player, ItemStack item) {
        setReady(player, false);
        playFrames(player, item, RAISE, IDLE, () -> setReady(player, true));
    }

    public static void recoil(Player player, ItemStack item) {
        playFrames(player, item, RECOIL, IDLE);
    }

    public static void cancel(Player player) {
        BukkitTask task = ACTIVE_ANIMATIONS.remove(player.getUniqueId());
        if (task != null) task.cancel();
        Runnable reset = RESETS.remove(player.getUniqueId());
        if (reset != null) reset.run();
    }

    public static void setReady(Player player, boolean ready) {
        if (ready) READY.put(player.getUniqueId(), true);
        else READY.remove(player.getUniqueId());
    }

    /** Returns true only after the raise animation for the currently held RPG completed. */
    public static boolean isReady(Player player) {
        return READY.containsKey(player.getUniqueId()) && isRpg(player.getInventory().getItemInMainHand());
    }

    public static boolean isAnimating(Player player) {
        return ACTIVE_ANIMATIONS.containsKey(player.getUniqueId());
    }
}
