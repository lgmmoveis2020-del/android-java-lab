package com.lgm.androidlab.labs.primes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lgm.androidlab.core.primes.PrimeSieve;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Roda o crivo numa thread de fundo, com progresso e cancelamento; sobrevive a rotações. */
public class PrimeViewModel extends ViewModel {

    /** Fase do trabalho. */
    public enum Status { IDLE, RUNNING, DONE, CANCELLED }

    /** Snapshot imutável do estado para a UI. */
    public record State(@NonNull Status status, double progress, long primesSoFar,
                        @Nullable PrimeSieve.Result result) {
        public static State idle() {
            return new State(Status.IDLE, 0, 0, null);
        }

        public boolean isRunning() {
            return status == Status.RUNNING;
        }
    }

    private static final long PROGRESS_THROTTLE_NANOS = 40_000_000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "prime-sieve");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private final MutableLiveData<State> state = new MutableLiveData<>(State.idle());
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile int generation;

    @NonNull
    public LiveData<State> state() {
        return state;
    }

    /** Inicia uma contagem até {@code limit}; ignora se já houver uma em andamento. */
    public void start(long limit) {
        State current = state.getValue();
        if (current != null && current.isRunning()) {
            return;
        }
        cancelled.set(false);
        final int myGeneration = ++generation;
        state.setValue(new State(Status.RUNNING, 0, 0, null));
        executor.execute(() -> {
            final long[] lastPublish = {0};
            PrimeSieve.Result result = PrimeSieve.count(limit, PrimeSieve.DEFAULT_SEGMENT_SIZE,
                    (fraction, primesSoFar) -> {
                        long now = System.nanoTime();
                        if (now - lastPublish[0] >= PROGRESS_THROTTLE_NANOS || fraction >= 1.0) {
                            lastPublish[0] = now;
                            if (myGeneration == generation) {
                                state.postValue(new State(Status.RUNNING, fraction, primesSoFar, null));
                            }
                        }
                    },
                    cancelled::get);
            if (myGeneration == generation) {
                Status status = result.cancelled() ? Status.CANCELLED : Status.DONE;
                state.postValue(new State(status, result.cancelled() ? 0 : 1.0, result.count(), result));
            }
        });
    }

    public void cancel() {
        cancelled.set(true);
    }

    public void reset() {
        cancel();
        generation++;
        state.setValue(State.idle());
    }

    @Override
    protected void onCleared() {
        cancelled.set(true);
        executor.shutdownNow();
    }
}
