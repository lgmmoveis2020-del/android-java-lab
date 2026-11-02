package com.lgm.androidlab.core.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotesTest {

    @Test
    @DisplayName("Note.create gera id único e normaliza campos")
    void createNote() {
        Note a = Note.create("  Título  ", "corpo", 1000);
        Note b = Note.create("x", null, 1000);

        assertNotEquals(a.id(), b.id());
        assertEquals("Título", a.title());
        assertEquals("", b.body());
        assertEquals(1000, a.createdAt());
        assertEquals(1000, a.updatedAt());
        assertFalse(a.isEmpty());
        assertTrue(Note.create("", "  ", 1).isEmpty());
    }

    @Test
    @DisplayName("edited preserva id e createdAt")
    void editedNote() {
        Note original = Note.create("a", "b", 10);
        Note edited = original.edited("novo", "corpo novo", 20);

        assertEquals(original.id(), edited.id());
        assertEquals(10, edited.createdAt());
        assertEquals(20, edited.updatedAt());
        assertEquals("novo", edited.title());
        assertEquals("a", original.title(), "original é imutável");
    }

    @Test
    @DisplayName("displayTitle cai para a primeira linha do corpo")
    void displayTitle() {
        assertEquals("Compras", new Note("1", "Compras", "leite", 0, 0).displayTitle());
        assertEquals("primeira linha", new Note("1", "", "primeira linha\nsegunda", 0, 0).displayTitle());
        assertEquals("(sem título)", new Note("1", " ", "  \n", 0, 0).displayTitle());
    }

    @Test
    @DisplayName("id vazio é rejeitado")
    void invalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Note(" ", "t", "b", 0, 0));
        assertThrows(NullPointerException.class, () -> new Note(null, "t", "b", 0, 0));
    }

    @Test
    @DisplayName("codec faz round-trip exato, inclusive caracteres especiais")
    void codecRoundTrip() {
        List<Note> notes = List.of(
                new Note("id-1", "Compras \"urgentes\"", "leite\npão\n\tovos \\ café", 1, 2),
                new Note("id-2", "", "", 3, 3),
                new Note("id-3", "Unicode 😀 çãé", "{\"json\": [1,2]}", 4, 5));

        String json = NoteCodec.encode(notes);
        List<Note> decoded = NoteCodec.decode(json);

        assertEquals(notes, decoded);
        assertTrue(json.startsWith("{\"version\":1,\"notes\":["));
    }

    @Test
    @DisplayName("decode tolera vazio, formato legado e entradas inválidas")
    void codecTolerance() {
        assertEquals(List.of(), NoteCodec.decode(null));
        assertEquals(List.of(), NoteCodec.decode("   "));
        assertEquals(List.of(), NoteCodec.decode("{\"version\":1,\"notes\":[]}"));

        List<Note> legacy = NoteCodec.decode("[{\"id\":\"a\",\"title\":\"t\",\"body\":\"b\",\"createdAt\":5}]");
        assertEquals(1, legacy.size());
        assertEquals(5, legacy.get(0).updatedAt(), "updatedAt ausente cai para createdAt");

        List<Note> mixed = NoteCodec.decode("{\"notes\":[{\"id\":\"ok\"}, {\"title\":\"sem id\"}, 42, {\"id\":\"\"}]}");
        assertEquals(1, mixed.size());
        assertEquals("ok", mixed.get(0).id());
        assertEquals("", mixed.get(0).title());

        assertThrows(IllegalArgumentException.class, () -> NoteCodec.decode("{not json"));
        assertThrows(IllegalArgumentException.class, () -> NoteCodec.decode("{\"version\":1}"));
        assertThrows(IllegalArgumentException.class, () -> NoteCodec.decode("\"string\""));
    }

    @Test
    @DisplayName("repositório em memória ordena por updatedAt desc e chama persist")
    void inMemoryRepository() {
        AtomicInteger persisted = new AtomicInteger();
        InMemoryNoteRepository repo = new InMemoryNoteRepository() {
            @Override
            protected void persist() {
                persisted.incrementAndGet();
            }
        };

        Note old = new Note("old", "velha", "", 1, 1);
        Note recent = new Note("new", "nova", "", 2, 9);
        Note middle = new Note("mid", "meio", "", 3, 5);
        repo.save(old);
        repo.save(recent);
        repo.save(middle);

        assertEquals(List.of(recent, middle, old), repo.all());
        assertEquals(3, repo.count());
        assertEquals(3, persisted.get());

        repo.save(old.edited("velha editada", "", 100));
        assertEquals("velha editada", repo.all().get(0).title());
        assertEquals(3, repo.count(), "save com mesmo id substitui");

        assertTrue(repo.delete("mid"));
        assertFalse(repo.delete("mid"));
        assertTrue(repo.find("mid").isEmpty());
        assertTrue(repo.find("new").isPresent());
        assertEquals(5, persisted.get(), "delete inexistente não persiste");

        repo.clear();
        assertEquals(0, repo.count());
        assertEquals(6, persisted.get());

        InMemoryNoteRepository seeded = new InMemoryNoteRepository(List.of(old, recent));
        assertEquals(2, seeded.count());
    }
}
