package com.lgm.androidlab;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

/** Ponto de entrada do processo: tema segue o sistema e cores dinâmicas (Material You) quando disponíveis. */
public class LabApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
