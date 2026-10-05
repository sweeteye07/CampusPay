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

        adapter = new HistoryAdapter(items, this::openDetail);
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
                    resolveCounterpartyNames();
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

    /**
     * Looks up display names for transfer counterparties (the ledger
     * only stores emails). One capped query per unique email; the list
     * renders immediately with email prefixes and upgrades to names.
     */
    private void resolveCounterpartyNames() {
        java.util.Set<String> emails = new java.util.HashSet<>();
        for (CreditTransaction tx : items) {
            if ((tx.isSend() || tx.isReceive())
                    && tx.getCounterpartyEmail() != null
                    && !tx.getCounterpartyEmail().isEmpty()) {
                emails.add(tx.getCounterpartyEmail());
            }
        }
        if (emails.isEmpty()) {
            return;
        }
        java.util.Map<String, String> names = new java.util.HashMap<>();
        java.util.concurrent.atomic.AtomicInteger remaining =
                new java.util.concurrent.atomic.AtomicInteger(emails.size());
        for (String email : emails) {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            String name = snap.getDocuments().get(0)
                                    .getString("name");
                            if (name != null && !name.isEmpty()) {
                                names.put(email, name);
                            }
                        }
                        if (remaining.decrementAndGet() == 0) {
                            adapter.setCounterpartyNames(names);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) {
                            adapter.setCounterpartyNames(names);
                        }
                    });
        }
    }

    /** Opens the receipt-style detail screen for one transaction. */
    private void openDetail(CreditTransaction tx) {
        android.content.Intent intent = new android.content.Intent(
                this, TransactionDetailActivity.class);
        intent.putExtra(TransactionDetailActivity.EXTRA_TX_ID, tx.getId());
        intent.putExtra(TransactionDetailActivity.EXTRA_TYPE, tx.getType());
        intent.putExtra(TransactionDetailActivity.EXTRA_DIRECTION,
                tx.getDirection());
        intent.putExtra(TransactionDetailActivity.EXTRA_CREDITS,
                tx.getCredits() != null ? tx.getCredits() : 0L);
        intent.putExtra(TransactionDetailActivity.EXTRA_DESCRIPTION,
                tx.getDescription());
        String title = tx.getEventTitle() != null ? tx.getEventTitle()
                : tx.getItemTitle();
        intent.putExtra(TransactionDetailActivity.EXTRA_TITLE, title);
        intent.putExtra(TransactionDetailActivity.EXTRA_COUNTERPARTY_EMAIL,
                tx.getCounterpartyEmail());
        if (tx.getCounterpartyEmail() != null) {
            intent.putExtra(TransactionDetailActivity.EXTRA_COUNTERPARTY_NAME,
                    adapter.displayName(tx));
        }
        intent.putExtra(TransactionDetailActivity.EXTRA_TIMESTAMP,
                tx.getCreatedAt() != null
                        ? tx.getCreatedAt().toDate().getTime() : -1L);
        startActivity(intent);
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
