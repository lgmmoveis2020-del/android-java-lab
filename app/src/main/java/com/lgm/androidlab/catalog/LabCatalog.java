package com.lgm.androidlab.catalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lgm.androidlab.R;
import com.lgm.androidlab.labs.calculator.CalculatorLabActivity;
import com.lgm.androidlab.labs.life.LifeLabActivity;
import com.lgm.androidlab.labs.maze.MazeLabActivity;
import com.lgm.androidlab.labs.notes.NotesLabActivity;
import com.lgm.androidlab.labs.particles.ParticleLabActivity;
import com.lgm.androidlab.labs.primes.PrimeLabActivity;
import com.lgm.androidlab.labs.sensors.SensorLabActivity;
import com.lgm.androidlab.labs.sort.SortLabActivity;

import java.util.List;

/** Fonte única da lista de laboratórios; a ordem aqui é a ordem na tela. */
public final class LabCatalog {

    private static final List<LabItem> ITEMS = List.of(
            new LabItem("particles", R.string.lab_particles_title, R.string.lab_particles_desc,
                    R.drawable.ic_lab_particles, R.color.accent_particles, ParticleLabActivity.class),
            new LabItem("life", R.string.lab_life_title, R.string.lab_life_desc,
                    R.drawable.ic_lab_life, R.color.accent_life, LifeLabActivity.class),
            new LabItem("calculator", R.string.lab_calculator_title, R.string.lab_calculator_desc,
                    R.drawable.ic_lab_calculator, R.color.accent_calculator, CalculatorLabActivity.class),
            new LabItem("primes", R.string.lab_primes_title, R.string.lab_primes_desc,
                    R.drawable.ic_lab_primes, R.color.accent_primes, PrimeLabActivity.class),
            new LabItem("maze", R.string.lab_maze_title, R.string.lab_maze_desc,
                    R.drawable.ic_lab_maze, R.color.accent_maze, MazeLabActivity.class),
            new LabItem("sort", R.string.lab_sort_title, R.string.lab_sort_desc,
                    R.drawable.ic_lab_sort, R.color.accent_sort, SortLabActivity.class),
            new LabItem("notes", R.string.lab_notes_title, R.string.lab_notes_desc,
                    R.drawable.ic_lab_notes, R.color.accent_notes, NotesLabActivity.class),
            new LabItem("sensors", R.string.lab_sensors_title, R.string.lab_sensors_desc,
                    R.drawable.ic_lab_sensors, R.color.accent_sensors, SensorLabActivity.class));

    private LabCatalog() {
    }

    @NonNull
    public static List<LabItem> all() {
        return ITEMS;
    }

    @Nullable
    public static LabItem byId(@NonNull String id) {
        for (LabItem item : ITEMS) {
            if (item.id().equals(id)) {
                return item;
            }
        }
        return null;
    }
}
