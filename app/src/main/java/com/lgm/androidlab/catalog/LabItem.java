package com.lgm.androidlab.catalog;

import android.app.Activity;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/** Um laboratório exibido no catálogo da tela inicial. */
public record LabItem(
        @NonNull String id,
        @StringRes int titleRes,
        @StringRes int descriptionRes,
        @DrawableRes int iconRes,
        @ColorRes int accentRes,
        @NonNull Class<? extends Activity> activity) {

    public LabItem {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id vazio");
        }
    }
}
