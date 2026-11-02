package com.lgm.androidlab.core.notes;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência de notas. O app Android implementa com SharedPreferences;
 * os testes usam {@link InMemoryNoteRepository}.
 */
public interface NoteRepository {

    /** Todas as notas, da mais recentemente atualizada para a mais antiga. */
    List<Note> all();

    Optional<Note> find(String id);

    /** Insere ou substitui (pelo id). */
    void save(Note note);

    /** @return true se existia e foi removida */
    boolean delete(String id);

    void clear();

    default int count() {
        return all().size();
    }
}
