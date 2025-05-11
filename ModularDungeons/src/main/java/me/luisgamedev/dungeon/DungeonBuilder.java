package me.luisgamedev.dungeon;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.luisgamedev.ModularDungeons;
import me.luisgamedev.tiles.TileData;
import me.luisgamedev.tiles.TileLoader;
import me.luisgamedev.tiles.TileType;
import me.luisgamedev.tiles.TileUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;



public class DungeonBuilder {

    private final String setName;
    private final List<PlacedTile> placedTiles = new ArrayList<>();
    private final List<TileData> availableTiles;
    private final int mainPathLength;
    private World world;

    public DungeonBuilder(String setName, World worldd) {
        this.setName = setName;
        this.world = worldd;
        this.availableTiles = TileLoader.getLoadedTiles().getOrDefault(setName, new ArrayList<>());
        this.mainPathLength = ModularDungeons.getInstance().getConfig().getInt("dungeons." + setName + ".mainpathlength", 10);
    }

    public void buildMainPath() {
        ModularDungeons.getInstance().getLogger().info("Starting dungeon path creation...");

        TileData startTile = getTileByType(TileType.START);
        if (startTile == null) {
            ModularDungeons.getInstance().getLogger().warning("No START tile found for set: " + setName);
            return;
        }

        Vector origin = new Vector(0, 64, 0);
        placedTiles.add(new PlacedTile(startTile, origin, 0, null));

        ModularDungeons.getInstance().getLogger().info("Placed start tile at " + origin);

        for (int i = 1; i < mainPathLength - 1; i++) {
            ModularDungeons.getInstance().getLogger().info("Placing tile " + (i+1) + " of type ROADS");
            placeNextMainPathTile(TileType.ROADS);
        }

        ModularDungeons.getInstance().getLogger().info("Placing end tile");
        placeNextMainPathTile(TileType.END);
    }

    private void placeNextMainPathTile(TileType type) {
        PlacedTile last = placedTiles.get(placedTiles.size() - 1);
        List<BlockFace> lastConnectors = last.getTileData().getConnectors();

        ModularDungeons.getInstance().getLogger().info("Checking connectors for tile: " + last.getTileData().getType().name());

        for (BlockFace lastConnector : lastConnectors) {
            BlockFace incomingSide = lastConnector.getOppositeFace();

            ModularDungeons.getInstance().getLogger().info("Incoming side: " + incomingSide);

            List<TileData> candidates = getTilesByType(type);

            List<TileData> mutableCandidates = new ArrayList<>(candidates);

            Collections.shuffle(mutableCandidates);

            ModularDungeons.getInstance().getLogger().info("Found " + mutableCandidates.size() + " candidates for tile type: " + type);

            for (TileData candidate : mutableCandidates) {
                for (BlockFace candidateConnector : candidate.getConnectors()) {
                    ModularDungeons.getInstance().getLogger().info("Checking connector " + candidateConnector);

                    if (!TileUtils.isOpposite(candidateConnector, incomingSide)) {
                        ModularDungeons.getInstance().getLogger().info("Skipping candidate due to non-opposite connectors.");
                        continue;
                    }

                    int rotation = TileUtils.getRotationToMatch(incomingSide, candidateConnector);
                    Vector newPos = getNewTilePosition(last, lastConnector, candidate, rotation);

                    ModularDungeons.getInstance().getLogger().info("Calculated new position for tile: " + newPos);

                    if (!isColliding(newPos, candidate, rotation)) {
                        PlacedTile newTile = new PlacedTile(candidate, newPos, rotation, incomingSide);
                        placedTiles.add(newTile);
                        ModularDungeons.getInstance().getLogger().info("Placed new tile at position: " + newPos);

                        Location location = new Location(Bukkit.getWorld(world.getName()), newPos.getX(), newPos.getY(), newPos.getZ());
                        placeTileInWorld(newPos, candidate, location, rotation);
                        return;
                    } else {
                        ModularDungeons.getInstance().getLogger().info("Tile placement collision detected at position: " + newPos);
                    }
                }
            }
        }

        ModularDungeons.getInstance().getLogger().warning("Failed to place tile of type " + type + " in main path!");
    }

