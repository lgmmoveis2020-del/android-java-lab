package com.lgm.androidlab.labs.particles;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.databinding.ActivityParticlesBinding;

import java.util.Locale;

/** Lab 1 — partículas: toque para explodir, incline o aparelho para mudar a gravidade. */
public class ParticleLabActivity extends LabActivity implements SensorEventListener {

    private ActivityParticlesBinding binding;
    @Nullable
    private SensorManager sensorManager;
    @Nullable
    private Sensor accelerometer;
    private boolean useSensorGravity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityParticlesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);

        sensorManager = getSystemService(SensorManager.class);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        binding.switchSensorGravity.setEnabled(accelerometer != null);

        binding.particleView.setStatsListener((count, fps, energy) -> runOnUiThread(() ->
                binding.statsText.setText(getString(R.string.particles_stats,
                        count, String.format(Locale.getDefault(), "%.0f", fps),
                        String.format(Locale.getDefault(), "%.1e", energy)))));

        binding.switchSensorGravity.setOnCheckedChangeListener((button, checked) -> {
            useSensorGravity = checked;
            if (checked) {
                registerSensor();
            } else {
                unregisterSensor();
                binding.particleView.setGravityDirection(0, 1);
            }
        });
        binding.switchCollisions.setChecked(true);
        binding.switchCollisions.setOnCheckedChangeListener(
                (button, checked) -> binding.particleView.setCollisionsEnabled(checked));
        binding.switchBouncy.setOnCheckedChangeListener(
                (button, checked) -> binding.particleView.setBouncy(checked));

        binding.buttonBurst.setOnClickListener(v -> binding.particleView.burstAtCenter());
        binding.buttonClear.setOnClickListener(v -> binding.particleView.clear());
        binding.buttonPlayPause.setOnClickListener(v -> {
            boolean run = !binding.particleView.isRunning();
            binding.particleView.setRunning(run);
            binding.buttonPlayPause.setIconResource(run ? R.drawable.ic_pause : R.drawable.ic_play);
            binding.buttonPlayPause.setContentDescription(
                    getString(run ? R.string.action_pause : R.string.action_play));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.particleView.setRunning(true);
        if (useSensorGravity) {
            registerSensor();
        }
    }

    @Override
    protected void onPause() {
        unregisterSensor();
        binding.particleView.setRunning(false);
        super.onPause();
    }

    private void registerSensor() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void unregisterSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!useSensorGravity || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        // Converte o vetor do sensor (eixos do aparelho) para os eixos da tela, respeitando a rotação.
        float ax = event.values[0];
        float ay = event.values[1];
        int rotation = getDisplayRotation();
        float sx;
        float sy;
        switch (rotation) {
            case Surface.ROTATION_90:
                sx = ay;
                sy = -ax;
                break;
            case Surface.ROTATION_180:
                sx = ax;
                sy = -ay;
                break;
            case Surface.ROTATION_270:
                sx = -ay;
                sy = ax;
                break;
            default:
                sx = -ax;
                sy = ay;
                break;
        }
        binding.particleView.setGravityDirection(sx / SensorManager.GRAVITY_EARTH, sy / SensorManager.GRAVITY_EARTH);
    }

    @SuppressWarnings("deprecation")
    private int getDisplayRotation() {
        Display display = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? getDisplay()
                : getWindowManager().getDefaultDisplay();
        return display == null ? Surface.ROTATION_0 : display.getRotation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // sem uso
    }
}
