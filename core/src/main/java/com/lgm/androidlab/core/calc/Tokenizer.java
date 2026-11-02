package com.lgm.androidlab.core.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte o texto de uma expressão em uma lista de {@link Token}s.
 */
final class Tokenizer {

    private final String source;
    private int pos;

    private Tokenizer(String source) {
        this.source = source;
    }

    static List<Token> tokenize(String source) {
        return new Tokenizer(source).run();
    }

    private List<Token> run() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (pos >= source.length()) {
                tokens.add(new Token(Token.Type.EOF, "", pos));
                return tokens;
            }
            char c = source.charAt(pos);
            if (Character.isDigit(c) || (c == '.' && pos + 1 < source.length()
                    && Character.isDigit(source.charAt(pos + 1)))) {
                tokens.add(readNumber());
            } else if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifier());
            } else {
                tokens.add(readSymbol(c));
            }
        }
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
    }

    private Token readNumber() {
        int start = pos;
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
            pos++;
        }
        if (pos < source.length() && source.charAt(pos) == '.') {
            pos++;
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                pos++;
            }
        }
        if (pos < source.length() && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
            int save = pos;
            pos++;
            if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) {
                pos++;
            }
            if (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                    pos++;
                }
            } else {
                pos = save; // era um identificador (ex.: "2e" -> 2 * e), não notação científica
            }
        }
        return new Token(Token.Type.NUMBER, source.substring(start, pos), start);
    }

    private Token readIdentifier() {
        int start = pos;
        while (pos < source.length()
                && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
            pos++;
        }
        return new Token(Token.Type.IDENTIFIER, source.substring(start, pos), start);
    }

    private Token readSymbol(char c) {
        int start = pos++;
        Token.Type type = switch (c) {
            case '+' -> Token.Type.PLUS;
            case '-', '−' -> Token.Type.MINUS;
            case '*', '×', '·' -> Token.Type.STAR;
            case '/', '÷' -> Token.Type.SLASH;
            case '%' -> Token.Type.PERCENT;
            case '^' -> Token.Type.CARET;
            case '!' -> Token.Type.BANG;
            case '(' -> Token.Type.LPAREN;
            case ')' -> Token.Type.RPAREN;
            case ',' -> Token.Type.COMMA;
            default -> throw new ExpressionException("Caractere inesperado '" + c + "'", start);
        };
        return new Token(type, String.valueOf(c), start);
    }
}
