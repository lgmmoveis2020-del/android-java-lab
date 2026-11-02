package com.lgm.androidlab;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lgm.androidlab.catalog.LabAdapter;
import com.lgm.androidlab.catalog.LabCatalog;
import com.lgm.androidlab.catalog.LabItem;
import com.lgm.androidlab.common.InsetsUtil;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.databinding.ActivityMainBinding;

/** Tela inicial: catálogo de laboratórios. */
public class MainActivity extends LabActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtil.applySystemBarsPadding(binding.getRoot());
        setupToolbar(binding.toolbar, false);

        int columns = getResources().getInteger(R.integer.catalog_columns);
        binding.labList.setLayoutManager(new GridLayoutManager(this, columns));
        binding.labList.setAdapter(new LabAdapter(LabCatalog.all(), this::openLab));
        binding.labList.setHasFixedSize(true);
    }

    private void openLab(@NonNull LabItem item) {
        startActivity(new Intent(this, item.activity()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_message, BuildConfig.VERSION_NAME, LabCatalog.all().size()))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
