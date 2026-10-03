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
        void onShowQR(Event event);
    }

    private final List<Event> events;
    private final OnEventActionListener listener;

    public EventAdapter(
            List<Event> events,
            OnEventActionListener listener
    ) {
        this.events = events;
        this.listener = listener;
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

        holder.btnShowQR.setOnClickListener(
                v -> listener.onShowQR(event)
        );
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
        final com.google.android.material.button.MaterialButton btnShowQR;

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
            btnShowQR = itemView.findViewById(R.id.btnEventItemShowQR);
        }
    }
}
