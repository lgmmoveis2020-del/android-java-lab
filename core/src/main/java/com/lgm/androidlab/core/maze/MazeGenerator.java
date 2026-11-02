package com.lgm.androidlab.core.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Gera labirintos "perfeitos" (exatamente um caminho entre quaisquer duas células)
 * usando busca em profundidade com retrocesso (recursive backtracker), em versão iterativa
 * para não estourar a pilha em grades grandes.
 */
public final class MazeGenerator {

    private MazeGenerator() {
    }

    public static Maze generate(int width, int height, Random random) {
        Objects.requireNonNull(random, "random");
        Maze maze = new Maze(width, height);
        boolean[] visited = new boolean[width * height];
        Deque<Cell> stack = new ArrayDeque<>();

        Cell start = new Cell(random.nextInt(width), random.nextInt(height));
        visited[start.y() * width + start.x()] = true;
        stack.push(start);

        List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
        while (!stack.isEmpty()) {
            Cell current = stack.peek();
            Collections.shuffle(dirs, random);
            Cell nextCell = null;
            Direction chosen = null;
            for (Direction dir : dirs) {
                Cell candidate = current.neighbor(dir);
                if (maze.contains(candidate) && !visited[candidate.y() * width + candidate.x()]) {
                    nextCell = candidate;
                    chosen = dir;
                    break;
                }
            }
            if (nextCell == null) {
                stack.pop();
            } else {
                maze.open(current, chosen);
                visited[nextCell.y() * width + nextCell.x()] = true;
                stack.push(nextCell);
            }
        }
        return maze;
    }
}
