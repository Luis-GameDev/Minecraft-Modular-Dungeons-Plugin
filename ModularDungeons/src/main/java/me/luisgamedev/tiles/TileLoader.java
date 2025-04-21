package me.luisgamedev.tiles;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import me.luisgamedev.ModularDungeons;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.world.block.BlockState;

import java.io.*;
import java.util.*;

public class TileLoader {

    private static final Map<String, List<TileData>> loadedTiles = new HashMap<>();

    public static void loadAllTiles(File tilesFolder) {
        loadedTiles.clear();
        if (!tilesFolder.exists()) return;

        File[] sets = tilesFolder.listFiles(File::isDirectory);
        if (sets == null) return;

        for (File setFolder : sets) {
            String setName = setFolder.getName();
            List<TileData> tileList = new ArrayList<>();

            // Lade connectorblock aus config.yml
            Material connectorMaterial = Material.DIAMOND_BLOCK;
            String configPath = "dungeons." + setName + ".connectorblock";
            if (ModularDungeons.getInstance().getConfig().isString(configPath)) {
                try {
                    connectorMaterial = Material.valueOf(ModularDungeons.getInstance().getConfig().getString(configPath).toUpperCase());
                } catch (IllegalArgumentException e) {
                    ModularDungeons.getInstance().getLogger().warning("Invalid connectorblock for " + setName);
                }
            }

            File[] schems = setFolder.listFiles((dir, name) -> name.endsWith(".schem"));
            if (schems == null) continue;

            for (File schem : schems) {
                String baseName = schem.getName().replace(".schem", "");
                File props = new File(setFolder, baseName + ".properties");

                if (!props.exists()) {
                    ModularDungeons.getInstance().getLogger().warning("Missing properties for " + schem.getName());
                    continue;
                }

                try {
                    Properties properties = new Properties();
                    try (FileReader reader = new FileReader(props)) {
                        properties.load(reader);
                    }

                    TileType type = TileType.fromString(properties.getProperty("type"));
                    int spawnrate = Integer.parseInt(properties.getProperty("spawnrate"));

                    ClipboardFormat format = ClipboardFormats.findByFile(schem);
                    if (format == null) continue;

                    Clipboard clipboard;
                    try (ClipboardReader reader = format.getReader(new FileInputStream(schem))) {
                        clipboard = reader.read();
                    }

                    int width = clipboard.getDimensions().getX();
                    int height = clipboard.getDimensions().getY();
                    int length = clipboard.getDimensions().getZ();

                    BoundingBox box = new BoundingBox(0, 0, 0, width - 1, height - 1, length - 1);

                    List<BlockFace> connectors = new ArrayList<>();
                    BlockType connectorType = BlockType.REGISTRY.get("minecraft:" + connectorMaterial.name().toLowerCase());

                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                BlockVector3 pos = BlockVector3.at(x, y, z);
                                BlockState block = clipboard.getBlock(pos);

                                if (block.getBlockType().equals(connectorType)) {
                                    BlockFace face = getFacingDirection(x, z, width, length);
                                    if (face != BlockFace.SELF) {
                                        connectors.add(face);
                                    }
                                }
                            }
                        }
                    }

                    if (connectors.isEmpty()) {
                        ModularDungeons.getInstance().getLogger().warning("No connector blocks found in " + schem.getName());
                    }

                    TileData tile = new TileData(schem, type, spawnrate, box, connectors);
                    tileList.add(tile);

                } catch (Exception e) {
                    ModularDungeons.getInstance().getLogger().warning("Error loading tile " + schem.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (!tileList.isEmpty()) {
                loadedTiles.put(setName, tileList);
                ModularDungeons.getInstance().getLogger().info("Loaded " + tileList.size() + " tiles from set '" + setName + "'");
            }
        }
    }

    private static BlockFace getFacingDirection(int x, int z, int width, int length) {
        if (x == 0) return BlockFace.WEST;
        if (x == width - 1) return BlockFace.EAST;
        if (z == 0) return BlockFace.NORTH;
        if (z == length - 1) return BlockFace.SOUTH;
        return BlockFace.SELF; // Nicht an der Außenseite
    }

    public static Map<String, List<TileData>> getLoadedTiles() {
        return loadedTiles;
    }
}
