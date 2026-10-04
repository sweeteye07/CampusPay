package com.example.campuspay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter
        extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<CreditTransaction> items;

    public HistoryAdapter(List<CreditTransaction> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);

        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull HistoryViewHolder holder,
            int position
    ) {
        CreditTransaction item = items.get(position);

        String description = item.getDescription();

        if (description == null || description.isEmpty()) {
            if (item.isEarn() && item.getEventTitle() != null) {
                description = "Earned for " + item.getEventTitle();
            } else {
                description = "Credit transaction";
            }
        }

        holder.tvDescription.setText(description);

        if (item.getCreatedAt() != null) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            );

            holder.tvDate.setText(
                    format.format(item.getCreatedAt().toDate())
            );
        } else {
            holder.tvDate.setText("");
        }

        long credits = item.getCredits() != null ? item.getCredits() : 0L;

        if (item.isEarn()) {
            holder.tvAmount.setText("+" + credits);
            holder.tvAmount.setTextColor(0xFF2E7D32);
        } else {
            holder.tvAmount.setText("-" + credits);
            holder.tvAmount.setTextColor(0xFFC62828);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        final TextView tvDescription;
        final TextView tvDate;
        final TextView tvAmount;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDescription = itemView.findViewById(R.id.tvHistoryDescription);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvAmount = itemView.findViewById(R.id.tvHistoryAmount);
        }
    }
}
