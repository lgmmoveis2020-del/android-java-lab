package com.lgm.androidlab.labs.calculator;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.color.MaterialColors;
import com.lgm.androidlab.R;
import com.lgm.androidlab.common.LabActivity;
import com.lgm.androidlab.core.calc.ExpressionException;
import com.lgm.androidlab.databinding.ActivityCalculatorBinding;

/** Lab 3 — calculadora científica com parser próprio (precedência, funções, fatorial, ans). */
public class CalculatorLabActivity extends LabActivity {

    private static final String OPERATORS = "+−-×*÷/^%!";

    private ActivityCalculatorBinding binding;
    private CalculatorViewModel viewModel;
    private HistoryAdapter adapter;
    private boolean justEvaluated;
    private int previewColor;
    private int resultColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalculatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupScreen(binding.getRoot(), binding.toolbar);
        viewModel = new ViewModelProvider(this).get(CalculatorViewModel.class);

        previewColor = MaterialColors.getColor(binding.resultText,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        // colorPrimary é declarado pelo AppCompat, não pelo Material (R não-transitivo!).
        resultColor = MaterialColors.getColor(binding.resultText, androidx.appcompat.R.attr.colorPrimary);

        adapter = new HistoryAdapter(new HistoryAdapter.Listener() {
            @Override
            public void onExpressionSelected(@NonNull String expression) {
                setExpression(expression);
            }

            @Override
            public void onResultSelected(@NonNull String result) {
                insert(result);
            }
        });
        binding.historyList.setLayoutManager(new LinearLayoutManager(this));
        binding.historyList.setAdapter(adapter);
        viewModel.history().observe(this, list -> {
            adapter.submitList(list);
            binding.historyEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.expressionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                showPreview(s.toString());
            }
        });
        binding.expressionInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                evaluate();
                return true;
            }
            return false;
        });
        binding.expressionInput.setShowSoftInputOnFocus(false);

        wireKeypad(binding.keypad);
        binding.keyEquals.setOnClickListener(v -> evaluate());
        binding.keyBackspace.setOnClickListener(v -> backspace());
        binding.keyBackspace.setOnLongClickListener(v -> {
            setExpression("");
            return true;
        });
        binding.keyClear.setOnClickListener(v -> setExpression(""));
        binding.keyClear.setOnLongClickListener(v -> {
            viewModel.clearHistory();
            snack(binding.getRoot(), getString(R.string.calc_history_cleared));
            return true;
        });
        showPreview(text());
    }

    /** Todo botão do teclado com {@code android:tag} insere o texto da tag. */
    private void wireKeypad(@NonNull ViewGroup keypad) {
        for (int i = 0; i < keypad.getChildCount(); i++) {
            View child = keypad.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof String token) {
                child.setOnClickListener(v -> onToken(token));
            }
        }
    }

    private void onToken(@NonNull String token) {
        if (justEvaluated) {
            justEvaluated = false;
            boolean isOperator = OPERATORS.indexOf(token.charAt(0)) >= 0;
            setExpression(isOperator ? "ans" + token : token);
            return;
        }
        insert(token);
    }

    private void insert(@NonNull String token) {
        Editable editable = binding.expressionInput.getText();
        if (editable == null) {
            binding.expressionInput.setText(token);
            binding.expressionInput.setSelection(token.length());
            return;
        }
        int start = Math.max(0, binding.expressionInput.getSelectionStart());
        int end = Math.max(start, binding.expressionInput.getSelectionEnd());
        editable.replace(start, end, token);
        binding.expressionInput.setSelection(start + token.length());
    }

    private void backspace() {
        justEvaluated = false;
        Editable editable = binding.expressionInput.getText();
        if (editable == null || editable.length() == 0) {
            return;
        }
        int start = Math.max(0, binding.expressionInput.getSelectionStart());
        int end = Math.max(start, binding.expressionInput.getSelectionEnd());
        if (start == end && start > 0) {
            editable.delete(start - 1, start);
        } else if (end > start) {
            editable.delete(start, end);
        }
    }

    private void setExpression(@NonNull String expression) {
        justEvaluated = false;
        binding.expressionInput.setText(expression);
        binding.expressionInput.setSelection(expression.length());
    }

    @NonNull
    private String text() {
        Editable e = binding.expressionInput.getText();
        return e == null ? "" : e.toString();
    }

    private void showPreview(@NonNull String source) {
        CalculatorViewModel.Preview preview = viewModel.preview(source);
        binding.resultText.setTextColor(previewColor);
        if (preview.result() != null) {
            binding.resultText.setText(preview.result());
            binding.expressionLayout.setError(null);
        } else {
            binding.resultText.setText(source.isEmpty() ? "0" : "…");
            // Só mostra o erro depois do "=" para não incomodar enquanto digita.
            binding.expressionLayout.setError(null);
        }
    }

    private void evaluate() {
        String source = text();
        if (source.trim().isEmpty()) {
            return;
        }
        try {
            String result = viewModel.evaluate(source);
            binding.resultText.setText(result);
            binding.resultText.setTextColor(resultColor);
            binding.expressionLayout.setError(null);
            justEvaluated = true;
            binding.historyList.scrollToPosition(0);
        } catch (ExpressionException e) {
            binding.expressionLayout.setError(getString(R.string.calc_error_at, e.getMessage(), e.position() + 1));
            int pos = Math.min(Math.max(0, e.position()), source.length());
            binding.expressionInput.setSelection(pos);
        } catch (RuntimeException e) {
            binding.expressionLayout.setError(e.getMessage());
        }
    }
}
