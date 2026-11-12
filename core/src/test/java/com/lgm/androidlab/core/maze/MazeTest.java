package com.lgm.androidlab.core.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MazeTest {

    @ParameterizedTest(name = "{0}x{1} semente {2}")
    @CsvSource({"1, 1, 0", "2, 2, 1", "5, 3, 2", "10, 10, 3", "25, 40, 4", "60, 60, 5"})
    @DisplayName("labirinto gerado é perfeito: conexo e com exatamente n-1 passagens")
    void generatedMazeIsPerfect(int w, int h, long seed) {
        Maze maze = MazeGenerator.generate(w, h, new Random(seed));

        assertEquals(w * h - 1, maze.openPassageCount(), "árvore geradora tem n-1 arestas");
        MazeSolver.Solution solution = MazeSolver.solve(maze, new Cell(0, 0), new Cell(w - 1, h - 1));
        assertTrue(solution.found());
        assertEquals(w * h, countReachable(maze), "todas as células devem ser alcançáveis");
    }

    private static int countReachable(Maze maze) {
        // alvo impossível de alcançar força o BFS a esgotar o componente conexo
        Maze bigger = new Maze(maze.width() + 1, maze.height());
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Cell c = new Cell(x, y);
                for (Direction d : Direction.values()) {
                    if (maze.isOpen(c, d) && bigger.contains(c.neighbor(d))) {
                        bigger.open(c, d);
                    }
                }
            }
        }
        MazeSolver.Solution s = MazeSolver.solve(bigger, new Cell(0, 0), new Cell(maze.width(), 0));
        assertFalse(s.found());
        return s.visitedOrder().size();
    }

    @Test
    @DisplayName("caminho é contíguo, começa no início, termina no fim e não repete células")
    void pathIsContiguous() {
        Maze maze = MazeGenerator.generate(15, 9, new Random(99));
        Cell start = new Cell(0, 0);
        Cell goal = new Cell(14, 8);
        MazeSolver.Solution solution = MazeSolver.solve(maze, start, goal);
        List<Cell> path = solution.path();

        assertEquals(start, path.get(0));
        assertEquals(goal, path.get(path.size() - 1));
        assertEquals(path.size() - 1, solution.length());
        assertEquals(path.size(), path.stream().distinct().count());
        for (int i = 1; i < path.size(); i++) {
            Cell a = path.get(i - 1);
            Cell b = path.get(i);
            assertEquals(1, a.manhattanDistance(b));
            assertTrue(maze.openNeighbors(a).contains(b), a + " -> " + b + " deve ter passagem");
        }
    }

    @Test
    @DisplayName("mesma semente gera o mesmo labirinto")
    void deterministicForSeed() {
        Maze a = MazeGenerator.generate(20, 20, new Random(2024));
        Maze b = MazeGenerator.generate(20, 20, new Random(2024));
        assertEquals(a.toAscii(), b.toAscii());
    }

    @Test
    @DisplayName("sem passagens, só o próprio início é alcançável")
    void unreachableGoal() {
        Maze closed = new Maze(3, 3);
        MazeSolver.Solution s = MazeSolver.solve(closed, new Cell(0, 0), new Cell(2, 2));
        assertFalse(s.found());
        assertEquals(0, s.length());
        assertEquals(List.of(new Cell(0, 0)), s.visitedOrder());
    }

    @Test
    @DisplayName("início igual ao fim devolve caminho de uma célula")
    void startEqualsGoal() {
        Maze maze = MazeGenerator.generate(4, 4, new Random(1));
        MazeSolver.Solution s = MazeSolver.solve(maze, new Cell(2, 2), new Cell(2, 2));
        assertEquals(List.of(new Cell(2, 2)), s.path());
        assertEquals(0, s.length());
    }

    @Test
    @DisplayName("open abre dos dois lados e toAscii desenha a passagem")
    void openIsSymmetric() {
        Maze maze = new Maze(2, 1);
        maze.open(new Cell(0, 0), Direction.EAST);

        assertTrue(maze.isOpen(new Cell(0, 0), Direction.EAST));
        assertTrue(maze.isOpen(new Cell(1, 0), Direction.WEST));
        assertFalse(maze.isOpen(new Cell(0, 0), Direction.NORTH));
        assertEquals(1, maze.openPassageCount());
        assertEquals("""
                +---+---+
                |       |
                +---+---+
                """, maze.toAscii());
    }

    @Test
    @DisplayName("validações")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new Maze(0, 1));
        Maze maze = new Maze(2, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> maze.open(new Cell(1, 1), Direction.EAST));
        assertThrows(IndexOutOfBoundsException.class, () -> maze.isOpen(new Cell(5, 5), Direction.NORTH));
        assertThrows(IllegalArgumentException.class,
                () -> MazeSolver.solve(maze, new Cell(0, 0), new Cell(9, 9)));
        assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
        assertEquals(Direction.WEST, Direction.EAST.opposite());
        assertEquals(new Cell(1, 0), new Cell(0, 0).neighbor(Direction.EAST));
    }
}
