package com.lgm.androidlab.common;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

/** Base para todas as telas: edge-to-edge, toolbar com botão "voltar" e helpers de UI. */
public abstract class LabActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    /** Instala a toolbar como action bar; {@code showUp} habilita a seta de voltar. */
    protected void setupToolbar(@NonNull Toolbar toolbar, boolean showUp) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(showUp);
        }
    }

    /** Toolbar + insets da raiz num só passo (o caso comum de todos os labs). */
    protected void setupScreen(@NonNull View root, @NonNull Toolbar toolbar) {
        InsetsUtil.applySystemBarsPadding(root);
        setupToolbar(toolbar, true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void snack(@NonNull View anchor, @NonNull CharSequence message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show();
    }
}
