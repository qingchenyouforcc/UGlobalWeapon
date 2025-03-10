package org.mmga.uglobal.weapon;

import org.bukkit.ChatColor;

import java.util.Map;

public class Gun {
    // [枪械名称，{弹药量, 换弹时间, 伤害}]
    Map<String, int[]> GunConfig = Map.of(
            ChatColor.RED + "AK-47", new int[]{30, 3, 6},
            ChatColor.RED + "M4A1", new int[]{30, 2, 4}
    );

    private int currentAmmo;
    private boolean isReloading;

    public int getCurrentAmmo() {
        return currentAmmo;
    }
    public boolean isReloading() {
        return isReloading;
    }



}
