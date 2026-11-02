package com.lgm.androidlab.core.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LifeGridTest {

    @Test
    @DisplayName("blinker oscila com período 2")
    void blinkerOscillates() {
        LifeGrid grid = LifeGrid.parse("""
                .....
                .....
                .###.
                .....
                .....""");
        LifeGrid original = grid.copy();

        grid.step();
        assertEquals("""
                .....
                ..#..
                ..#..
                ..#..
                .....""", grid.toString());

        grid.step();
        assertEquals(original, grid);
        assertEquals(2, grid.generation());
    }

    @Test
    @DisplayName("bloco é estável: step devolve 0 mudanças")
    void blockIsStillLife() {
        LifeGrid grid = new LifeGrid(6, 6);
        grid.place(Pattern.BLOCK, 2, 2);
        LifeGrid before = grid.copy();

        assertEquals(0, grid.step());
        assertEquals(before, grid);
        assertEquals(4, grid.population());
    }

    @Test
    @DisplayName("glider volta à mesma forma deslocado (1,1) após 4 gerações")
    void gliderTranslates() {
        LifeGrid grid = new LifeGrid(12, 12);
        grid.place(Pattern.GLIDER, 1, 1);
        LifeGrid expected = new LifeGrid(12, 12);
        expected.place(Pattern.GLIDER, 2, 2);

        for (int i = 0; i < 4; i++) {
            grid.step();
        }

        assertEquals(expected.toString(), grid.toString());
        assertEquals(5, grid.population());
    }

    @Test
    @DisplayName("grade toroidal: vizinhos atravessam as bordas")
    void toroidalNeighbors() {
        LifeGrid grid = new LifeGrid(5, 5);
        grid.set(0, 0, true);
        grid.set(4, 4, true);
        grid.set(4, 0, true);

        assertEquals(2, grid.liveNeighbors(0, 0));
        assertTrue(grid.get(-1, -1), "get(-1,-1) deve dar a volta para (4,4)");
        assertTrue(grid.get(5, 5), "get(5,5) deve dar a volta para (0,0)");
    }

    @Test
    @DisplayName("célula solitária morre; três em L geram nascimento")
    void birthAndDeathRules() {
        LifeGrid grid = LifeGrid.parse("""
                ....
                .#..
                .##.
                ....""");
        grid.step();
        assertTrue(grid.get(2, 1), "nasce pela regra B3");
        assertTrue(grid.get(1, 1) && grid.get(1, 2) && grid.get(2, 2), "sobrevivem (bloco)");
        assertEquals(4, grid.population());

        LifeGrid lonely = new LifeGrid(3, 3);
        lonely.set(1, 1, true);
        assertEquals(1, lonely.step());
        assertEquals(0, lonely.population());
    }

    @ParameterizedTest
    @EnumSource(Pattern.class)
    @DisplayName("todo padrão cabe em uma grade 40x40 e tem população > 0")
    void patternsHavePopulation(Pattern pattern) {
        LifeGrid grid = new LifeGrid(40, 40);
        grid.placeCentered(pattern);
        assertTrue(grid.population() > 0);
        assertTrue(pattern.width() <= 40 && pattern.height() <= 40);
    }

    @Test
    @DisplayName("pulsar tem período 3")
    void pulsarPeriodThree() {
        LifeGrid grid = new LifeGrid(20, 20);
        grid.placeCentered(Pattern.PULSAR);
        LifeGrid original = grid.copy();
        assertEquals(48, grid.population());

        grid.step();
        assertNotEquals(original, grid);
        grid.step();
        grid.step();
        assertEquals(original, grid);
    }

    @Test
    @DisplayName("canhão de Gosper produz um glider: população cresce após 30 gerações")
    void gosperGunEmitsGliders() {
        LifeGrid grid = new LifeGrid(60, 40);
        grid.place(Pattern.GOSPER_GUN, 2, 2);
        int initial = grid.population();
        assertEquals(36, initial);

        for (int i = 0; i < 30; i++) {
            grid.step();
        }
        assertTrue(grid.population() > initial, "o canhão deve ter emitido um glider");
    }

    @Test
    @DisplayName("randomize respeita aproximadamente a densidade e zera a geração")
    void randomizeDensity() {
        LifeGrid grid = new LifeGrid(100, 100);
        grid.step();
        grid.randomize(new Random(1), 0.3);

        assertEquals(0, grid.generation());
        double ratio = grid.population() / 10_000.0;
        assertTrue(ratio > 0.27 && ratio < 0.33, "densidade observada: " + ratio);
    }

    @Test
    @DisplayName("parse/toString são inversos e toggle alterna")
    void parseRoundTripAndToggle() {
        String ascii = """
                #..#
                .##.
                #..#""";
        LifeGrid grid = LifeGrid.parse(ascii);
        assertEquals(ascii, grid.toString());
        assertEquals(4, grid.width());
        assertEquals(3, grid.height());

        grid.toggle(0, 0);
        assertFalse(grid.get(0, 0));
        grid.toggle(0, 0);
        assertTrue(grid.get(0, 0));
    }

    @Test
    @DisplayName("validações")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new LifeGrid(0, 5));
        LifeGrid grid = new LifeGrid(3, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> grid.set(3, 0, true));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.toggle(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> grid.randomize(new Random(), 1.5));
        grid.clear();
        assertEquals(0, grid.population());
    }

    @Test
    @DisplayName("copy é independente da original")
    void copyIsIndependent() {
        LifeGrid grid = new LifeGrid(4, 4);
        grid.set(1, 1, true);
        LifeGrid copy = grid.copy();
        copy.set(2, 2, true);

        assertFalse(grid.get(2, 2));
        assertEquals(grid.hashCode(), grid.copy().hashCode());
    }
}
