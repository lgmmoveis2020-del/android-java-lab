package com.lgm.androidlab.labs.life;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.common.Labels;
import com.lgm.androidlab.core.life.Pattern;
import com.lgm.androidlab.databinding.ActivityLifeBinding;

/** Lab 2 — Jogo da Vida de Conway numa grade toroidal. */
public class LifeLabActivity extends LabActivity {

    private ActivityLifeBinding binding;
    private final Pattern[] patterns = Pattern.values();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLifeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);

        binding.lifeView.setListener((generation, population, running) -> {
            binding.statusText.setText(getString(R.string.life_status, generation, population));
            binding.buttonPlayPause.setIconResource(running ? R.drawable.ic_pause : R.drawable.ic_play);
            binding.buttonPlayPause.setContentDescription(
                    getString(running ? R.string.action_pause : R.string.action_play));
        });

        String[] names = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            names[i] = Labels.prettify(patterns[i].name());
        }
        binding.patternDropdown.setSimpleItems(names);
        binding.patternDropdown.setText(names[indexOf(Pattern.GOSPER_GUN)], false);
        binding.patternDropdown.setOnItemClickListener((parent, view, position, id) -> {
            binding.lifeView.setRunning(false);
            binding.lifeView.placePattern(patterns[position]);
        });

        binding.speedSlider.setLabelFormatter(value -> getString(R.string.life_speed_label, (int) value));
        binding.speedSlider.addOnChangeListener((slider, value, fromUser) ->
                binding.lifeView.setIntervalMs(Math.round(1000f / value)));
        binding.lifeView.setIntervalMs(Math.round(1000f / binding.speedSlider.getValue()));

        binding.buttonPlayPause.setOnClickListener(v -> binding.lifeView.setRunning(!binding.lifeView.isRunning()));
        binding.buttonStep.setOnClickListener(v -> {
            binding.lifeView.setRunning(false);
            binding.lifeView.step();
        });
        binding.buttonClear.setOnClickListener(v -> {
            binding.lifeView.setRunning(false);
            binding.lifeView.clear();
        });
        binding.buttonRandom.setOnClickListener(v -> binding.lifeView.randomize(0.3));
    }

    private int indexOf(@NonNull Pattern pattern) {
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] == pattern) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onPause() {
        binding.lifeView.setRunning(false);
        super.onPause();
    }
}
