package com.example.campuspay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter
        extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventActionListener {
        void onRegister(Event event);
        void onEdit(Event event);
        void onDelete(Event event);
    }

    private final List<Event> events;
    private final OnEventActionListener listener;
    private boolean adminMode = false;
    private String currentUserId = null;

    public EventAdapter(
            List<Event> events,
            OnEventActionListener listener
    ) {
        this.events = events;
        this.listener = listener;
    }

    /** Called by EventsActivity once the user role is known. */
    public void setAdminMode(boolean adminMode, String currentUserId) {
        this.adminMode = adminMode;
        this.currentUserId = currentUserId;
    }

    /** Same ownership rule as ScanQRActivity + rules: own events + owner-less. */
    private boolean canManage(Event event) {
        if (!adminMode) {
            return false;
        }
        String createdBy = event.getCreatedBy();
        return createdBy == null || createdBy.equals(currentUserId);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);

        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EventViewHolder holder,
            int position
    ) {
        Event event = events.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvDescription.setText(event.getDescription());
        holder.tvCredits.setText(
                "+" + event.getCredits() + " pts"
        );
        holder.tvDate.setText(event.getDate());

        if (event.isRegistered()) {
            holder.btnRegister.setText("Registered");
            holder.btnRegister.setEnabled(false);
        } else {
            holder.btnRegister.setText("Register");
            holder.btnRegister.setEnabled(true);
        }

        holder.btnRegister.setOnClickListener(
                v -> listener.onRegister(event)
        );

        boolean manageable = canManage(event);
        holder.layoutAdminActions.setVisibility(
                manageable ? View.VISIBLE : View.GONE
        );
        if (manageable) {
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(event));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(event));
        } else {
            holder.btnEdit.setOnClickListener(null);
            holder.btnDelete.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvDescription;
        final TextView tvCredits;
        final TextView tvDate;
        final com.google.android.material.button.MaterialButton btnRegister;
        final View layoutAdminActions;
        final com.google.android.material.button.MaterialButton btnEdit;
        final com.google.android.material.button.MaterialButton btnDelete;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvEventItemTitle);
            tvDescription = itemView.findViewById(
                    R.id.tvEventItemDescription
            );
            tvCredits = itemView.findViewById(R.id.tvEventItemCredits);
            tvDate = itemView.findViewById(R.id.tvEventItemDate);
            btnRegister = itemView.findViewById(
                    R.id.btnEventItemRegister
            );
            layoutAdminActions = itemView.findViewById(
                    R.id.layoutEventAdminActions
            );
            btnEdit = itemView.findViewById(R.id.btnEventItemEdit);
            btnDelete = itemView.findViewById(R.id.btnEventItemDelete);
        }
    }
}
