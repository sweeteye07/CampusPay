package com.example.campuspay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MarketAdapter
        extends RecyclerView.Adapter<MarketAdapter.MarketViewHolder> {

    public interface OnMarketActionListener {
        void onRedeem(MarketItem item);
        void onEdit(MarketItem item);
        void onDelete(MarketItem item);
    }

    private final List<MarketItem> items;
    private final OnMarketActionListener listener;
    private boolean adminMode = false;
    private boolean studentView = true;
    private boolean unrestricted = false;
    private String currentUserId = null;

    public MarketAdapter(
            List<MarketItem> items,
            OnMarketActionListener listener
    ) {
        this.items = items;
        this.listener = listener;
    }

    /** Called by MarketplaceActivity once the user role is known. */
    public void setAdminMode(boolean adminMode, String currentUserId) {
        this.adminMode = adminMode;
        this.currentUserId = currentUserId;
    }

    /** Redeem buttons only render for students. */
    public void setStudentView(boolean studentView) {
        this.studentView = studentView;
    }

    /** Admins manage every row regardless of ownership. */
    public void setUnrestricted(boolean unrestricted) {
        this.unrestricted = unrestricted;
    }

    /** Same ownership rule as events: own items + owner-less. */
    private boolean canManage(MarketItem item) {
        if (unrestricted) {
            return true;
        }
        if (!adminMode) {
            return false;
        }
        String createdBy = item.getCreatedBy();
        return createdBy == null || createdBy.equals(currentUserId);
    }

    @NonNull
    @Override
    public MarketViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_market, parent, false);

        return new MarketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MarketViewHolder holder,
            int position
    ) {
        MarketItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.tvPrice.setText(
                (item.getPrice() != null ? item.getPrice() : 0) + " pts"
        );

        long stock = item.getStock() != null ? item.getStock() : 0;
        if (!item.isActive()) {
            holder.tvStock.setText("Hidden");
            holder.tvStock.setTextColor(0xFF9E9E9E);
        } else if (stock <= 0) {
            holder.tvStock.setText("Sold out");
            holder.tvStock.setTextColor(0xFFC62828);
        } else if (stock <= 5) {
            holder.tvStock.setText("Only " + stock + " left!");
            holder.tvStock.setTextColor(0xFFE65100);
        } else {
            holder.tvStock.setText(stock + " left");
            holder.tvStock.setTextColor(0xFF2E7D32);
        }

        if (!item.isInStock()) {
            holder.btnRedeem.setText("Unavailable");
            holder.btnRedeem.setEnabled(false);
        } else {
            holder.btnRedeem.setText("Redeem");
            holder.btnRedeem.setEnabled(true);
        }

        // Redeeming is students-only: organizers manage instead.
        holder.btnRedeem.setVisibility(
                studentView && !adminMode ? View.VISIBLE : View.GONE);
        if (studentView && !adminMode) {
            holder.btnRedeem.setOnClickListener(
                    v -> listener.onRedeem(item)
            );
        } else {
            holder.btnRedeem.setOnClickListener(null);
        }

        boolean manageable = canManage(item);
        holder.layoutAdminActions.setVisibility(
                manageable ? View.VISIBLE : View.GONE
        );
        if (manageable) {
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
        } else {
            holder.btnEdit.setOnClickListener(null);
            holder.btnDelete.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MarketViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvDescription;
        final TextView tvPrice;
        final TextView tvStock;
        final com.google.android.material.button.MaterialButton btnRedeem;
        final View layoutAdminActions;
        final com.google.android.material.button.MaterialButton btnEdit;
        final com.google.android.material.button.MaterialButton btnDelete;

        MarketViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvMarketItemTitle);
            tvDescription = itemView.findViewById(
                    R.id.tvMarketItemDescription
            );
            tvPrice = itemView.findViewById(R.id.tvMarketItemPrice);
            tvStock = itemView.findViewById(R.id.tvMarketItemStock);
            btnRedeem = itemView.findViewById(
                    R.id.btnMarketItemRedeem
            );
            layoutAdminActions = itemView.findViewById(
                    R.id.layoutMarketAdminActions
            );
            btnEdit = itemView.findViewById(R.id.btnMarketItemEdit);
            btnDelete = itemView.findViewById(R.id.btnMarketItemDelete);
        }
    }
}
