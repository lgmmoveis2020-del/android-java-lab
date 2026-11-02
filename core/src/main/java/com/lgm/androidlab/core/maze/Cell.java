package com.lgm.androidlab.core.maze;

/**
 * Coordenada de uma célula do labirinto.
 */
public record Cell(int x, int y) {

    public Cell neighbor(Direction dir) {
        return new Cell(x + dir.dx(), y + dir.dy());
    }

    public int manhattanDistance(Cell other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
