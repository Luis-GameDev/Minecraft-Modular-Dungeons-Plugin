package me.luisgamedev.tiles;

import org.bukkit.block.BlockFace;

public class TileUtils {

    public static int getRotationToMatch(BlockFace from, BlockFace to) {
        BlockFace rotated = to;
        int rotation = 0;
        while (rotation < 360 && !isOpposite(rotated, from)) {
            rotated = rotateClockwise(rotated);
            rotation += 90;
        }
        return rotation % 360;
    }

    public static BlockFace rotateClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }

    public static boolean isOpposite(BlockFace a, BlockFace b) {
        return a.getOppositeFace() == b;
    }
}
