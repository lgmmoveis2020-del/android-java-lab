package com.lgm.androidlab.core.sort;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SortRecorderTest {

    @ParameterizedTest
    @EnumSource(SortAlgorithm.class)
    @DisplayName("reproduzir os passos ordena arrays aleatórios, ordenados, invertidos e com repetições")
    void replayingStepsSorts(SortAlgorithm algorithm) {
        Random random = new Random(algorithm.ordinal());
        int[][] inputs = {
                {},
                {7},
                {2, 1},
                {5, 5, 5, 5},
                {1, 2, 3, 4, 5, 6, 7, 8},
                {8, 7, 6, 5, 4, 3, 2, 1},
                random.ints(50, 0, 100).toArray(),
                random.ints(257, -1000, 1000).toArray(),
        };
        for (int[] input : inputs) {
            int[] expected = input.clone();
            Arrays.sort(expected);
            List<SortStep> steps = SortRecorder.record(input, algorithm);

            assertArrayEquals(expected, SortRecorder.replay(input, steps),
                    algorithm + " falhou para " + Arrays.toString(input));
            for (SortStep step : steps) {
                assertTrue(step.i() >= 0 && step.i() < Math.max(1, input.length), "índice i fora do array");
                if (step.type() != SortStep.Type.SET) {
                    assertTrue(step.j() >= 0 && step.j() < Math.max(1, input.length), "índice j fora do array");
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(SortAlgorithm.class)
    @DisplayName("entrada já ordenada não gera mutações desnecessárias em algoritmos adaptativos")
    void sortedInputProducesNoMutations(SortAlgorithm algorithm) {
        int[] sorted = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        List<SortStep> steps = SortRecorder.record(sorted, algorithm);
        long mutations = steps.stream().filter(SortStep::mutates).count();

        if (algorithm == SortAlgorithm.BUBBLE || algorithm == SortAlgorithm.INSERTION
                || algorithm == SortAlgorithm.MERGE || algorithm == SortAlgorithm.SELECTION) {
            assertEquals(0, mutations, algorithm + " não deveria mutar entrada ordenada");
        }
        assertFalse(steps.isEmpty(), "ao menos comparações devem ser gravadas");
    }

    @Test
    @DisplayName("arrays vazios e unitários não geram passos")
    void trivialInputs() {
        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            assertTrue(SortRecorder.record(new int[0], algorithm).isEmpty());
            assertTrue(SortRecorder.record(new int[] {42}, algorithm).isEmpty());
        }
    }

    @Test
    @DisplayName("bubble sort em [2,1] grava exatamente uma comparação e uma troca")
    void bubbleMinimalCase() {
        List<SortStep> steps = SortRecorder.record(new int[] {2, 1}, SortAlgorithm.BUBBLE);
        assertEquals(List.of(SortStep.compare(1, 0), SortStep.swap(0, 1)), steps);
    }

    @Test
    @DisplayName("merge sort usa SET, os demais usam SWAP")
    void stepTypesPerAlgorithm() {
        int[] input = {5, 3, 9, 1, 7, 2, 8};
        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            List<SortStep> steps = SortRecorder.record(input, algorithm);
            boolean hasSet = steps.stream().anyMatch(s -> s.type() == SortStep.Type.SET);
            boolean hasSwap = steps.stream().anyMatch(s -> s.type() == SortStep.Type.SWAP);
            if (algorithm == SortAlgorithm.MERGE) {
                assertTrue(hasSet && !hasSwap);
            } else {
                assertTrue(hasSwap && !hasSet, algorithm.toString());
            }
        }
    }

    @Test
    @DisplayName("algoritmos O(n log n) fazem bem menos comparações que os quadráticos em n = 512")
    void complexityIsVisible() {
        int[] input = new Random(3).ints(512, 0, 10_000).toArray();
        long bubble = comparisons(input, SortAlgorithm.BUBBLE);
        long selection = comparisons(input, SortAlgorithm.SELECTION);
        long quick = comparisons(input, SortAlgorithm.QUICK);
        long merge = comparisons(input, SortAlgorithm.MERGE);
        long heap = comparisons(input, SortAlgorithm.HEAP);

        assertEquals(512L * 511 / 2, selection, "selection sempre faz n(n-1)/2 comparações");
        assertTrue(quick * 5 < bubble, "quick=" + quick + " bubble=" + bubble);
        assertTrue(merge * 5 < bubble, "merge=" + merge + " bubble=" + bubble);
        assertTrue(heap * 5 < bubble, "heap=" + heap + " bubble=" + bubble);
    }

    private static long comparisons(int[] input, SortAlgorithm algorithm) {
        return SortRecorder.record(input, algorithm).stream()
                .filter(s -> s.type() == SortStep.Type.COMPARE)
                .count();
    }

    @Test
    @DisplayName("a lista de passos é imutável e SortStep.applyTo funciona isoladamente")
    void stepsAreImmutable() {
        List<SortStep> steps = SortRecorder.record(new int[] {3, 1, 2}, SortAlgorithm.INSERTION);
        assertThrows(UnsupportedOperationException.class, steps::clear);

        int[] data = {1, 2, 3};
        SortStep.swap(0, 2).applyTo(data);
        assertArrayEquals(new int[] {3, 2, 1}, data);
        SortStep.set(1, 9).applyTo(data);
        assertArrayEquals(new int[] {3, 9, 1}, data);
        SortStep.compare(0, 1).applyTo(data);
        assertArrayEquals(new int[] {3, 9, 1}, data);
        assertFalse(SortStep.compare(0, 1).mutates());
    }

    @Test
    @DisplayName("metadados dos algoritmos")
    void metadata() {
        assertEquals("Quick Sort", SortAlgorithm.QUICK.displayName());
        assertEquals("O(n log n)", SortAlgorithm.MERGE.complexity());
        assertTrue(SortAlgorithm.MERGE.stable());
        assertFalse(SortAlgorithm.HEAP.stable());
        assertEquals("Heap Sort", SortAlgorithm.HEAP.toString());
    }
}
