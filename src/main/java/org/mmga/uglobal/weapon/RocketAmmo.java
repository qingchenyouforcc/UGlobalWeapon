package org.mmga.uglobal.weapon;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mmga.uglobal.Weapon;
import java.util.*;

public final class RocketAmmo implements Listener {
    private static NamespacedKey key(String name) { return new NamespacedKey(Weapon.getInstance(), name); }
    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD, amount);
        var meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "火箭弹");
        meta.setLore(List.of(ChatColor.GRAY + "RPG 专用弹药", ChatColor.DARK_GRAY + "右键方块顶面放置，右键弹体取回"));
        meta.setCustomModelData(2001);
        meta.getPersistentDataContainer().set(key("special_item"), PersistentDataType.STRING, "ROCKET_AMMO");
        item.setItemMeta(meta);
        return item;
    }
    public static boolean isAmmo(ItemStack item) {
        return item != null && item.getType() == Material.PRISMARINE_SHARD && item.hasItemMeta()
                && "ROCKET_AMMO".equals(item.getItemMeta().getPersistentDataContainer().get(key("special_item"), PersistentDataType.STRING));
    }
    public static int find(Player player) {
        for (int slot = 0; slot < 36; slot++) if (isAmmo(player.getInventory().getItem(slot))) return slot;
        return -1;
    }
    public static void configureDisplay(ItemDisplay display) {
        display.setItemStack(create(1));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        display.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(.75F),new Quaternionf()));
        display.setDisplayWidth(2); display.setDisplayHeight(2);
        display.setInvulnerable(true);
    }
    public static Interaction place(Location base) {
        World world=base.getWorld();
        ItemDisplay display=world.spawn(base.clone().add(0,.23,0), ItemDisplay.class, RocketAmmo::configureDisplay);
        try {
            return world.spawn(base, Interaction.class, hitbox -> {
                hitbox.setInteractionWidth(.9F); hitbox.setInteractionHeight(.5F); hitbox.setResponsive(true);
                hitbox.getPersistentDataContainer().set(key("placed_rocket"),PersistentDataType.STRING,display.getUniqueId().toString());
            });
        } catch (RuntimeException ex) { display.remove(); throw ex; }
    }
    public static boolean isPlaced(Entity entity) { return entity.getPersistentDataContainer().has(key("placed_rocket"),PersistentDataType.STRING); }
    private static Entity display(Entity hitbox) {
        String id=hitbox.getPersistentDataContainer().get(key("placed_rocket"),PersistentDataType.STRING);
        try { return id==null ? null : Bukkit.getEntity(UUID.fromString(id)); } catch (IllegalArgumentException ex) { return null; }
    }
    public static boolean retrieve(Player player, Entity hitbox) {
        if (!isPlaced(hitbox) || !hitbox.isValid()) return false;
        Entity display=display(hitbox);
        if (!(display instanceof ItemDisplay)) { hitbox.remove(); return false; }
        if (!player.getInventory().addItem(create(1)).isEmpty()) return false;
        hitbox.remove(); display.remove(); return true;
    }
    @EventHandler public void onPlace(PlayerInteractEvent event) {
        if (event.getHand()!=EquipmentSlot.HAND || event.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || !isAmmo(event.getItem()) || event.useInteractedBlock()==Event.Result.DENY) return;
        event.setCancelled(true);
        Player player=event.getPlayer();
        if (player.getGameMode()==GameMode.SPECTATOR || player.getGameMode()==GameMode.ADVENTURE
                || event.getBlockFace()!=BlockFace.UP || !event.getClickedBlock().getType().isSolid()
                || !event.getClickedBlock().getRelative(BlockFace.UP).isPassable()) return;
        Location base=event.getClickedBlock().getLocation().add(.5,0,.5);
        base.setY(event.getClickedBlock().getBoundingBox().getMaxY());base.setYaw(player.getLocation().getYaw());
        if (base.getWorld().getNearbyEntities(base,.5,.5,.5).stream().anyMatch(RocketAmmo::isPlaced)) return;
        place(base);
        if (player.getGameMode()!=GameMode.CREATIVE) {
            ItemStack remaining=player.getInventory().getItemInMainHand().clone();
            remaining.setAmount(remaining.getAmount()-1);player.getInventory().setItemInMainHand(remaining);
        }
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onPickup(PlayerInteractEntityEvent event) {
        if (!isPlaced(event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getHand()==EquipmentSlot.HAND && event.getPlayer().getGameMode()!=GameMode.SPECTATOR)
            retrieve(event.getPlayer(),event.getRightClicked());
    }
    private static void dropUnsupported(Location location, double radius) {
        Bukkit.getScheduler().runTask(Weapon.getInstance(), () -> {
            for (Entity entity:location.getWorld().getNearbyEntities(location,radius,radius,radius)) {
                if (!isPlaced(entity) || entity.getLocation().clone().add(0,-.05,0).getBlock().getType().isSolid()) continue;
                Entity display=display(entity);
                if (display instanceof ItemDisplay) { display.remove(); entity.getWorld().dropItem(entity.getLocation(),create(1)); }
                entity.remove();
            }
        });
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onSupportBreak(BlockBreakEvent event) { dropUnsupported(event.getBlock().getLocation().add(.5,1,.5),1.5); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBlast(EntityExplodeEvent event) { dropUnsupported(event.getLocation(),32); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBlockBlast(BlockExplodeEvent event) { dropUnsupported(event.getBlock().getLocation(),32); }
}
