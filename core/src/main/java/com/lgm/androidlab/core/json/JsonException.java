package com.lgm.androidlab.core.json;

/**
 * Erro de sintaxe JSON com posição.
 */
public final class JsonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int position;

    public JsonException(String message, int position) {
        super(message + " (posição " + position + ")");
        this.position = position;
    }

    public int position() {
        return position;
    }
}
