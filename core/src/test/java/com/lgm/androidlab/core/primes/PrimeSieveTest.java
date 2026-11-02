package com.lgm.androidlab.core.primes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PrimeSieveTest {

    @ParameterizedTest(name = "π({0}) = {1}, maior = {2}")
    @CsvSource({
            "0, 0, -1",
            "1, 0, -1",
            "2, 1, 2",
            "3, 2, 3",
            "10, 4, 7",
            "100, 25, 97",
            "1000, 168, 997",
            "65536, 6542, 65521",
            "1000000, 78498, 999983",
    })
    void countsPrimes(long limit, long expectedCount, long expectedLargest) {
        PrimeSieve.Result result = PrimeSieve.count(limit);
        assertEquals(expectedCount, result.count());
        assertEquals(expectedLargest, result.largest());
        assertFalse(result.cancelled());
        assertEquals(limit, result.limit());
        assertTrue(result.elapsedNanos() >= 0);
    }

    @Test
    @DisplayName("tamanho do segmento não altera o resultado (inclusive limites na fronteira)")
    void segmentSizeDoesNotChangeResult() {
        for (int segment : new int[] {16, 17, 100, 1024, 1 << 20}) {
            PrimeSieve.Result r = PrimeSieve.count(10_000, segment, null, () -> false);
            assertEquals(1229, r.count(), "segmento " + segment);
            assertEquals(9973, r.largest(), "segmento " + segment);
        }
        assertEquals(1, PrimeSieve.count(17, 16, null, () -> false).count() - PrimeSieve.count(16).count());
    }

    @Test
    @DisplayName("progresso é monotônico e termina em 1.0")
    void progressIsMonotonic() {
        List<Double> fractions = new ArrayList<>();
        PrimeSieve.count(200_000, 1 << 12, (fraction, count) -> fractions.add(fraction), () -> false);

        assertTrue(fractions.size() > 10);
        for (int i = 1; i < fractions.size(); i++) {
            assertTrue(fractions.get(i) >= fractions.get(i - 1), "regressão no índice " + i);
        }
        assertEquals(1.0, fractions.get(fractions.size() - 1), 0.0);
    }

    @Test
    @DisplayName("cancelamento cooperativo interrompe e marca o resultado")
    void cancellationStopsEarly() {
        AtomicBoolean cancel = new AtomicBoolean(false);
        AtomicInteger segments = new AtomicInteger();
        PrimeSieve.Result r = PrimeSieve.count(10_000_000, 1 << 12, (f, c) -> {
            if (segments.incrementAndGet() == 3) {
                cancel.set(true);
            }
        }, cancel::get);

        assertTrue(r.cancelled());
        assertTrue(r.count() > 0);
        assertTrue(r.count() < 664_579, "não deveria ter terminado");
    }

    @ParameterizedTest
    @ValueSource(longs = {2, 3, 5, 7, 97, 7919, 65521, 2147483647L, 1000000007L, 998244353L, 4294967311L,
            1000000000039L})
    void recognizesPrimes(long n) {
        assertTrue(PrimeSieve.isPrime(n), n + " é primo");
    }

    @ParameterizedTest
    @ValueSource(longs = {-7, 0, 1, 4, 9, 561, 1105, 1729, 2465, 25326001L, 3215031751L, 4294967297L,
            1000000007L * 3, 999999999999L})
    void recognizesComposites(long n) {
        assertFalse(PrimeSieve.isPrime(n), n + " não é primo");
    }

    @Test
    @DisplayName("isPrime concorda com o crivo até 20.000")
    void isPrimeAgreesWithSieve() {
        List<Long> primes = PrimeSieve.listPrimes(20_000);
        int idx = 0;
        for (long n = 0; n <= 20_000; n++) {
            boolean inList = idx < primes.size() && primes.get(idx) == n;
            if (inList) {
                idx++;
            }
            assertEquals(inList, PrimeSieve.isPrime(n), "n = " + n);
        }
        assertEquals(2262, primes.size());
    }

    @Test
    @DisplayName("validações")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> PrimeSieve.count(-1));
        assertThrows(IllegalArgumentException.class, () -> PrimeSieve.count(10, 8, null, () -> false));
        assertEquals(List.of(), PrimeSieve.listPrimes(1));
        assertEquals(List.of(2L, 3L, 5L, 7L), PrimeSieve.listPrimes(10));
        assertTrue(PrimeSieve.count(10).elapsedMillis() >= 0);
    }
}
