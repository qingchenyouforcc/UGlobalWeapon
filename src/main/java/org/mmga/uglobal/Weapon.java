package org.mmga.uglobal;

import org.bukkit.plugin.java.JavaPlugin;
import org.mmga.uglobal.command.UGlobalWeapon;
import org.mmga.uglobal.utils.FireballInteractionListener;

import java.util.Objects;

public final class Weapon extends JavaPlugin {
    private static Weapon instance;

    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        getLogger().info("UGlobal Weapon Plugin Enabled");

        // Register commands
        Objects.requireNonNull(getCommand("UGlobalWeapon")).setExecutor(new UGlobalWeapon());
        getServer().getPluginManager().registerEvents(new FireballInteractionListener(), this);

    }

    @Override
    public void onDisable() {
        instance = this;
        // Plugin shutdown logic
        getLogger().info("UGlobal Weapon Plugin Disabled");
    }

    public static Weapon getInstance() {
        return instance;
    }
}
