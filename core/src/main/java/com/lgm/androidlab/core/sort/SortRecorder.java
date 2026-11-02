package com.lgm.androidlab.core.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Executa um algoritmo de ordenação sobre uma cópia dos dados e grava cada comparação e
 * mutação como {@link SortStep}. Reproduzir os passos mutáveis sobre o array original
 * produz exatamente o array ordenado.
 */
public final class SortRecorder {

    private final int[] data;
    private final List<SortStep> steps = new ArrayList<>();

    private SortRecorder(int[] input) {
        this.data = input.clone();
    }

    public static List<SortStep> record(int[] input, SortAlgorithm algorithm) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(algorithm, "algorithm");
        SortRecorder recorder = new SortRecorder(input);
        switch (algorithm) {
            case BUBBLE -> recorder.bubble();
            case INSERTION -> recorder.insertion();
            case SELECTION -> recorder.selection();
            case QUICK -> recorder.quick(0, input.length - 1);
            case MERGE -> recorder.merge(0, input.length - 1);
            case HEAP -> recorder.heap();
        }
        return Collections.unmodifiableList(recorder.steps);
    }

    /** Aplica todos os passos a uma cópia de {@code input} e devolve o resultado. */
    public static int[] replay(int[] input, List<SortStep> steps) {
        int[] copy = input.clone();
        for (SortStep step : steps) {
            step.applyTo(copy);
        }
        return copy;
    }

    // ------------------------------------------------------------------ algoritmos

    private void bubble() {
        int n = data.length;
        for (int end = n - 1; end > 0; end--) {
            boolean swapped = false;
            for (int i = 0; i < end; i++) {
                if (less(i + 1, i)) {
                    swap(i, i + 1);
                    swapped = true;
                }
            }
            if (!swapped) {
                return;
            }
        }
    }

    private void insertion() {
        for (int i = 1; i < data.length; i++) {
            int j = i;
            while (j > 0 && less(j, j - 1)) {
                swap(j, j - 1);
                j--;
            }
        }
    }

    private void selection() {
        int n = data.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) {
                if (less(j, min)) {
                    min = j;
                }
            }
            if (min != i) {
                swap(i, min);
            }
        }
    }

    private void quick(int low, int high) {
        if (low >= high) {
            return;
        }
        // mediana de três reduz o pior caso em entradas quase ordenadas
        int mid = low + (high - low) / 2;
        if (less(mid, low)) {
            swap(mid, low);
        }
        if (less(high, low)) {
            swap(high, low);
        }
        if (less(high, mid)) {
            swap(high, mid);
        }
        swap(mid, high);
        int pivot = data[high];
        int store = low;
        for (int i = low; i < high; i++) {
            steps.add(SortStep.compare(i, high));
            if (data[i] < pivot) {
                if (i != store) {
                    swap(i, store);
                }
                store++;
            }
        }
        if (store != high) {
            swap(store, high);
        }
        quick(low, store - 1);
        quick(store + 1, high);
    }

    private void merge(int low, int high) {
        if (low >= high) {
            return;
        }
        int mid = low + (high - low) / 2;
        merge(low, mid);
        merge(mid + 1, high);

        int[] merged = new int[high - low + 1];
        int i = low;
        int j = mid + 1;
        int k = 0;
        while (i <= mid && j <= high) {
            steps.add(SortStep.compare(i, j));
            if (data[j] < data[i]) {
                merged[k++] = data[j++];
            } else {
                merged[k++] = data[i++];
            }
        }
        while (i <= mid) {
            merged[k++] = data[i++];
        }
        while (j <= high) {
            merged[k++] = data[j++];
        }
        for (int t = 0; t < merged.length; t++) {
            set(low + t, merged[t]);
        }
    }

    private void heap() {
        int n = data.length;
        for (int i = n / 2 - 1; i >= 0; i--) {
            siftDown(i, n);
        }
        for (int end = n - 1; end > 0; end--) {
            swap(0, end);
            siftDown(0, end);
        }
    }

    private void siftDown(int root, int size) {
        int parent = root;
        while (true) {
            int left = 2 * parent + 1;
            int right = left + 1;
            int largest = parent;
            if (left < size && less(largest, left)) {
                largest = left;
            }
            if (right < size && less(largest, right)) {
                largest = right;
            }
            if (largest == parent) {
                return;
            }
            swap(parent, largest);
            parent = largest;
        }
    }

    // ------------------------------------------------------------------ primitivas gravadas

    private boolean less(int a, int b) {
        steps.add(SortStep.compare(a, b));
        return data[a] < data[b];
    }

    private void swap(int a, int b) {
        if (a == b) {
            return;
        }
        steps.add(SortStep.swap(a, b));
        int tmp = data[a];
        data[a] = data[b];
        data[b] = tmp;
    }

    private void set(int index, int value) {
        if (data[index] == value) {
            return;
        }
        steps.add(SortStep.set(index, value));
        data[index] = value;
    }
}
