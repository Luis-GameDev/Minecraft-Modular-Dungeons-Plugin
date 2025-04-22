package me.luisgamedev.dungeon;

import me.luisgamedev.ModularDungeons;
import me.luisgamedev.tiles.TileData;
import me.luisgamedev.tiles.TileLoader;
import me.luisgamedev.tiles.TileType;
import me.luisgamedev.tiles.TileUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class DungeonBuilder {

    private final String setName;
    private final List<PlacedTile> placedTiles = new ArrayList<>();
    private final List<TileData> availableTiles;
    private final int mainPathLength;

    public DungeonBuilder(String setName) {
        this.setName = setName;
        // Sicherstellen, dass availableTiles eine veränderbare Liste ist
        this.availableTiles = new ArrayList<>(TileLoader.getLoadedTiles().getOrDefault(setName, new ArrayList<>()));
        this.mainPathLength = ModularDungeons.getInstance().getConfig().getInt("dungeons." + setName + ".mainpathlength", 10);
    }

    public void buildMainPath() {
        TileData startTile = getTileByType(TileType.START);
        if (startTile == null) {
            ModularDungeons.getInstance().getLogger().warning("No START tile found for set: " + setName);
            return;
        }

        Vector origin = new Vector(0, 64, 0);
        placedTiles.add(new PlacedTile(startTile, origin, 0, null));

        for (int i = 1; i < mainPathLength - 1; i++) {
            placeNextMainPathTile(TileType.ROADS);
        }

        placeNextMainPathTile(TileType.END);
    }

    private void placeNextMainPathTile(TileType type) {
        PlacedTile last = placedTiles.get(placedTiles.size() - 1);
        List<BlockFace> lastConnectors = last.getTileData().getConnectors();

        for (BlockFace lastConnector : lastConnectors) {
            BlockFace incomingSide = lastConnector.getOppositeFace();

            List<TileData> candidates = new ArrayList<>(getTilesByType(type));
            Collections.shuffle(candidates);

            for (TileData candidate : candidates) {
                for (BlockFace candidateConnector : candidate.getConnectors()) {
                    if (!TileUtils.isOpposite(candidateConnector, incomingSide)) continue;

                    int rotation = TileUtils.getRotationToMatch(incomingSide, candidateConnector);
                    Vector newPos = getNewTilePosition(last, lastConnector, candidate, rotation);

                    if (!isColliding(newPos, candidate, rotation)) {
                        PlacedTile newTile = new PlacedTile(candidate, newPos, rotation, incomingSide);
                        placedTiles.add(newTile);

                        ModularDungeons.getInstance().getLogger().info(String.format(
                                "Placing tile of type %s at position: (%.1f, %.1f, %.1f)",
                                type.name(),
                                newPos.getX(), newPos.getY(), newPos.getZ()
                        ));
                        return;
                    }
                }
            }
        }

        ModularDungeons.getInstance().getLogger().warning("Failed to place tile of type " + type + " in main path!");
    }




    private Vector getNewTilePosition(PlacedTile anchor, BlockFace anchorOut, TileData nextTile, int rotation) {
        double offsetX = anchor.getTileData().getBoundingBox().getMaxX() - anchor.getTileData().getBoundingBox().getMinX();
        double offsetZ = anchor.getTileData().getBoundingBox().getMaxZ() - anchor.getTileData().getBoundingBox().getMinZ();

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
            if (newBox.overlaps(placedBox)) return true;
        }
        return false;
    }

    private TileData getTileByType(TileType type) {
        return availableTiles.stream().filter(t -> t.getType() == type).findFirst().orElse(null);
    }

    private List<TileData> getTilesByType(TileType type) {
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
