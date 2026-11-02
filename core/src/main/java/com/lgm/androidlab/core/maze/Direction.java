package com.lgm.androidlab.core.maze;

/**
 * As quatro direções cardeais, com o bit usado na máscara de passagens de cada célula.
 */
public enum Direction {
    NORTH(0, -1, 1),
    EAST(1, 0, 2),
    SOUTH(0, 1, 4),
    WEST(-1, 0, 8);

    private final int dx;
    private final int dy;
    private final int bit;

    Direction(int dx, int dy, int bit) {
        this.dx = dx;
        this.dy = dy;
        this.bit = bit;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int bit() {
        return bit;
    }

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }
}
