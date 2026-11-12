package com.lgm.androidlab.core.sort;

/**
 * Algoritmos disponíveis no laboratório de ordenação, com complexidade típica para exibição.
 */
public enum SortAlgorithm {
    BUBBLE("Bubble Sort", "O(n²)", true),
    INSERTION("Insertion Sort", "O(n²)", true),
    SELECTION("Selection Sort", "O(n²)", false),
    QUICK("Quick Sort", "O(n log n)", false),
    MERGE("Merge Sort", "O(n log n)", true),
    HEAP("Heap Sort", "O(n log n)", false);

    private final String displayName;
    private final String complexity;
    private final boolean stable;

    SortAlgorithm(String displayName, String complexity, boolean stable) {
        this.displayName = displayName;
        this.complexity = complexity;
        this.stable = stable;
    }

    public String displayName() {
        return displayName;
    }

    public String complexity() {
        return complexity;
    }

    public boolean stable() {
        return stable;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
