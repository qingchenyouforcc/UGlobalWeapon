package org.mmga.uglobal.weapon;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;
public final class WeaponGeometry {
    private static Location point(Player player,double length) {
        Vector forward=player.getEyeLocation().getDirection();
        Vector right=forward.clone().crossProduct(new Vector(0,1,0));
        if (right.lengthSquared()<1e-8) right=new Vector(1,0,0);
        right.normalize(); Vector up=right.clone().crossProduct(forward).normalize();
        return player.getEyeLocation().add(forward.multiply(length))
                .add(right.multiply(player.getMainHand()==MainHand.LEFT ? -.30 : .30)).add(up.multiply(-.25));
    }
    public static Location muzzle(Player player) { return point(player,1.05); }
    public static Location breech(Player player) { return point(player,.05); }
}
