package com.lgm.androidlab.labs.notes;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lgm.androidlab.core.notes.Note;
import com.lgm.androidlab.databinding.ItemNoteBinding;

/** Lista de notas com DiffUtil. */
public final class NoteAdapter extends ListAdapter<Note, NoteAdapter.ViewHolder> {

    /** Clique numa nota. */
    public interface OnNoteClickListener {
        void onNoteClick(@NonNull Note note);
    }

    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.id().equals(newItem.id());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final OnNoteClickListener listener;

    public NoteAdapter(@NonNull OnNoteClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = getItem(position);
        holder.binding.noteTitle.setText(note.displayTitle());
        holder.binding.noteBody.setText(note.body());
        holder.binding.noteBody.setVisibility(note.body().isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.noteDate.setText(DateUtils.getRelativeTimeSpanString(
                note.updatedAt(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        holder.binding.getRoot().setOnClickListener(v -> listener.onNoteClick(note));
    }

    @NonNull
    public Note noteAt(int position) {
        return getItem(position);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ItemNoteBinding binding;

        ViewHolder(@NonNull ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