    private void placeTileInWorld(Vector position, TileData candidate, Location location, int rotation) {

        try {
            if (world == null) {
                ModularDungeons.getInstance().getLogger().warning("World not found, skipping tile placement.");
                return;
            }

            WorldEditPlugin wePlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
            if (wePlugin == null) {
                ModularDungeons.getInstance().getLogger().warning("WorldEdit plugin not found, skipping tile placement.");
                return;
            }

            BlockVector3 blockVector = BlockVector3.at(position.getX(), position.getY(), position.getZ());

            File schematicRaw = candidate.getSchematicFile();

            try {
                Clipboard clipboard;

                try (InputStream is = new FileInputStream(schematicRaw)) {
                    clipboard = ClipboardFormats.findByFile(schematicRaw).getReader(is).read();
                }

                BlockVector3 pasteVector = BlockVector3.at(
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                );

                ClipboardHolder holder = new ClipboardHolder(clipboard);
                holder.setTransform(new AffineTransform().rotateY(rotation));

                try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance()
                        .newEditSession(world)) {

                    Operation operation = holder
                            .createPaste(editSession)
                            .to(pasteVector)
                            .ignoreAirBlocks(false)
                            .build();

                    Operations.complete(operation);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

                ModularDungeons.getInstance().getLogger().info("Tile placed in world at " + position);
        } catch (Exception e) {
            ModularDungeons.getInstance().getLogger().warning("Failed to place tile in world: " + e.getMessage());
        }
    }


    private Vector getNewTilePosition(PlacedTile anchor, BlockFace anchorOut, TileData nextTile, int rotation) {
        double offsetX = anchor.getTileData().getBoundingBox().getMaxX() - anchor.getTileData().getBoundingBox().getMinX();
        double offsetZ = anchor.getTileData().getBoundingBox().getMaxZ() - anchor.getTileData().getBoundingBox().getMinZ();

        ModularDungeons.getInstance().getLogger().info("Offset X: " + offsetX + ", Offset Z: " + offsetZ);

        Vector offset = switch (anchorOut) {
            case NORTH -> new Vector(0, 0, -offsetZ);
            case SOUTH -> new Vector(0, 0, offsetZ);
            case EAST -> new Vector(offsetX, 0, 0);
            case WEST -> new Vector(-offsetX, 0, 0);
            default -> new Vector();
        };
        return anchor.getPosition().clone().add(offset);
    }

    private boolean isColliding(Vector position, TileData candidate, int rotation) {
        BoundingBox newBox = candidate.getBoundingBox().shift(position);
        for (PlacedTile placed : placedTiles) {
            BoundingBox placedBox = placed.getTileData().getBoundingBox().shift(placed.getPosition());
            if (newBox.overlaps(placedBox)) {
                ModularDungeons.getInstance().getLogger().info("Collision detected with placed tile at " + placed.getPosition());
                return true;
            }
        }
        return false;
    }

    private TileData getTileByType(TileType type) {
        ModularDungeons.getInstance().getLogger().info("Fetching tile of type: " + type);
        return availableTiles.stream().filter(t -> t.getType() == type).findFirst().orElse(null);
    }

    private List<TileData> getTilesByType(TileType type) {
        ModularDungeons.getInstance().getLogger().info("Fetching all tiles of type: " + type);
        return availableTiles.stream().filter(t -> t.getType() == type).toList();
    }

    public List<PlacedTile> getPlacedTiles() {
        return placedTiles;
    }

    public void printDungeonLayout() {
        ModularDungeons.getInstance().getLogger().info("Dungeon layout for set: " + setName);
        for (int i = 0; i < placedTiles.size(); i++) {
            PlacedTile tile = placedTiles.get(i);
            String msg = String.format(
                    "#%02d | %-10s | Pos: (%.1f, %.1f, %.1f) | Rot: %3d°",
                    i,
                    tile.getTileData().getType().name(),
                    tile.getPosition().getX(),
                    tile.getPosition().getY(),
                    tile.getPosition().getZ(),
                    tile.getRotation()
            );
            ModularDungeons.getInstance().getLogger().info(msg);
        }
    }
}
