package com.lgm.androidlab.labs.sort;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.common.Labels;
import com.lgm.androidlab.core.sort.SortAlgorithm;
import com.lgm.androidlab.core.sort.SortRecorder;
import com.lgm.androidlab.core.sort.SortStep;
import com.lgm.androidlab.databinding.ActivitySortBinding;

import java.util.List;
import java.util.Random;

/** Lab 6 — visualizador de algoritmos de ordenação (passos gravados pelo core, reproduzidos aqui). */
public class SortLabActivity extends LabActivity {

    private static final String STATE_SEED = "seed";
    private static final String STATE_ALGO = "algo";

    private ActivitySortBinding binding;
    private final SortAlgorithm[] algorithms = SortAlgorithm.values();
    private int algorithmIndex;
    private long seed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySortBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);

        if (savedInstanceState != null) {
            seed = savedInstanceState.getLong(STATE_SEED);
            algorithmIndex = savedInstanceState.getInt(STATE_ALGO, 0);
        } else {
            seed = System.nanoTime();
            algorithmIndex = indexOf(SortAlgorithm.QUICK);
        }

        String[] names = new String[algorithms.length];
        for (int i = 0; i < algorithms.length; i++) {
            names[i] = algorithms[i].displayName();
        }
        binding.algorithmDropdown.setSimpleItems(names);
        binding.algorithmDropdown.setText(names[algorithmIndex], false);
        binding.algorithmDropdown.setOnItemClickListener((parent, view, position, id) -> {
            algorithmIndex = position;
            rebuild();
        });

        binding.sizeSlider.setLabelFormatter(value -> getString(R.string.sort_size_label, (int) value));
        binding.sizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                rebuild();
            }
        });
        binding.speedSlider.setLabelFormatter(value -> getString(R.string.sort_speed_label, (int) value));
        binding.speedSlider.addOnChangeListener((slider, value, fromUser) ->
                binding.sortView.setStepsPerFrame((int) value));
        binding.sortView.setStepsPerFrame((int) binding.speedSlider.getValue());

        binding.sortView.setListener((cursor, total, comparisons, writes, running) -> {
            binding.buttonPlayPause.setIconResource(running ? R.drawable.ic_pause : R.drawable.ic_play);
            binding.buttonPlayPause.setContentDescription(
                    getString(running ? R.string.action_pause : R.string.action_play));
            int percent = total == 0 ? 100 : (int) (100L * cursor / total);
            binding.statsText.setText(getString(R.string.sort_stats,
                    Labels.groupDigits(comparisons), Labels.groupDigits(writes), percent));
        });

        binding.buttonPlayPause.setOnClickListener(v -> {
            if (binding.sortView.isRunning()) {
                binding.sortView.pause();
            } else {
                binding.sortView.play();
            }
        });
        binding.buttonStep.setOnClickListener(v -> binding.sortView.stepOnce());
        binding.buttonShuffle.setOnClickListener(v -> {
            seed = System.nanoTime();
            rebuild();
        });
        binding.buttonReset.setOnClickListener(v -> binding.sortView.reset());

        rebuild();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SEED, seed);
        outState.putInt(STATE_ALGO, algorithmIndex);
    }

    @Override
    protected void onPause() {
        binding.sortView.pause();
        super.onPause();
    }

    private int indexOf(@NonNull SortAlgorithm algorithm) {
        for (int i = 0; i < algorithms.length; i++) {
            if (algorithms[i] == algorithm) {
                return i;
            }
        }
        return 0;
    }

    private void rebuild() {
        int size = (int) binding.sizeSlider.getValue();
        int[] data = new int[size];
        Random random = new Random(seed);
        for (int i = 0; i < size; i++) {
            data[i] = 1 + random.nextInt(1000);
        }
        SortAlgorithm algorithm = algorithms[algorithmIndex];
        List<SortStep> steps = SortRecorder.record(data, algorithm);
        binding.sortView.load(data, steps);
        binding.algorithmInfo.setText(getString(R.string.sort_algorithm_info,
                algorithm.complexity(),
                getString(algorithm.stable() ? R.string.sort_stable : R.string.sort_unstable),
                Labels.groupDigits(steps.size())));
    }
}
