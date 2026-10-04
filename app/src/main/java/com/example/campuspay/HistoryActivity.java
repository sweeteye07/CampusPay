package com.example.campuspay;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private RecyclerView recyclerView;
    private View emptyContainer;
    private TextView tvEmpty;

    private TextView tvTotalIn;
    private TextView tvTotalOut;
    private TextView tvNet;

    private HistoryAdapter adapter;

    private final List<CreditTransaction> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarHistory);

        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.historyRecyclerView);
        emptyContainer = findViewById(R.id.layoutHistoryEmpty);
        tvEmpty = findViewById(R.id.tvHistoryEmpty);

        tvTotalIn = findViewById(R.id.tvHistoryTotalIn);
        tvTotalOut = findViewById(R.id.tvHistoryTotalOut);
        tvNet = findViewById(R.id.tvHistoryNet);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistoryAdapter(items);
        recyclerView.setAdapter(adapter);

        com.google.android.material.chip.ChipGroup chipGroup =
                findViewById(R.id.chipGroupHistoryFilter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFilterEarned)) {
                adapter.setFilter(HistoryAdapter.FILTER_EARNED);
            } else if (checkedIds.contains(R.id.chipFilterReceived)) {
                adapter.setFilter(HistoryAdapter.FILTER_RECEIVED);
            } else if (checkedIds.contains(R.id.chipFilterSent)) {
                adapter.setFilter(HistoryAdapter.FILTER_SENT);
            } else if (checkedIds.contains(R.id.chipFilterSpent)) {
                adapter.setFilter(HistoryAdapter.FILTER_SPENT);
            } else {
                adapter.setFilter(HistoryAdapter.FILTER_ALL);
            }
            updateEmptyState();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in again",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    items.clear();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {
                        items.add(
                                CreditTransaction.fromDocument(document)
                        );
                    }

                    adapter.setItems(items);
                    renderSummary();
                    updateEmptyState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load history: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /** Earned vs spent totals across the full ledger. */
    private void renderSummary() {
        long earned = 0;
        long spent = 0;

        for (CreditTransaction tx : items) {
            long credits = tx.getCredits() != null ? tx.getCredits() : 0L;
            if (tx.isEarn() || tx.isReceive()) {
                earned += credits;
            } else if (tx.isOutgoing()) {
                spent += credits;
            }
        }

        tvTotalIn.setText("+" + earned);
        tvTotalOut.setText("-" + spent);
        tvNet.setText(String.valueOf(earned - spent));
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty && !items.isEmpty()) {
            tvEmpty.setText("Nothing matches this filter.");
        } else {
            tvEmpty.setText(
                    "Attend an event to earn your first credits.");
        }
    }
}
