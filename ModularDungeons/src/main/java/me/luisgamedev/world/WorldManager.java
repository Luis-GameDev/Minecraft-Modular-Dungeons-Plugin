package me.luisgamedev.world;

import me.luisgamedev.ModularDungeons;
import me.luisgamedev.dungeon.PlacedTile;
import com.sk89q.worldedit.function.operation.Operation;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import java.io.File;
import java.io.FileInputStream;



import java.util.List;
import java.util.function.Consumer;

public class WorldManager {

    public static void createDungeonWorld(String worldName, Consumer<World> callback) {
        ModularDungeons.getInstance().getLogger().info("Creating new dungeon world: " + worldName);

        Bukkit.getScheduler().runTask(ModularDungeons.getInstance(), () -> {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);

            World world = creator.createWorld();

            if (world == null) {
                ModularDungeons.getInstance().getLogger().warning("Failed to create world: " + worldName);
                return;
            }

            ModularDungeons.getInstance().getLogger().info("World created: " + worldName);
            callback.accept(world);
        });
    }

    public static void placeDungeonTiles(World world, List<PlacedTile> tiles, Runnable onFinished) {
        // Sicherstellen, dass die WorldEdit-Operationen synchron im Haupt-Thread ausgeführt werden
        Bukkit.getScheduler().runTask(ModularDungeons.getInstance(), () -> {
            try {
                for (PlacedTile placed : tiles) {
                    File file = placed.getTileData().getSchematicFile();
                    ClipboardFormat format = ClipboardFormats.findByFile(file);
                    if (format == null) continue;

                    try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                        Clipboard clipboard = reader.read();
                        clipboard.setOrigin(BlockVector3.ZERO);

                        ClipboardHolder holder = new ClipboardHolder(clipboard);
                        holder.setTransform(new AffineTransform().rotateY(placed.getRotation()));

                        EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));
                        Operation operation = holder
                                .createPaste(BukkitAdapter.adapt(world))
                                .to(BlockVector3.at(
                                        placed.getPosition().getBlockX(),
                                        placed.getPosition().getBlockY(),
                                        placed.getPosition().getBlockZ()))
                                .ignoreAirBlocks(false)
                                .build();

                        Operations.complete(operation);
                        editSession.close();
                    }
                }

                // Rückkehr in Bukkit-Thread
                Bukkit.getScheduler().runTask(ModularDungeons.getInstance(), onFinished);

            } catch (Exception e) {
                ModularDungeons.getInstance().getLogger().severe("Error while pasting dungeon: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

}
