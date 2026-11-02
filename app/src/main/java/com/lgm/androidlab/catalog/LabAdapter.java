package com.lgm.androidlab.catalog;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lgm.androidlab.core.color.ColorMath;
import com.lgm.androidlab.databinding.ItemLabBinding;

import java.util.List;

/** Cards do catálogo da tela inicial. */
public final class LabAdapter extends RecyclerView.Adapter<LabAdapter.ViewHolder> {

    /** Callback de clique num lab. */
    public interface OnLabClickListener {
        void onLabClick(@NonNull LabItem item);
    }

    private final List<LabItem> items;
    private final OnLabClickListener listener;

    public LabAdapter(@NonNull List<LabItem> items, @NonNull OnLabClickListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLabBinding binding = ItemLabBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id().hashCode();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLabBinding binding;

        ViewHolder(@NonNull ItemLabBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LabItem item, @NonNull OnLabClickListener listener) {
            binding.labTitle.setText(item.titleRes());
            binding.labDescription.setText(item.descriptionRes());
            binding.labIcon.setImageResource(item.iconRes());
            int accent = ContextCompat.getColor(binding.getRoot().getContext(), item.accentRes());
            binding.labIcon.setImageTintList(ColorStateList.valueOf(accent));
            binding.labIconBackground.setBackgroundTintList(
                    ColorStateList.valueOf(ColorMath.withAlpha(accent, 0x30)));
            binding.getRoot().setOnClickListener(v -> listener.onLabClick(item));
        }
    }
}
