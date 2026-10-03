package com.example.campuspay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyEventAdapter
        extends RecyclerView.Adapter<MyEventAdapter.MyEventViewHolder> {

    private final List<MyEvent> items;

    public MyEventAdapter(List<MyEvent> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public MyEventViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_event, parent, false);

        return new MyEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MyEventViewHolder holder,
            int position
    ) {
        MyEvent item = items.get(position);

        holder.tvTitle.setText(item.getTitle());

        holder.tvDate.setText(
                item.getDate() != null ? item.getDate() : ""
        );

        holder.tvCredits.setText(
                "+" + item.getCredits() + " pts"
        );

        if (item.isAttended()) {
            holder.tvStatus.setText(
                    "Attended • +" + item.getCredits() + " earned"
            );
        } else {
            holder.tvStatus.setText("Registered — scan QR at venue");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MyEventViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvCredits;
        final TextView tvStatus;

        MyEventViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvMyEventTitle);
            tvDate = itemView.findViewById(R.id.tvMyEventDate);
            tvCredits = itemView.findViewById(R.id.tvMyEventCredits);
            tvStatus = itemView.findViewById(R.id.tvMyEventStatus);
        }
    }
}
