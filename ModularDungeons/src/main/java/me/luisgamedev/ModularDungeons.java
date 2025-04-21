package me.luisgamedev;

import org.bukkit.plugin.java.JavaPlugin;

public class ModularDungeons extends JavaPlugin {

    private static ModularDungeons instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("ModularDungeons has been enabled!");
        
    }

    @Override
    public void onDisable() {
        getLogger().info("ModularDungeons has been disabled.");
    }

    public static ModularDungeons getInstance() {
        return instance;
    }
}
