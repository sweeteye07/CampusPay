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
import java.util.Map;

public class VendorOrderAdapter
        extends RecyclerView.Adapter<VendorOrderAdapter.VendorOrderViewHolder> {

    private final List<Redemption> orders;
    private Map<String, String> buyerNames;

    public VendorOrderAdapter(List<Redemption> orders) {
        this.orders = orders;
    }

    public void setOrders(List<Redemption> orders) {
        this.orders.clear();
        if (orders != null) {
            this.orders.addAll(orders);
        }
        notifyDataSetChanged();
    }

    public void setBuyerNames(Map<String, String> names) {
        this.buyerNames = names;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VendorOrderViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vendor_order, parent, false);
        return new VendorOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VendorOrderViewHolder holder,
            int position
    ) {
        Redemption order = orders.get(position);

        holder.tvTitle.setText(order.getItemTitle());
        holder.tvPrice.setText(
                (order.getPrice() != null ? order.getPrice() : 0) + " pts");

        String buyer = buyerNames != null
                ? buyerNames.get(order.getUserId()) : null;
        holder.tvBuyer.setText(buyer != null && !buyer.isEmpty()
                ? buyer : "Student");

        if (order.getRedeemedAt() != null) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault());
            holder.tvDate.setText(
                    format.format(order.getRedeemedAt().toDate()));
        } else {
            holder.tvDate.setText("");
        }

        holder.tvStatus.setText("Handed over");
        holder.tvStatus.setBackgroundResource(R.drawable.bg_pill_success);
        holder.tvStatus.setTextColor(0xFF2E7D32);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class VendorOrderViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvBuyer;
        final TextView tvDate;
        final TextView tvPrice;
        final TextView tvStatus;

        VendorOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvVendorOrderTitle);
            tvBuyer = itemView.findViewById(R.id.tvVendorOrderBuyer);
            tvDate = itemView.findViewById(R.id.tvVendorOrderDate);
            tvPrice = itemView.findViewById(R.id.tvVendorOrderPrice);
            tvStatus = itemView.findViewById(R.id.tvVendorOrderStatus);
        }
    }
}
