package com.lgm.androidlab.core.notes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação em memória, útil em testes e como base para implementações persistentes:
 * subclasses só precisam sobrescrever {@link #persist()} e carregar no construtor.
 */
public class InMemoryNoteRepository implements NoteRepository {

    private final Map<String, Note> notes = new LinkedHashMap<>();

    public InMemoryNoteRepository() {
    }

    public InMemoryNoteRepository(List<Note> initial) {
        for (Note n : initial) {
            notes.put(n.id(), n);
        }
    }

    @Override
    public final List<Note> all() {
        List<Note> list = new ArrayList<>(notes.values());
        list.sort(Comparator.comparingLong(Note::updatedAt).reversed());
        return list;
    }

    @Override
    public final Optional<Note> find(String id) {
        return Optional.ofNullable(notes.get(id));
    }

    @Override
    public final void save(Note note) {
        notes.put(note.id(), note);
        persist();
    }

    @Override
    public final boolean delete(String id) {
        boolean removed = notes.remove(id) != null;
        if (removed) {
            persist();
        }
        return removed;
    }

    @Override
    public final void clear() {
        notes.clear();
        persist();
    }

    /** Gancho chamado após cada mutação; a versão em memória não faz nada. */
    protected void persist() {
    }
}
