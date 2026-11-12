package com.lgm.androidlab.labs.primes;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.common.Labels;
import com.lgm.androidlab.core.primes.PrimeSieve;
import com.lgm.androidlab.databinding.ActivityPrimesBinding;

import java.util.Locale;

/** Lab 4 — crivo segmentado em background + teste de primalidade Miller-Rabin instantâneo. */
public class PrimeLabActivity extends LabActivity {

    private static final long MAX_LIMIT = 2_000_000_000L;
    private static final long[] PRESETS = {1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L};

    private ActivityPrimesBinding binding;
    private PrimeViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrimesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);
        viewModel = new ViewModelProvider(this).get(PrimeViewModel.class);

        binding.chipPreset1.setOnClickListener(v -> setLimit(PRESETS[0]));
        binding.chipPreset2.setOnClickListener(v -> setLimit(PRESETS[1]));
        binding.chipPreset3.setOnClickListener(v -> setLimit(PRESETS[2]));
        binding.chipPreset4.setOnClickListener(v -> setLimit(PRESETS[3]));

        binding.buttonStart.setOnClickListener(v -> onStartOrCancel());
        binding.limitInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onStartOrCancel();
                return true;
            }
            return false;
        });

        binding.buttonCheck.setOnClickListener(v -> checkPrimality());
        binding.checkInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkPrimality();
                return true;
            }
            return false;
        });

        viewModel.state().observe(this, this::render);
    }

    private void setLimit(long limit) {
        binding.limitInput.setText(String.valueOf(limit));
        binding.limitInput.setSelection(binding.limitInput.length());
    }

    private void onStartOrCancel() {
        PrimeViewModel.State state = viewModel.state().getValue();
        if (state != null && state.isRunning()) {
            viewModel.cancel();
            return;
        }
        Long limit = parseLong(binding.limitInput.getText() == null ? "" : binding.limitInput.getText().toString());
        if (limit == null || limit < 0) {
            binding.limitLayout.setError(getString(R.string.primes_error_invalid));
            return;
        }
        if (limit > MAX_LIMIT) {
            binding.limitLayout.setError(getString(R.string.primes_error_too_big, Labels.groupDigits(MAX_LIMIT)));
            return;
        }
        binding.limitLayout.setError(null);
        viewModel.start(limit);
    }

    private void checkPrimality() {
        String raw = binding.checkInput.getText() == null ? "" : binding.checkInput.getText().toString();
        Long n = parseLong(raw);
        if (n == null) {
            binding.checkLayout.setError(getString(R.string.primes_error_invalid));
            binding.checkResult.setText("");
            return;
        }
        binding.checkLayout.setError(null);
        long start = System.nanoTime();
        boolean prime = PrimeSieve.isPrime(n);
        double micros = (System.nanoTime() - start) / 1000.0;
        binding.checkResult.setText(getString(
                prime ? R.string.primes_check_prime : R.string.primes_check_composite,
                Labels.groupDigits(n), String.format(Locale.getDefault(), "%.1f", micros)));
    }

    @Nullable
    private static Long parseLong(@NonNull String raw) {
        String digits = raw.replaceAll("[\\s._,]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void render(@NonNull PrimeViewModel.State state) {
        boolean running = state.isRunning();
        binding.buttonStart.setText(running ? R.string.action_cancel : R.string.primes_start);
        binding.limitLayout.setEnabled(!running);
        binding.progress.setVisibility(state.status() == PrimeViewModel.Status.IDLE ? View.INVISIBLE : View.VISIBLE);
        binding.progress.setProgressCompat((int) Math.round(state.progress() * 1000), true);

        switch (state.status()) {
            case IDLE:
                binding.resultCard.setVisibility(View.GONE);
                binding.progressText.setText(R.string.primes_idle_hint);
                break;
            case RUNNING:
                binding.resultCard.setVisibility(View.GONE);
                binding.progressText.setText(getString(R.string.primes_running,
                        (int) Math.round(state.progress() * 100), Labels.groupDigits(state.primesSoFar())));
                break;
            case DONE:
            case CANCELLED:
                PrimeSieve.Result result = state.result();
                if (result != null) {
                    binding.resultCard.setVisibility(View.VISIBLE);
                    binding.resultCount.setText(Labels.groupDigits(result.count()));
                    binding.resultLargest.setText(result.largest() < 0
                            ? getString(R.string.value_none) : Labels.groupDigits(result.largest()));
                    binding.resultTime.setText(Labels.duration(result.elapsedMillis()));
                    binding.resultLimit.setText(getString(
                            result.cancelled() ? R.string.primes_result_cancelled : R.string.primes_result_limit,
                            Labels.groupDigits(result.limit())));
                }
                binding.progressText.setText(result != null && result.cancelled()
                        ? R.string.primes_cancelled : R.string.primes_done);
                break;
        }
    }
}
