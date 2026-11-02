package com.lgm.androidlab.core.notes;

import com.lgm.androidlab.core.json.JsonException;
import com.lgm.androidlab.core.json.MiniJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializa notas em JSON (um array de objetos) para guardar em SharedPreferences ou arquivo.
 *
 * <p>Formato: {@code {"version":1,"notes":[{"id":...,"title":...,"body":...,"createdAt":...,"updatedAt":...}]}}</p>
 */
public final class NoteCodec {

    public static final int VERSION = 1;

    private NoteCodec() {
    }

    public static String encode(List<Note> notes) {
        List<Object> items = new ArrayList<>(notes.size());
        for (Note n : notes) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("id", n.id());
            obj.put("title", n.title());
            obj.put("body", n.body());
            obj.put("createdAt", n.createdAt());
            obj.put("updatedAt", n.updatedAt());
            items.add(obj);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", VERSION);
        root.put("notes", items);
        return MiniJson.stringify(root);
    }

    /**
     * Decodifica; texto vazio/nulo vira lista vazia. Entradas malformadas individuais são
     * ignoradas, mas JSON inválido lança {@link IllegalArgumentException}.
     */
    public static List<Note> decode(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        Object root;
        try {
            root = MiniJson.parse(json);
        } catch (JsonException e) {
            throw new IllegalArgumentException("JSON de notas inválido: " + e.getMessage(), e);
        }
        List<?> items;
        if (root instanceof Map<?, ?> map) {
            Object notes = map.get("notes");
            if (!(notes instanceof List<?> list)) {
                throw new IllegalArgumentException("Campo 'notes' ausente ou inválido");
            }
            items = list;
        } else if (root instanceof List<?> list) {
            items = list; // formato legado: array puro
        } else {
            throw new IllegalArgumentException("Raiz do JSON deve ser objeto ou array");
        }

        List<Note> result = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> obj)) {
                continue;
            }
            Object id = obj.get("id");
            if (!(id instanceof String s) || s.isBlank()) {
                continue;
            }
            long created = asLong(obj.get("createdAt"), 0);
            long updated = asLong(obj.get("updatedAt"), created);
            result.add(new Note(s, asString(obj.get("title")), asString(obj.get("body")), created, updated));
        }
        return result;
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : "";
    }

    private static long asLong(Object value, long fallback) {
        return value instanceof Number n ? n.longValue() : fallback;
    }
}
