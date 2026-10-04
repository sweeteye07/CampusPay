package com.example.campuspay;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter
        extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onShowQr(Redemption redemption);
    }

    private final List<Redemption> orders;
    private final OnOrderActionListener listener;

    public OrderAdapter(
            List<Redemption> orders,
            OnOrderActionListener listener
    ) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);

        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull OrderViewHolder holder,
            int position
    ) {
        Redemption order = orders.get(position);

        holder.tvTitle.setText(order.getItemTitle());
        holder.tvPrice.setText(
                (order.getPrice() != null ? order.getPrice() : 0) + " pts"
        );

        if (order.getRedeemedAt() != null) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            );
            holder.tvDate.setText(
                    format.format(order.getRedeemedAt().toDate())
            );
        } else {
            holder.tvDate.setText("");
        }

        if (order.isPending()) {
            holder.tvStatus.setText("Ready for pickup");
            holder.btnQr.setText("Show Pickup QR");
            holder.btnQr.setEnabled(true);
            holder.btnQr.setOnClickListener(v -> listener.onShowQr(order));
        } else {
            holder.tvStatus.setText("Collected");
            holder.btnQr.setText("Collected");
            holder.btnQr.setEnabled(false);
            holder.btnQr.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvPrice;
        final TextView tvStatus;
        final com.google.android.material.button.MaterialButton btnQr;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvOrderItemTitle);
            tvDate = itemView.findViewById(R.id.tvOrderItemDate);
            tvPrice = itemView.findViewById(R.id.tvOrderItemPrice);
            tvStatus = itemView.findViewById(R.id.tvOrderItemStatus);
            btnQr = itemView.findViewById(R.id.btnOrderItemQr);
        }
    }
}
