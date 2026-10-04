package com.example.campuspay;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onClick(AppNotification notification);
    }

    private final List<AppNotification> items;
    private final OnNotificationClickListener listener;

    public NotificationsAdapter(
            List<AppNotification> items,
            OnNotificationClickListener listener
    ) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(AppNotification.relativeTime(
                item.getTimestampMillis(), System.currentTimeMillis()));
        holder.ivIcon.setImageResource(iconFor(item.getType()));
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    private int iconFor(String type) {
        if (AppNotification.TYPE_PAYMENT_RECEIVED.equals(type)) {
            return R.drawable.ic_receive;
        }
        if (AppNotification.TYPE_CREDITS_EARNED.equals(type)
                || AppNotification.TYPE_REWARD_REDEEMED.equals(type)) {
            return R.drawable.ic_wallet;
        }
        if (AppNotification.TYPE_EVENT_REMINDER.equals(type)) {
            return R.drawable.ic_event;
        }
        return R.drawable.ic_bell;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvMessage;
        final TextView tvTime;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
        }
    }

    /** Maps a notification to its destination screen. */
    public static Intent targetIntent(
            android.content.Context context, AppNotification notification) {
        String type = notification.getType();
        if (AppNotification.TYPE_PAYMENT_RECEIVED.equals(type)
                || AppNotification.TYPE_CREDITS_EARNED.equals(type)) {
            return new Intent(context, HistoryActivity.class);
        }
        if (AppNotification.TYPE_REWARD_REDEEMED.equals(type)) {
            return new Intent(context, MyOrdersActivity.class);
        }
        if (AppNotification.TYPE_NEW_ITEM.equals(type)) {
            return new Intent(context, MarketplaceActivity.class);
        }
        if (AppNotification.TYPE_EVENT_REMINDER.equals(type)) {
            return new Intent(context, MyEventsActivity.class);
        }
        return new Intent(context, EventsActivity.class);
    }
}
