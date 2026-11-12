package com.lgm.androidlab.core.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Resolve labirintos com busca em largura (BFS), que garante o caminho mais curto.
 */
public final class MazeSolver {

    /** Resultado da busca: caminho (vazio se inalcançável) e a ordem em que as células foram visitadas. */
    public record Solution(List<Cell> path, List<Cell> visitedOrder) {
        public Solution {
            path = List.copyOf(path);
            visitedOrder = List.copyOf(visitedOrder);
        }

        public boolean found() {
            return !path.isEmpty();
        }

        public int length() {
            return path.isEmpty() ? 0 : path.size() - 1;
        }
    }

    private MazeSolver() {
    }

    public static Solution solve(Maze maze, Cell start, Cell goal) {
        Objects.requireNonNull(maze, "maze");
        if (!maze.contains(start) || !maze.contains(goal)) {
            throw new IllegalArgumentException("start e goal precisam estar dentro do labirinto");
        }
        int w = maze.width();
        int[] previous = new int[maze.cellCount()];
        java.util.Arrays.fill(previous, -1);
        boolean[] seen = new boolean[maze.cellCount()];
        List<Cell> visitedOrder = new ArrayList<>();
        Deque<Cell> queue = new ArrayDeque<>();

        seen[start.y() * w + start.x()] = true;
        queue.add(start);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();
            visitedOrder.add(current);
            if (current.equals(goal)) {
                return new Solution(reconstruct(previous, w, start, goal), visitedOrder);
            }
            for (Cell next : maze.openNeighbors(current)) {
                int idx = next.y() * w + next.x();
                if (!seen[idx]) {
                    seen[idx] = true;
                    previous[idx] = current.y() * w + current.x();
                    queue.add(next);
                }
            }
        }
        return new Solution(List.of(), visitedOrder);
    }

    private static List<Cell> reconstruct(int[] previous, int w, Cell start, Cell goal) {
        List<Cell> path = new ArrayList<>();
        Cell current = goal;
        while (true) {
            path.add(current);
            if (current.equals(start)) {
                break;
            }
            int prev = previous[current.y() * w + current.x()];
            current = new Cell(prev % w, prev / w);
        }
        Collections.reverse(path);
        return path;
    }
}
