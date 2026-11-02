package com.lgm.androidlab.core.life;

/**
 * Padrões clássicos do Game of Life, descritos em ASCII ('#' vivo, '.' morto).
 */
public enum Pattern {

    BLOCK("""
            ##
            ##"""),

    BLINKER("""
            ###"""),

    GLIDER("""
            .#.
            ..#
            ###"""),

    LWSS("""
            .#..#
            #....
            #...#
            ####."""),

    R_PENTOMINO("""
            .##
            ##.
            .#."""),

    PULSAR("""
            ..###...###..
            .............
            #....#.#....#
            #....#.#....#
            #....#.#....#
            ..###...###..
            .............
            ..###...###..
            #....#.#....#
            #....#.#....#
            #....#.#....#
            .............
            ..###...###.."""),

    GOSPER_GUN("""
            ........................#...........
            ......................#.#...........
            ............##......##............##
            ...........#...#....##............##
            ##........#.....#...##..............
            ##........#...#.##....#.#...........
            ..........#.....#.......#...........
            ...........#...#....................
            ............##......................""");

    private final String[] rows;

    Pattern(String ascii) {
        this.rows = ascii.split("\n");
    }

    public int width() {
        int max = 0;
        for (String row : rows) {
            max = Math.max(max, row.length());
        }
        return max;
    }

    public int height() {
        return rows.length;
    }

    public boolean alive(int x, int y) {
        if (y < 0 || y >= rows.length || x < 0 || x >= rows[y].length()) {
            return false;
        }
        char c = rows[y].charAt(x);
        return c == '#' || c == 'O' || c == 'o';
    }
}
