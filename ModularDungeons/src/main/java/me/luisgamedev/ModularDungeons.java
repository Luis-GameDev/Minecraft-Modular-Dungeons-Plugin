package me.luisgamedev;

import me.luisgamedev.commands.DungeonCommand;
import me.luisgamedev.tiles.TileLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ModularDungeons extends JavaPlugin {

    private static ModularDungeons instance;
    private File tilesFolder;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupTilesFolder();
        TileLoader.loadAllTiles(getTilesFolder());
        getCommand("dungeons").setExecutor(new DungeonCommand());

        getLogger().info("ModularDungeons has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ModularDungeons has been disabled.");
    }

    public static ModularDungeons getInstance() {
        return instance;
    }

    private void setupTilesFolder() {
        tilesFolder = new File(getDataFolder(), "tiles");
        if (!tilesFolder.exists()) {
            if (tilesFolder.mkdirs()) {
                getLogger().info("Created tiles folder at " + tilesFolder.getPath());
                File exampleSet = new File(tilesFolder, "example");
                exampleSet.mkdirs();
                getLogger().info("Created example tile set: " + exampleSet.getPath());
            } else {
                getLogger().warning("Could not create tiles folder!");
            }
        }
    }

    public File getTilesFolder() {
        return tilesFolder;
    }
}
