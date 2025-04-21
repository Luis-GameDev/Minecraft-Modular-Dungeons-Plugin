package me.luisgamedev.dungeon;

import me.luisgamedev.tiles.TileData;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;

public class PlacedTile {
    private final TileData tileData;
    private final Vector position;
    private final int rotation; // 0, 90, 180, 270
    private final BlockFace connectedFrom;

    public PlacedTile(TileData tileData, Vector position, int rotation, BlockFace connectedFrom) {
        this.tileData = tileData;
        this.position = position;
        this.rotation = rotation;
        this.connectedFrom = connectedFrom;
    }

    public TileData getTileData() {
        return tileData;
    }

    public Vector getPosition() {
        return position;
    }

    public int getRotation() {
        return rotation;
    }

    public BlockFace getConnectedFrom() {
        return connectedFrom;
    }
}
