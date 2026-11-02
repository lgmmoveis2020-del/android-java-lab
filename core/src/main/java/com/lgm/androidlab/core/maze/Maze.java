package com.lgm.androidlab.core.maze;

import java.util.ArrayList;
import java.util.List;

/**
 * Labirinto em grade. Cada célula guarda uma máscara de bits com as passagens abertas.
 * Um labirinto recém-criado tem todas as paredes fechadas.
 */
public final class Maze {

    private final int width;
    private final int height;
    private final byte[] passages;

    public Maze(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("largura e altura devem ser > 0");
        }
        this.width = width;
        this.height = height;
        this.passages = new byte[width * height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int cellCount() {
        return width * height;
    }

    public boolean contains(Cell cell) {
        return cell.x() >= 0 && cell.y() >= 0 && cell.x() < width && cell.y() < height;
    }

    public boolean isOpen(Cell cell, Direction dir) {
        checkBounds(cell);
        return (passages[index(cell)] & dir.bit()) != 0;
    }

    /** Abre a passagem entre {@code cell} e sua vizinha na direção {@code dir} (dos dois lados). */
    public void open(Cell cell, Direction dir) {
        checkBounds(cell);
        Cell other = cell.neighbor(dir);
        checkBounds(other);
        passages[index(cell)] |= (byte) dir.bit();
        passages[index(other)] |= (byte) dir.opposite().bit();
    }

    /** Vizinhas alcançáveis (com passagem aberta) a partir de uma célula. */
    public List<Cell> openNeighbors(Cell cell) {
        List<Cell> result = new ArrayList<>(4);
        for (Direction dir : Direction.values()) {
            if (isOpen(cell, dir)) {
                result.add(cell.neighbor(dir));
            }
        }
        return result;
    }

    /** Número total de passagens abertas (cada parede derrubada conta uma vez). */
    public int openPassageCount() {
        int total = 0;
        for (byte mask : passages) {
            total += Integer.bitCount(mask & 0xF);
        }
        return total / 2;
    }

    /** Desenho ASCII clássico com '+', '-' e '|'. */
    public String toAscii() {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < width; x++) {
            sb.append("+---");
        }
        sb.append("+\n");
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder("|");
            StringBuilder bottom = new StringBuilder("+");
            for (int x = 0; x < width; x++) {
                Cell c = new Cell(x, y);
                row.append("   ").append(isOpen(c, Direction.EAST) ? " " : "|");
                bottom.append(isOpen(c, Direction.SOUTH) ? "   " : "---").append("+");
            }
            sb.append(row).append('\n').append(bottom).append('\n');
        }
        return sb.toString();
    }

    private int index(Cell cell) {
        return cell.y() * width + cell.x();
    }

    private void checkBounds(Cell cell) {
        if (!contains(cell)) {
            throw new IndexOutOfBoundsException(cell + " fora do labirinto " + width + "x" + height);
        }
    }
}
