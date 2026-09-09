package org.mmga.uglobal;

import org.bukkit.plugin.java.JavaPlugin;
import org.mmga.uglobal.command.CommandTabCompleter;
import org.mmga.uglobal.command.UGlobalWeaponCommand;
import org.mmga.uglobal.event.PlayerInteractionEvent;
import org.mmga.uglobal.event.WeaponSwitchAnimationEvent;
import org.mmga.uglobal.utils.FireballInteractionListener;
import org.mmga.uglobal.weapon.PlayerMovementTracker;
import org.mmga.uglobal.weapon.RpgEffects;

import java.util.Objects;

public final class Weapon extends JavaPlugin {
    private static Weapon instance;

    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        getLogger().info("UGlobal Weapon Plugin Enabled");

        // Register Commands
        Objects.requireNonNull(getCommand("UGlobalWeapon")).setExecutor(new UGlobalWeaponCommand());
        if (this.getCommand("UGlobalWeapon") != null) {
            Objects.requireNonNull(this.getCommand("UGlobalWeapon")).setTabCompleter(new CommandTabCompleter());
        }

        // Register Listener
        getServer().getPluginManager().registerEvents(new FireballInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractionEvent(), this);
        getServer().getPluginManager().registerEvents(new WeaponSwitchAnimationEvent(), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementTracker(), this);
        getServer().getPluginManager().registerEvents(new RpgEffects(), this);
        var annihilation=new org.mmga.uglobal.weapon.Annihilation();
        getServer().getPluginManager().registerEvents(annihilation, this);
        getServer().getMessenger().registerIncomingPluginChannel(this,"uglobalweapon:charge",annihilation);
        getServer().getPluginManager().registerEvents(new org.mmga.uglobal.weapon.RocketAmmo(),this);
        getServer().getPluginManager().registerEvents(new org.mmga.uglobal.weapon.BlastAfterfire(),this);

    }

    @Override
    public void onDisable() {
        PlayerMovementTracker.clear();
        RpgEffects.shutdown();
        org.mmga.uglobal.weapon.Annihilation.shutdown();
        org.mmga.uglobal.weapon.RocketProjectiles.shutdown();
        org.mmga.uglobal.weapon.BlastAfterfire.shutdown();
        instance = this;
        // Plugin shutdown logic
        getLogger().info("UGlobal Weapon Plugin Disabled");
    }

    public static Weapon getInstance() {
        return instance;
    }
}
