package com.lgm.androidlab.core.life;

import java.util.Objects;
import java.util.Random;

/**
 * Grade toroidal (as bordas se conectam) do Game of Life de Conway.
 *
 * <p>Regras B3/S23: uma célula morta com exatamente 3 vizinhas vivas nasce; uma célula viva
 * com 2 ou 3 vizinhas sobrevive; qualquer outra morre.</p>
 */
public final class LifeGrid {

    private final int width;
    private final int height;
    private boolean[] cells;
    private boolean[] scratch;
    private long generation;

    public LifeGrid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("largura e altura devem ser > 0");
        }
        this.width = width;
        this.height = height;
        this.cells = new boolean[width * height];
        this.scratch = new boolean[width * height];
    }

    private LifeGrid(LifeGrid other) {
        this.width = other.width;
        this.height = other.height;
        this.cells = other.cells.clone();
        this.scratch = new boolean[width * height];
        this.generation = other.generation;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long generation() {
        return generation;
    }

    /** Lê uma célula; coordenadas fora da grade "dão a volta" (topologia de toro). */
    public boolean get(int x, int y) {
        return cells[index(Math.floorMod(x, width), Math.floorMod(y, height))];
    }

    public void set(int x, int y, boolean alive) {
        checkBounds(x, y);
        cells[index(x, y)] = alive;
    }

    public void toggle(int x, int y) {
        checkBounds(x, y);
        cells[index(x, y)] = !cells[index(x, y)];
    }

    public int population() {
        int count = 0;
        for (boolean alive : cells) {
            if (alive) {
                count++;
            }
        }
        return count;
    }

    public void clear() {
        java.util.Arrays.fill(cells, false);
        generation = 0;
    }

    /** Preenche aleatoriamente; {@code density} é a probabilidade de cada célula estar viva. */
    public void randomize(Random random, double density) {
        Objects.requireNonNull(random, "random");
        if (density < 0 || density > 1) {
            throw new IllegalArgumentException("density deve estar em [0, 1]");
        }
        for (int i = 0; i < cells.length; i++) {
            cells[i] = random.nextDouble() < density;
        }
        generation = 0;
    }

    /** Carimba um padrão com o canto superior esquerdo em (originX, originY), com wrap. */
    public void place(Pattern pattern, int originX, int originY) {
        Objects.requireNonNull(pattern, "pattern");
        for (int py = 0; py < pattern.height(); py++) {
            for (int px = 0; px < pattern.width(); px++) {
                if (pattern.alive(px, py)) {
                    int x = Math.floorMod(originX + px, width);
                    int y = Math.floorMod(originY + py, height);
                    cells[index(x, y)] = true;
                }
            }
        }
    }

    /** Centraliza um padrão na grade. */
    public void placeCentered(Pattern pattern) {
        place(pattern, (width - pattern.width()) / 2, (height - pattern.height()) / 2);
    }

    public int liveNeighbors(int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (get(x + dx, y + dy)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Avança uma geração.
     *
     * @return quantidade de células que mudaram de estado (0 significa que a grade está estável)
     */
    public int step() {
        int changed = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int n = liveNeighbors(x, y);
                boolean alive = cells[index(x, y)];
                boolean next = alive ? (n == 2 || n == 3) : n == 3;
                scratch[index(x, y)] = next;
                if (next != alive) {
                    changed++;
                }
            }
        }
        boolean[] tmp = cells;
        cells = scratch;
        scratch = tmp;
        generation++;
        return changed;
    }

    public LifeGrid copy() {
        return new LifeGrid(this);
    }

    /** Representação ASCII (linhas separadas por '\n'), útil para depuração e testes. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(width * height + height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(cells[index(x, y)] ? '#' : '.');
            }
            if (y < height - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /** Inverso de {@link #toString()}: '#' ou 'O' representam células vivas. */
    public static LifeGrid parse(String ascii) {
        String[] rows = ascii.strip().split("\n");
        int h = rows.length;
        int w = 0;
        for (String row : rows) {
            w = Math.max(w, row.strip().length());
        }
        LifeGrid grid = new LifeGrid(w, h);
        for (int y = 0; y < h; y++) {
            String row = rows[y].strip();
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                grid.cells[grid.index(x, y)] = c == '#' || c == 'O' || c == 'o';
            }
        }
        return grid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LifeGrid other)) {
            return false;
        }
        return width == other.width && height == other.height
                && java.util.Arrays.equals(cells, other.cells);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * width + height) + java.util.Arrays.hashCode(cells);
    }

    private int index(int x, int y) {
        return y * width + x;
    }

    private void checkBounds(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException("(" + x + ", " + y + ") fora da grade "
                    + width + "x" + height);
        }
    }
}
