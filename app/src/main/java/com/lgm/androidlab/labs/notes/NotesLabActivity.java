package com.lgm.androidlab.labs.notes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.core.notes.Note;
import com.lgm.androidlab.databinding.ActivityNotesBinding;
import com.lgm.androidlab.databinding.DialogNoteBinding;

/** Lab 7 — bloco de notas persistido em SharedPreferences via JSON do core. */
public class NotesLabActivity extends LabActivity {

    private ActivityNotesBinding binding;
    private NotesViewModel viewModel;
    private NoteAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);

        adapter = new NoteAdapter(this::showEditor);
        binding.noteList.setLayoutManager(new LinearLayoutManager(this));
        binding.noteList.setAdapter(adapter);
        new ItemTouchHelper(new SwipeToDelete()).attachToRecyclerView(binding.noteList);

        viewModel.notes().observe(this, notes -> {
            adapter.submitList(notes);
            binding.emptyText.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
            binding.toolbar.setSubtitle(notes.size() == 1
                    ? getString(R.string.notes_count_one)
                    : getString(R.string.notes_count_other, notes.size()));
        });

        binding.fabAdd.setOnClickListener(v -> showEditor(null));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_all) {
            confirmClearAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmClearAll() {
        if (adapter.getItemCount() == 0) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notes_clear_all)
                .setMessage(R.string.notes_clear_all_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> viewModel.clearAll())
                .show();
    }

    private void showEditor(@Nullable Note existing) {
        DialogNoteBinding dialog = DialogNoteBinding.inflate(getLayoutInflater());
        if (existing != null) {
            dialog.titleInput.setText(existing.title());
            dialog.bodyInput.setText(existing.body());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.notes_new : R.string.notes_edit)
                .setView(dialog.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String title = textOf(dialog.titleInput.getText());
                    String body = textOf(dialog.bodyInput.getText());
                    Note saved = viewModel.save(existing == null ? null : existing.id(), title, body);
                    if (saved == null) {
                        snack(binding.getRoot(), getString(R.string.notes_discarded_empty));
                    }
                })
                .show();
        dialog.titleInput.requestFocus();
    }

    @NonNull
    private static String textOf(@Nullable CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    private void deleteWithUndo(@NonNull Note note) {
        viewModel.delete(note.id());
        Snackbar.make(binding.getRoot(), getString(R.string.notes_deleted, note.displayTitle()), Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fabAdd)
                .setAction(R.string.action_undo, v -> viewModel.restore(note))
                .show();
    }

    /** Arrastar para o lado apaga, pintando um fundo vermelho atrás do item. */
    private final class SwipeToDelete extends ItemTouchHelper.SimpleCallback {
        private final Paint paint = new Paint();

        SwipeToDelete() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            paint.setColor(ContextCompat.getColor(NotesLabActivity.this, R.color.swipe_delete_bg));
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                deleteWithUndo(adapter.noteAt(position));
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            View item = viewHolder.itemView;
            if (dX > 0) {
                c.drawRect(item.getLeft(), item.getTop(), item.getLeft() + dX, item.getBottom(), paint);
            } else if (dX < 0) {
                c.drawRect(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom(), paint);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}
