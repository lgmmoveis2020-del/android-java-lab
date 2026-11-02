package com.lgm.androidlab.core.calc;

/**
 * Erro de sintaxe ou de avaliação, com a posição (0-based) na expressão original.
 */
public final class ExpressionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int position;

    public ExpressionException(String message, int position) {
        super(message);
        this.position = position;
    }

    /** Índice do caractere onde o problema foi detectado, ou -1 se não se aplica. */
    public int position() {
        return position;
    }
}
