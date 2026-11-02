package com.lgm.androidlab.labs.notes;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.lgm.androidlab.core.notes.InMemoryNoteRepository;
import com.lgm.androidlab.core.notes.NoteCodec;

/** Persiste as notas como JSON (codec do core) num {@link SharedPreferences}. */
public final class PrefsNoteRepository extends InMemoryNoteRepository {

    static final String PREFS_NAME = "notes";
    static final String KEY_NOTES = "notes_json";

    private final SharedPreferences prefs;

    public PrefsNoteRepository(@NonNull SharedPreferences prefs) {
        super(NoteCodec.decode(prefs.getString(KEY_NOTES, "")));
        this.prefs = prefs;
    }

    @Override
    protected void persist() {
        prefs.edit().putString(KEY_NOTES, NoteCodec.encode(all())).apply();
    }
}
