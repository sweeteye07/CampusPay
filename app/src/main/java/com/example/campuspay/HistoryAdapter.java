package com.example.campuspay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter
        extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_EARNED = 1;
    public static final int FILTER_RECEIVED = 2;
    public static final int FILTER_SENT = 3;
    public static final int FILTER_SPENT = 4;

    private final List<CreditTransaction> allItems = new ArrayList<>();
    private final List<CreditTransaction> visibleItems = new ArrayList<>();
    private int filter = FILTER_ALL;

    public HistoryAdapter(List<CreditTransaction> items) {
        setItems(items);
    }

    public void setItems(List<CreditTransaction> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter();
    }

    public void setFilter(int filter) {
        this.filter = filter;
        applyFilter();
    }

    private void applyFilter() {
        visibleItems.clear();
        for (CreditTransaction item : allItems) {
            if (filter == FILTER_ALL || matches(item)) {
                visibleItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matches(CreditTransaction item) {
        switch (filter) {
            case FILTER_EARNED:
                return item.isEarn();
            case FILTER_RECEIVED:
                return item.isReceive();
            case FILTER_SENT:
                return item.isSend();
            case FILTER_SPENT:
                return item.isSpend();
            default:
                return true;
        }
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
        CreditTransaction item = visibleItems.get(position);

        String description = item.getDescription();

        if (description == null || description.isEmpty()) {
            if (item.isEarn() && item.getEventTitle() != null) {
                description = "Earned for " + item.getEventTitle();
            } else if (item.isSpend() && item.getItemTitle() != null) {
                description = "Redeemed " + item.getItemTitle();
            } else if (item.isSend()) {
                String to = item.getCounterpartyEmail() != null
                        ? item.getCounterpartyEmail() : "student";
                description = "Sent to " + to;
            } else if (item.isReceive()) {
                String from = item.getCounterpartyEmail() != null
                        ? item.getCounterpartyEmail() : "student";
                description = "Received from " + from;
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

        if (item.isOutgoing()) {
            holder.tvAmount.setText("-" + credits);
            holder.tvAmount.setTextColor(0xFFC62828);
        } else {
            holder.tvAmount.setText("+" + credits);
            holder.tvAmount.setTextColor(0xFF2E7D32);
        }

        bindTypeBadge(holder, item);
    }

    /** Colored icon + pill per transaction type. */
    private void bindTypeBadge(HistoryViewHolder holder, CreditTransaction item) {
        int icon;
        int tint;
        int pillBg;
        int pillText;
        String label;

        if (item.isEarn()) {
            icon = R.drawable.ic_event;
            tint = 0xFF2E7D32;
            pillBg = R.drawable.bg_pill_success;
            pillText = 0xFF2E7D32;
            label = "EARNED";
        } else if (item.isReceive()) {
            icon = R.drawable.ic_wallet;
            tint = 0xFF2E7D32;
            pillBg = R.drawable.bg_pill_success;
            pillText = 0xFF2E7D32;
            label = "RECEIVED";
        } else if (item.isSpend()) {
            icon = R.drawable.ic_wallet;
            tint = 0xFFC62828;
            pillBg = R.drawable.bg_pill_danger;
            pillText = 0xFFC62828;
            label = "REDEEMED";
        } else {
            icon = R.drawable.ic_logout;
            tint = 0xFFC62828;
            pillBg = R.drawable.bg_pill_danger;
            pillText = 0xFFC62828;
            label = "SENT";
        }

        holder.ivIcon.setImageResource(icon);
        holder.ivIcon.setImageTintList(
                android.content.res.ColorStateList.valueOf(tint));
        holder.tvPill.setText(label);
        holder.tvPill.setBackgroundResource(pillBg);
        holder.tvPill.setTextColor(pillText);
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        final android.widget.ImageView ivIcon;
        final TextView tvDescription;
        final TextView tvDate;
        final TextView tvAmount;
        final TextView tvPill;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            ivIcon = itemView.findViewById(R.id.ivHistoryIcon);
            tvDescription = itemView.findViewById(R.id.tvHistoryDescription);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvAmount = itemView.findViewById(R.id.tvHistoryAmount);
            tvPill = itemView.findViewById(R.id.tvHistoryPill);
        }
    }
}
