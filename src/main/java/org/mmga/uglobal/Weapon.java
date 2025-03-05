package org.mmga.uglobal;

import org.bukkit.plugin.java.JavaPlugin;
import org.mmga.uglobal.command.CommandTabCompleter;
import org.mmga.uglobal.command.UGlobalWeaponCommand;
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
        Objects.requireNonNull(getCommand("UGlobalWeapon")).setExecutor(new UGlobalWeaponCommand());
        getServer().getPluginManager().registerEvents(new FireballInteractionListener(), this);
        if (this.getCommand("UGlobalWeapon") != null) {
            Objects.requireNonNull(this.getCommand("UGlobalWeapon")).setTabCompleter(new CommandTabCompleter());
        }
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
