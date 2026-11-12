package com.lgm.androidlab.core.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MiniJsonTest {

    @Test
    @DisplayName("faz o parse de um documento completo")
    void parsesDocument() {
        String json = """
                {
                  "name": "Lab",
                  "version": 3,
                  "ratio": 0.75,
                  "big": 12345678901234,
                  "negative": -2.5e3,
                  "enabled": true,
                  "nothing": null,
                  "tags": ["a", "b", []],
                  "nested": {"deep": {"x": 1}}
                }
                """;
        Map<String, Object> obj = MiniJson.parseObject(json);

        assertEquals("Lab", obj.get("name"));
        assertEquals(3L, obj.get("version"));
        assertEquals(0.75, obj.get("ratio"));
        assertEquals(12345678901234L, obj.get("big"));
        assertEquals(-2500.0, obj.get("negative"));
        assertEquals(Boolean.TRUE, obj.get("enabled"));
        assertTrue(obj.containsKey("nothing"));
        assertNull(obj.get("nothing"));
        assertEquals(List.of("a", "b", List.of()), obj.get("tags"));
        assertEquals(Map.of("deep", Map.of("x", 1L)), obj.get("nested"));
        assertEquals(List.of("name", "version", "ratio", "big", "negative", "enabled", "nothing", "tags", "nested"),
                List.copyOf(obj.keySet()), "ordem das chaves é preservada");
    }

    @Test
    @DisplayName("strings com escapes e unicode fazem round-trip")
    void stringEscapesRoundTrip() {
        String original = "aspas \" barra \\ nova\nlinha tab\t ctrl\u0001 emoji 😀 acentuação ç";
        String json = MiniJson.stringify(original);

        assertEquals("\"aspas \\\" barra \\\\ nova\\nlinha tab\\t ctrl\\u0001 emoji 😀 acentuação ç\"", json);
        assertEquals(original, MiniJson.parse(json));
        assertEquals("/ é", MiniJson.parse("\"\\/ \\u00e9\""));
    }

    @Test
    @DisplayName("stringify de mapas, listas, números e enums")
    void stringifyValues() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("int", 7);
        map.put("long", 7_000_000_000L);
        map.put("double", 2.5);
        map.put("integralDouble", 3.0);
        map.put("nan", Double.NaN);
        map.put("bool", false);
        map.put("nil", null);
        map.put("list", List.of(1, "dois", List.of(3)));
        map.put("enum", Thread.State.RUNNABLE);

        assertEquals("{\"int\":7,\"long\":7000000000,\"double\":2.5,\"integralDouble\":3,\"nan\":null,"
                + "\"bool\":false,\"nil\":null,\"list\":[1,\"dois\",[3]],\"enum\":\"RUNNABLE\"}",
                MiniJson.stringify(map));
        assertEquals("[]", MiniJson.stringify(List.of()));
        assertEquals("{}", MiniJson.stringify(Map.of()));
        assertEquals("null", MiniJson.stringify(null));
    }

    @Test
    @DisplayName("parse(stringify(x)) devolve estrutura equivalente")
    void roundTrip() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("a", 1L);
        original.put("b", List.of(true, false, "x", 1.5));
        original.put("c", Map.of("k", "v"));

        Object parsed = MiniJson.parse(MiniJson.stringify(original));
        assertEquals(original, parsed);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "{", "}", "[1,]", "{\"a\":}", "{a:1}", "{\"a\" 1}", "[1 2]", "\"aberta",
            "tru", "nul", "01x", "-", "\"\\q\"", "\"\\u12\"", "{\"a\":1} extra", "\"ctrl\u0001\"",
            "[1,2]]", "{\"a\":1,}"
    })
    @DisplayName("JSON inválido lança JsonException com posição")
    void invalidJson(String json) {
        JsonException ex = assertThrows(JsonException.class, () -> MiniJson.parse(json), json);
        assertTrue(ex.position() >= 0);
        assertTrue(ex.getMessage().contains("posição"));
    }

    @Test
    @DisplayName("parseObject/parseArray validam o tipo raiz")
    void rootTypeHelpers() {
        assertThrows(JsonException.class, () -> MiniJson.parseObject("[1]"));
        assertThrows(JsonException.class, () -> MiniJson.parseArray("{}"));
        assertThrows(JsonException.class, () -> MiniJson.parse(null));
        assertEquals(List.of(1L, 2L), MiniJson.parseArray(" [1, 2] "));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.stringify(new Object()));
    }

    @Test
    @DisplayName("inteiros grandes demais para long viram double")
    void hugeIntegersBecomeDouble() {
        assertEquals(1.0E25, MiniJson.parse("10000000000000000000000000"));
        assertEquals(Long.MAX_VALUE, MiniJson.parse("9223372036854775807"));
    }
}
