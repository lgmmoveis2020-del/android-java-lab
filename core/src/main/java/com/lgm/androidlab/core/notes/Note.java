package com.lgm.androidlab.core.notes;

import java.util.Objects;
import java.util.UUID;

/**
 * Uma nota de texto. Imutável: edições produzem novas instâncias via {@link #edited}.
 */
public record Note(String id, String title, String body, long createdAt, long updatedAt) {

    public Note {
        Objects.requireNonNull(id, "id");
        title = title == null ? "" : title.strip();
        body = body == null ? "" : body;
        if (id.isBlank()) {
            throw new IllegalArgumentException("id não pode ser vazio");
        }
    }

    public static Note create(String title, String body, long now) {
        return new Note(UUID.randomUUID().toString(), title, body, now, now);
    }

    public Note edited(String newTitle, String newBody, long now) {
        return new Note(id, newTitle, newBody, createdAt, now);
    }

    public boolean isEmpty() {
        return title.isBlank() && body.isBlank();
    }

    /** Título para exibição: cai para a primeira linha do corpo quando não há título. */
    public String displayTitle() {
        if (!title.isBlank()) {
            return title;
        }
        String firstLine = body.strip().split("\n", 2)[0];
        return firstLine.isBlank() ? "(sem título)" : firstLine;
    }
}
