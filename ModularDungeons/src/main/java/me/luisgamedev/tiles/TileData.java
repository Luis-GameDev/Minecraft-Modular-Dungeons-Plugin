package me.luisgamedev.tiles;

import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import org.bukkit.block.BlockFace;
import java.io.File;
import java.util.List;

public class TileData {

    private final File schematicFile;
    private final TileType type;
    private final int spawnRate;
    private final BoundingBox boundingBox;
    private final List<BlockFace> connectors;

    public TileData(File schematicFile, TileType type, int spawnRate, BoundingBox boundingBox, List<BlockFace> connectors) {
        this.schematicFile = schematicFile;
        this.type = type;
        this.spawnRate = spawnRate;
        this.boundingBox = boundingBox;
        this.connectors = connectors;
    }

    public File getSchematicFile() {
        return schematicFile;
    }

    public TileType getType() {
        return type;
    }

    public int getSpawnRate() {
        return spawnRate;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public List<BlockFace> getConnectors() {
        return connectors;
    }

}
