package com.lgm.androidlab.labs.notes;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lgm.androidlab.core.notes.Note;
import com.lgm.androidlab.core.notes.NoteRepository;

import java.util.List;

/** Expõe o repositório de notas como LiveData. */
public class NotesViewModel extends AndroidViewModel {

    private final NoteRepository repository;
    private final MutableLiveData<List<Note>> notes = new MutableLiveData<>();

    public NotesViewModel(@NonNull Application application) {
        super(application);
        repository = new PrefsNoteRepository(
                application.getSharedPreferences(PrefsNoteRepository.PREFS_NAME, Context.MODE_PRIVATE));
        notes.setValue(repository.all());
    }

    @NonNull
    public LiveData<List<Note>> notes() {
        return notes;
    }

    @Nullable
    public Note find(@NonNull String id) {
        return repository.find(id).orElse(null);
    }

    /** Cria (id nulo) ou edita uma nota; devolve null se ficou vazia (e então a apaga). */
    @Nullable
    public Note save(@Nullable String id, @NonNull String title, @NonNull String body) {
        long now = System.currentTimeMillis();
        Note existing = id == null ? null : find(id);
        Note note = existing == null ? Note.create(title, body, now) : existing.edited(title, body, now);
        if (note.isEmpty()) {
            if (existing != null) {
                repository.delete(existing.id());
                refresh();
            }
            return null;
        }
        repository.save(note);
        refresh();
        return note;
    }

    public boolean delete(@NonNull String id) {
        boolean removed = repository.delete(id);
        if (removed) {
            refresh();
        }
        return removed;
    }

    /** Desfaz uma exclusão recolocando a nota original (mesmo id e datas). */
    public void restore(@NonNull Note note) {
        repository.save(note);
        refresh();
    }

    public void clearAll() {
        repository.clear();
        refresh();
    }

    private void refresh() {
        notes.setValue(repository.all());
    }
}
