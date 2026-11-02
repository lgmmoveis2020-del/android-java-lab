package com.lgm.androidlab.labs.calculator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lgm.androidlab.core.calc.Calculator;
import com.lgm.androidlab.core.calc.ExpressionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Estado da calculadora que sobrevive a rotações: histórico e {@code ans}. */
public class CalculatorViewModel extends ViewModel {

    /** Uma linha do histórico. */
    public record HistoryEntry(@NonNull String expression, @NonNull String result) {
    }

    /** Resultado de uma pré-visualização (avaliação sem efeitos colaterais). */
    public record Preview(@Nullable String result, @Nullable String error, int errorPosition) {
        public static Preview empty() {
            return new Preview(null, null, -1);
        }
    }

    private static final int MAX_HISTORY = 50;

    private final Calculator calculator = new Calculator();
    private final List<HistoryEntry> entries = new ArrayList<>();
    private final MutableLiveData<List<HistoryEntry>> history =
            new MutableLiveData<>(Collections.emptyList());

    @NonNull
    public LiveData<List<HistoryEntry>> history() {
        return history;
    }

    /** Avalia sem alterar {@code ans}; nunca lança. */
    @NonNull
    public Preview preview(@NonNull String source) {
        if (source.trim().isEmpty()) {
            return Preview.empty();
        }
        try {
            double value = calculator.parse(source).evaluate(calculator.environment());
            return new Preview(Calculator.format(value), null, -1);
        } catch (ExpressionException e) {
            return new Preview(null, e.getMessage(), e.position());
        } catch (RuntimeException e) {
            return new Preview(null, e.getMessage(), -1);
        }
    }

    /**
     * Avalia de verdade, atualiza {@code ans} e o histórico.
     *
     * @return o resultado formatado
     * @throws ExpressionException em caso de erro
     */
    @NonNull
    public String evaluate(@NonNull String source) {
        double value = calculator.evaluate(source);
        String formatted = Calculator.format(value);
        entries.add(0, new HistoryEntry(source.trim(), formatted));
        while (entries.size() > MAX_HISTORY) {
            entries.remove(entries.size() - 1);
        }
        history.setValue(new ArrayList<>(entries));
        return formatted;
    }

    public void clearHistory() {
        entries.clear();
        history.setValue(Collections.emptyList());
    }

    public boolean hasAns() {
        return calculator.hasResult();
    }
}
