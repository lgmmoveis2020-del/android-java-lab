package com.lgm.androidlab.core.primes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Crivo de Eratóstenes segmentado, pensado para rodar em uma thread de fundo:
 * reporta progresso a cada segmento e pode ser cancelado de forma cooperativa.
 */
public final class PrimeSieve {

    /** Tamanho padrão do segmento (cabe confortavelmente no cache L1/L2). */
    public static final int DEFAULT_SEGMENT_SIZE = 1 << 16;

    /** Resultado de uma contagem. */
    public record Result(long limit, long count, long largest, long elapsedNanos, boolean cancelled) {
        public double elapsedMillis() {
            return elapsedNanos / 1_000_000.0;
        }
    }

    /** Callback de progresso; {@code fraction} vai de 0.0 a 1.0. */
    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(double fraction, long primesSoFar);
    }

    private PrimeSieve() {
    }

    /** Conta os primos até {@code limit} (inclusive), sem progresso nem cancelamento. */
    public static Result count(long limit) {
        return count(limit, DEFAULT_SEGMENT_SIZE, null, () -> false);
    }

    /**
     * Conta os primos até {@code limit} (inclusive).
     *
     * @param limit        limite superior, >= 0
     * @param segmentSize  tamanho de cada segmento do crivo (>= 16)
     * @param listener     opcional; chamado na thread que executa o crivo
     * @param cancelled    consultado a cada segmento; se retornar true, para com {@code cancelled=true}
     */
    public static Result count(long limit, int segmentSize, ProgressListener listener,
                               BooleanSupplier cancelled) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit deve ser >= 0");
        }
        if (segmentSize < 16) {
            throw new IllegalArgumentException("segmentSize deve ser >= 16");
        }
        long start = System.nanoTime();
        if (limit < 2) {
            report(listener, 1.0, 0);
            return new Result(limit, 0, -1, System.nanoTime() - start, false);
        }

        int sqrt = (int) Math.sqrt((double) limit);
        int[] basePrimes = simpleSieve(sqrt);

        long count = 0;
        long largest = -1;
        BitSet composite = new BitSet(segmentSize);

        for (long low = 2; low <= limit; low += segmentSize) {
            if (cancelled.getAsBoolean()) {
                return new Result(limit, count, largest, System.nanoTime() - start, true);
            }
            long high = Math.min(low + segmentSize - 1, limit);
            composite.clear();

            for (int p : basePrimes) {
                long pSquared = (long) p * p;
                if (pSquared > high) {
                    break;
                }
                long first = Math.max(pSquared, ((low + p - 1) / p) * p);
                for (long m = first; m <= high; m += p) {
                    composite.set((int) (m - low));
                }
            }

            int segmentLength = (int) (high - low + 1);
            for (int i = composite.nextClearBit(0); i < segmentLength; i = composite.nextClearBit(i + 1)) {
                count++;
                largest = low + i;
            }
            report(listener, Math.min(1.0, (double) (high - 1) / (limit - 1)), count);
        }
        report(listener, 1.0, count);
        return new Result(limit, count, largest, System.nanoTime() - start, false);
    }

    /** Lista todos os primos até {@code limit} (use apenas para limites modestos). */
    public static List<Long> listPrimes(int limit) {
        List<Long> primes = new ArrayList<>();
        for (int p : simpleSieve(limit)) {
            primes.add((long) p);
        }
        return primes;
    }

    /** Teste de primalidade determinístico (Miller-Rabin com bases fixas para 64 bits). */
    public static boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }
        for (long p : new long[] {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37}) {
            if (n % p == 0) {
                return n == p;
            }
        }
        long d = n - 1;
        int r = 0;
        while ((d & 1) == 0) {
            d >>= 1;
            r++;
        }
        for (long a : new long[] {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37}) {
            if (!millerRabinWitness(a, d, r, n)) {
                return false;
            }
        }
        return true;
    }

    private static boolean millerRabinWitness(long a, long d, int r, long n) {
        long x = powMod(a, d, n);
        if (x == 1 || x == n - 1) {
            return true;
        }
        for (int i = 1; i < r; i++) {
            x = mulMod(x, x, n);
            if (x == n - 1) {
                return true;
            }
        }
        return false;
    }

    private static long powMod(long base, long exp, long mod) {
        long result = 1;
        long b = base % mod;
        long e = exp;
        while (e > 0) {
            if ((e & 1) == 1) {
                result = mulMod(result, b, mod);
            }
            b = mulMod(b, b, mod);
            e >>= 1;
        }
        return result;
    }

    private static long mulMod(long a, long b, long mod) {
        if (mod < (1L << 31)) {
            return (a * b) % mod;
        }
        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).mod(BigInteger.valueOf(mod)).longValue();
    }

    private static int[] simpleSieve(int limit) {
        if (limit < 2) {
            return new int[0];
        }
        BitSet composite = new BitSet(limit + 1);
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= limit; i++) {
            if (!composite.get(i)) {
                primes.add(i);
                for (long m = (long) i * i; m <= limit; m += i) {
                    composite.set((int) m);
                }
            }
        }
        int[] result = new int[primes.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = primes.get(i);
        }
        return result;
    }

    private static void report(ProgressListener listener, double fraction, long count) {
        if (listener != null) {
            listener.onProgress(fraction, count);
        }
    }
}
