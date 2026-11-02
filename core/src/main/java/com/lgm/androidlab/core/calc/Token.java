package com.lgm.androidlab.core.calc;

/**
 * Menor unidade léxica de uma expressão.
 */
record Token(Type type, String text, int position) {

    enum Type {
        NUMBER, IDENTIFIER,
        PLUS, MINUS, STAR, SLASH, PERCENT, CARET, BANG,
        LPAREN, RPAREN, COMMA,
        EOF
    }
}
