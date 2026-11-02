package com.lgm.androidlab.labs.calculator;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lgm.androidlab.databinding.ItemHistoryBinding;
import com.lgm.androidlab.labs.calculator.CalculatorViewModel.HistoryEntry;

/** Histórico de expressões; clique reutiliza a expressão, clique longo reutiliza o resultado. */
public final class HistoryAdapter extends ListAdapter<HistoryEntry, HistoryAdapter.ViewHolder> {

    /** Callbacks de seleção. */
    public interface Listener {
        void onExpressionSelected(@NonNull String expression);

        void onResultSelected(@NonNull String result);
    }

    private static final DiffUtil.ItemCallback<HistoryEntry> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull HistoryEntry oldItem, @NonNull HistoryEntry newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull HistoryEntry oldItem, @NonNull HistoryEntry newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final Listener listener;

    public HistoryAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry entry = getItem(position);
        holder.binding.historyExpression.setText(entry.expression());
        holder.binding.historyResult.setText(entry.result());
        holder.binding.getRoot().setOnClickListener(v -> listener.onExpressionSelected(entry.expression()));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            listener.onResultSelected(entry.result());
            return true;
        });
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;

        ViewHolder(@NonNull ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
