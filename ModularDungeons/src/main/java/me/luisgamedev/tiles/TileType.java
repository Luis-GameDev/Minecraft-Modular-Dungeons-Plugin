package me.luisgamedev.tiles;

public enum TileType {
    START,
    END,
    BOSS,
    ROADS,
    DEADEND_PASSIVE,
    DEADEND_FORCED;

    public static TileType fromString(String raw) {
        return switch (raw.toLowerCase()) {
            case "start" -> START;
            case "end" -> END;
            case "boss" -> BOSS;
            case "roads" -> ROADS;
            case "deadend_passive" -> DEADEND_PASSIVE;
            case "deadend_forced" -> DEADEND_FORCED;
            default -> throw new IllegalArgumentException("Unknown tile type: " + raw);
        };
    }
}
