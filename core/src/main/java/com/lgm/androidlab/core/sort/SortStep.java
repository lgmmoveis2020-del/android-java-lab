package com.lgm.androidlab.core.sort;

/**
 * Um passo observável de um algoritmo de ordenação. A UI reproduz os passos para animar.
 */
public record SortStep(Type type, int i, int j, int value) {

    public enum Type {
        /** Comparou as posições i e j (nenhuma mutação). */
        COMPARE,
        /** Trocou os valores das posições i e j. */
        SWAP,
        /** Escreveu {@code value} na posição i (usado por merge sort). */
        SET
    }

    public static SortStep compare(int i, int j) {
        return new SortStep(Type.COMPARE, i, j, 0);
    }

    public static SortStep swap(int i, int j) {
        return new SortStep(Type.SWAP, i, j, 0);
    }

    public static SortStep set(int i, int value) {
        return new SortStep(Type.SET, i, -1, value);
    }

    public boolean mutates() {
        return type != Type.COMPARE;
    }

    /** Aplica o passo a um array (no-op para comparações). */
    public void applyTo(int[] data) {
        switch (type) {
            case SWAP -> {
                int tmp = data[i];
                data[i] = data[j];
                data[j] = tmp;
            }
            case SET -> data[i] = value;
            case COMPARE -> {
                // nada a fazer
            }
        }
    }
}
