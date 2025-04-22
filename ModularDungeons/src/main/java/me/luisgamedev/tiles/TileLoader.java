package me.luisgamedev.tiles;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import me.luisgamedev.ModularDungeons;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;

import java.io.*;
import java.util.*;

public class TileLoader {

    private static final Map<String, List<TileData>> loadedTiles = new HashMap<>();

    public static void loadAllTiles(File tilesFolder) {
        loadedTiles.clear();
        if (tilesFolder == null || !tilesFolder.exists()) return;

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
                    int spawnrate = Integer.parseInt(properties.getProperty("spawnrate", "1"));

                    ClipboardFormat format = ClipboardFormats.findByFile(schem);
                    if (format == null) continue;

                    Clipboard clipboard;
                    try (ClipboardReader reader = format.getReader(new FileInputStream(schem))) {
                        clipboard = reader.read();
                    }

                    // Wichtig: Hole die Dimensionen und die Ursprungsposition des Clipboards
                    BlockVector3 dimensions = clipboard.getDimensions();
                    BlockVector3 origin = clipboard.getOrigin();
                    BlockVector3 minPoint = clipboard.getMinimumPoint();

                    int width = dimensions.getBlockX();
                    int height = dimensions.getBlockY();
                    int length = dimensions.getBlockZ();

                    BoundingBox box = new BoundingBox(0, 0, 0, width - 1, height - 1, length - 1);

                    List<BlockFace> connectors = new ArrayList<>();
                    String connectorId = "minecraft:" + connectorMaterial.name().toLowerCase();
                    BlockType connectorType = BlockTypes.get(connectorId);

                    if (connectorType == null) {
                        ModularDungeons.getInstance().getLogger().warning("Could not find BlockType for: " + connectorId);
                        continue;
                    }

                    ModularDungeons.getInstance().getLogger().info("Scanning " + schem.getName() + " for connector blocks (expecting: " + connectorId + ")");
                    ModularDungeons.getInstance().getLogger().info("Clipboard dimensions: " + dimensions);
                    ModularDungeons.getInstance().getLogger().info("Clipboard origin: " + origin);
                    ModularDungeons.getInstance().getLogger().info("Clipboard minimum point: " + minPoint);

                    // Durchlaufe den gesamten Bereich des Clipboards
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                // Berechne die Position relativ zum Minimum des Clipboards
                                BlockVector3 pos = minPoint.add(x, y, z);
                                BlockState block;

                                try {
                                    block = clipboard.getBlock(pos);
                                } catch (Exception e) {
                                    ModularDungeons.getInstance().getLogger().warning(
                                            String.format("Error getting block at (%d,%d,%d): %s",
                                                    pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), e.getMessage())
                                    );
                                    continue;
                                }

                                if (block == null) {
                                    continue;
                                }

                                // Debugging-Ausgabe für jeden Block, der nicht Air ist
                                if (!block.getBlockType().getId().equals("minecraft:air")) {
                                    ModularDungeons.getInstance().getLogger().info(String.format(
                                            "NON-AIR BLOCK at (%d,%d,%d): %s",
                                            pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), block.getBlockType().getId()
                                    ));

                                    // Prüfe explizit, ob der Block-ID mit der erwarteten Connector-ID übereinstimmt
                                    if (block.getBlockType().getId().equalsIgnoreCase(connectorId)) {
                                        BlockFace face = getFacingDirection(x, z, width, length);
                                        ModularDungeons.getInstance().getLogger().info(String.format(
                                                "CONNECTOR FOUND at (%d,%d,%d) → face: %s → valid: %s",
                                                pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), face, face != BlockFace.SELF
                                        ));
                                        if (face != BlockFace.SELF) {
                                            connectors.add(face);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (connectors.isEmpty()) {
                        ModularDungeons.getInstance().getLogger().warning("No connector blocks found in " + schem.getName());
                    } else {
                        ModularDungeons.getInstance().getLogger().info("Found " + connectors.size() + " connectors in " + schem.getName());
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
        if (x <= 1) return BlockFace.WEST;
        if (x >= width - 2) return BlockFace.EAST;
        if (z <= 1) return BlockFace.NORTH;
        if (z >= length - 2) return BlockFace.SOUTH;
        return BlockFace.SELF;
    }

    public static Map<String, List<TileData>> getLoadedTiles() {
        return loadedTiles;
    }
}