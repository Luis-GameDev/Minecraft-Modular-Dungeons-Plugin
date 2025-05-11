package me.luisgamedev.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import me.luisgamedev.ModularDungeons;
import me.luisgamedev.dungeon.DungeonBuilder;
import me.luisgamedev.dungeon.PlacedTile;
import me.luisgamedev.tiles.TileType;
import me.luisgamedev.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


import java.util.List;

public class DungeonCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /dungeons <setname>");
            return true;
        }

        String setName = args[0];

        player.sendMessage("§7Generating dungeon...");

        // Weltname mit Zeitstempel
        String worldName = "dungeon_" + setName + "_" + System.currentTimeMillis();

        WorldManager.createDungeonWorld(worldName, world -> {
            DungeonBuilder builder = new DungeonBuilder(setName, BukkitAdapter.adapt(world));
            builder.buildMainPath();

            List<PlacedTile> tiles = builder.getPlacedTiles();

            if (tiles.isEmpty()) {
                player.sendMessage("§cDungeon generation failed.");
                return;
            }

            ModularDungeons.getInstance().getLogger().info("Pasting dungeon into world: " + worldName);
            WorldManager.placeDungeonTiles(world, tiles, () -> {
                // Teleportiere den Spieler zum Start-Tile
                PlacedTile start = tiles.get(0);
                player.teleport(start.getPosition().toLocation(world));
                player.sendMessage("§aDungeon ready! Teleported to start.");
            });
        });

        return true;
    }
}
