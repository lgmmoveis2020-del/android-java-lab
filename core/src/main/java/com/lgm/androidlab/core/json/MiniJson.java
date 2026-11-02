package com.lgm.androidlab.core.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor/escritor JSON minimalista e sem dependências (RFC 8259).
 *
 * <p>Mapeamento de tipos:</p>
 * <ul>
 *   <li>objeto → {@code Map<String, Object>} (ordem de inserção preservada)</li>
 *   <li>array → {@code List<Object>}</li>
 *   <li>string → {@code String}</li>
 *   <li>número → {@code Long} quando inteiro representável, senão {@code Double}</li>
 *   <li>true/false → {@code Boolean}; null → {@code null}</li>
 * </ul>
 */
public final class MiniJson {

    private MiniJson() {
    }

    // ------------------------------------------------------------------ escrita

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Boolean b) {
            sb.append(b ? "true" : "false");
        } else if (value instanceof Number n) {
            writeNumber(sb, n);
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                write(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof Collection<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, item);
            }
            sb.append(']');
        } else if (value instanceof Enum<?> e) {
            writeString(sb, e.name());
        } else {
            throw new IllegalArgumentException("Tipo não suportado: " + value.getClass().getName());
        }
    }

    private static void writeNumber(StringBuilder sb, Number n) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else {
            sb.append(n.longValue());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ leitura

    public static Object parse(String json) {
        if (json == null) {
            throw new JsonException("JSON nulo", 0);
        }
        Reader reader = new Reader(json);
        reader.skipWhitespace();
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (!reader.atEnd()) {
            throw new JsonException("Conteúdo inesperado após o valor", reader.pos);
        }
        return value;
    }

    /** Conveniência: faz o parse e exige um objeto. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new JsonException("Esperado um objeto JSON", 0);
    }

    /** Conveniência: faz o parse e exige um array. */
    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        Object value = parse(json);
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new JsonException("Esperado um array JSON", 0);
    }

    private static final class Reader {
        private final String src;
        private int pos;

        Reader(String src) {
            this.src = src;
        }

        boolean atEnd() {
            return pos >= src.length();
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            if (atEnd()) {
                throw new JsonException("Fim inesperado", pos);
            }
            return src.charAt(pos);
        }

        private void expect(char c) {
            if (peek() != c) {
                throw new JsonException("Esperado '" + c + "' mas veio '" + peek() + "'", pos);
            }
            pos++;
        }

        Object readValue() {
            char c = peek();
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield readNumber();
                    }
                    throw new JsonException("Caractere inesperado '" + c + "'", pos);
                }
            };
        }

        private Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw new JsonException("Chave de objeto deve ser string", pos);
                }
                String key = readString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, readValue());
                skipWhitespace();
                char c = peek();
                pos++;
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new JsonException("Esperado ',' ou '}'", pos - 1);
                }
            }
        }

        private List<Object> readArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(readValue());
                skipWhitespace();
                char c = peek();
                pos++;
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new JsonException("Esperado ',' ou ']'", pos - 1);
                }
            }
        }

        private String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = peek();
                pos++;
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char esc = peek();
                    pos++;
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) {
                                throw new JsonException("Escape \\u incompleto", pos);
                            }
                            try {
                                sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            } catch (NumberFormatException e) {
                                throw new JsonException("Escape \\u inválido", pos);
                            }
                            pos += 4;
                        }
                        default -> throw new JsonException("Escape inválido '\\" + esc + "'", pos - 1);
                    }
                } else if (c < 0x20) {
                    throw new JsonException("Caractere de controle dentro de string", pos - 1);
                } else {
                    sb.append(c);
                }
            }
        }

        private Number readNumber() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            boolean integral = true;
            while (!atEnd()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    integral = false;
                    pos++;
                } else {
                    break;
                }
            }
            String text = src.substring(start, pos);
            try {
                if (integral) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException overflow) {
                        return Double.parseDouble(text);
                    }
                }
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                throw new JsonException("Número inválido '" + text + "'", start);
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!src.startsWith(literal, pos)) {
                throw new JsonException("Literal inválido", pos);
            }
            pos += literal.length();
            return value;
        }
    }
}
