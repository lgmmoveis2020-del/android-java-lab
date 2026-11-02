package com.lgm.androidlab.labs.maze;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.common.Labels;
import com.lgm.androidlab.core.maze.Cell;
import com.lgm.androidlab.core.maze.Maze;
import com.lgm.androidlab.core.maze.MazeGenerator;
import com.lgm.androidlab.core.maze.MazeSolver;
import com.lgm.androidlab.databinding.ActivityMazeBinding;

import java.util.Random;

/** Lab 5 — labirintos perfeitos (DFS iterativo) resolvidos por BFS com animação. */
public class MazeLabActivity extends LabActivity {

    private static final String STATE_SEED = "seed";
    private static final String STATE_SIZE = "size";
    private static final String STATE_SOLVED = "solved";

    private ActivityMazeBinding binding;
    private long seed;
    private boolean solved;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMazeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);

        if (savedInstanceState != null) {
            seed = savedInstanceState.getLong(STATE_SEED);
            solved = savedInstanceState.getBoolean(STATE_SOLVED);
            binding.sizeSlider.setValue(savedInstanceState.getInt(STATE_SIZE, (int) binding.sizeSlider.getValue()));
        } else {
            seed = System.nanoTime();
        }

        binding.sizeSlider.setLabelFormatter(value -> getString(R.string.maze_size_label, (int) value, (int) value));
        binding.sizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                seed = System.nanoTime();
                solved = false;
                generate();
            }
        });
        binding.buttonGenerate.setOnClickListener(v -> {
            seed = System.nanoTime();
            solved = false;
            generate();
        });
        binding.buttonSolve.setOnClickListener(v -> solve(false));
        binding.mazeView.setListener(this::showSolutionStats);

        generate();
        if (solved) {
            solve(true);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SEED, seed);
        outState.putInt(STATE_SIZE, (int) binding.sizeSlider.getValue());
        outState.putBoolean(STATE_SOLVED, solved);
    }

    private void generate() {
        int size = (int) binding.sizeSlider.getValue();
        Maze maze = MazeGenerator.generate(size, size, new Random(seed));
        binding.mazeView.setMaze(maze);
        binding.buttonSolve.setEnabled(true);
        binding.statsText.setText(getString(R.string.maze_stats_generated,
                maze.width(), maze.height(), Labels.groupDigits(maze.cellCount())));
    }

    private void solve(boolean instant) {
        Maze maze = binding.mazeView.maze();
        if (maze == null) {
            return;
        }
        Cell start = new Cell(0, 0);
        Cell goal = new Cell(maze.width() - 1, maze.height() - 1);
        long t0 = System.nanoTime();
        MazeSolver.Solution solution = MazeSolver.solve(maze, start, goal);
        double micros = (System.nanoTime() - t0) / 1000.0;
        solved = true;
        binding.buttonSolve.setEnabled(false);
        binding.statsText.setText(getString(R.string.maze_stats_solving, Labels.duration(micros / 1000.0)));
        binding.mazeView.showSolution(solution, instant);
    }

    private void showSolutionStats(@NonNull MazeSolver.Solution solution) {
        Maze maze = binding.mazeView.maze();
        int total = maze == null ? 0 : maze.cellCount();
        int visited = solution.visitedOrder().size();
        int percent = total == 0 ? 0 : Math.round(100f * visited / total);
        binding.statsText.setText(getString(R.string.maze_stats_solved,
                solution.length(), Labels.groupDigits(visited), percent));
    }
}
