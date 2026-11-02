package com.lgm.androidlab.labs.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.databinding.ActivitySensorsBinding;
import com.lgm.androidlab.databinding.ItemSensorBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lab 8 — leitura ao vivo dos sensores do aparelho + nível de bolha. */
public class SensorLabActivity extends LabActivity implements SensorEventListener {

    private static final long UI_THROTTLE_NANOS = 80_000_000L;

    /** Sensores que tentamos mostrar, na ordem; os ausentes são omitidos. */
    private static final int[] SENSOR_TYPES = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
    };

    private static final class Row {
        final Sensor sensor;
        final ItemSensorBinding binding;
        long lastUiNanos;

        Row(Sensor sensor, ItemSensorBinding binding) {
            this.sensor = sensor;
            this.binding = binding;
        }
    }

    private ActivitySensorsBinding binding;
    @Nullable
    private SensorManager sensorManager;
    private final List<Row> rows = new ArrayList<>();
    private int eventsPerSecond;
    private int eventCounter;
    private long counterWindowStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySensorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);

        sensorManager = getSystemService(SensorManager.class);
        if (sensorManager == null) {
            binding.noSensorsText.setVisibility(View.VISIBLE);
            return;
        }
        for (int type : SENSOR_TYPES) {
            Sensor sensor = sensorManager.getDefaultSensor(type);
            if (sensor == null) {
                continue;
            }
            ItemSensorBinding item = ItemSensorBinding.inflate(getLayoutInflater(), binding.sensorList, false);
            item.sensorName.setText(sensor.getName());
            item.sensorMeta.setText(getString(R.string.sensors_meta, sensor.getVendor(),
                    describeType(type), formatValue(sensor.getMaximumRange())));
            item.sensorValues.setText(R.string.sensors_waiting);
            binding.sensorList.addView(item.getRoot());
            rows.add(new Row(sensor, item));
        }
        binding.noSensorsText.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        binding.summaryText.setText(getString(R.string.sensors_summary,
                rows.size(), sensorManager.getSensorList(Sensor.TYPE_ALL).size()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            for (Row row : rows) {
                sensorManager.registerListener(this, row.sensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
        counterWindowStart = System.nanoTime();
        eventCounter = 0;
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.nanoTime();
        eventCounter++;
        if (now - counterWindowStart >= 1_000_000_000L) {
            eventsPerSecond = eventCounter;
            eventCounter = 0;
            counterWindowStart = now;
            binding.rateText.setText(getString(R.string.sensors_rate, eventsPerSecond));
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && event.values.length >= 2) {
            float g = SensorManager.GRAVITY_EARTH;
            binding.levelView.setGravity(-event.values[0] / g, event.values[1] / g);
            binding.tiltText.setText(getString(R.string.sensors_tilt,
                    String.format(Locale.getDefault(), "%.1f", binding.levelView.tiltDegrees())));
        }

        for (Row row : rows) {
            if (row.sensor.getType() != event.sensor.getType()) {
                continue;
            }
            if (now - row.lastUiNanos < UI_THROTTLE_NANOS) {
                return;
            }
            row.lastUiNanos = now;
            row.binding.sensorValues.setText(formatValues(event.values));
            return;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // sem uso
    }

    @NonNull
    private static String formatValues(@NonNull float[] values) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(values.length, 4);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append("   ");
            }
            sb.append(formatValue(values[i]));
        }
        return sb.toString();
    }

    @NonNull
    private static String formatValue(float v) {
        float abs = Math.abs(v);
        if (abs >= 1000 || abs == 0f) {
            return String.format(Locale.getDefault(), "%.0f", v);
        }
        if (abs >= 10) {
            return String.format(Locale.getDefault(), "%.2f", v);
        }
        return String.format(Locale.getDefault(), "%.3f", v);
    }

    @NonNull
    private String describeType(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return getString(R.string.sensors_unit_acceleration);
            case Sensor.TYPE_GYROSCOPE:
                return getString(R.string.sensors_unit_gyro);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return getString(R.string.sensors_unit_magnetic);
            case Sensor.TYPE_LIGHT:
                return getString(R.string.sensors_unit_light);
            case Sensor.TYPE_PROXIMITY:
                return getString(R.string.sensors_unit_distance);
            case Sensor.TYPE_PRESSURE:
                return getString(R.string.sensors_unit_pressure);
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return getString(R.string.sensors_unit_temperature);
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return getString(R.string.sensors_unit_humidity);
            default:
                return getString(R.string.sensors_unit_raw);
        }
    }
}
