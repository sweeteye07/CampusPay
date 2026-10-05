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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vendor/staff window into past handovers: every order this counter
 * marked handed over, newest first, with buyer names resolved.
 * Students are redirected to My Orders instead.
 */
public class VendorOrdersActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private RecyclerView recyclerView;
    private View emptyContainer;

    private VendorOrderAdapter adapter;
    private final List<Redemption> orders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_orders);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarVendorOrders);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.vendorOrdersRecyclerView);
        emptyContainer = findViewById(R.id.layoutVendorOrdersEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new VendorOrderAdapter(orders);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        RoleGate.fetchRole(db, mAuth.getCurrentUser().getUid(), role -> {
            if (UserRole.isStudent(role)) {
                Toast.makeText(this, "Use My Orders to track your pickups",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            loadHistory();
        });
    }

    private void loadHistory() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("redemptions")
                .whereEqualTo("handedBy", userId)
                .orderBy("handedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Redemption> loaded = new ArrayList<>();
                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {
                        loaded.add(Redemption.fromDocument(document));
                    }
                    adapter.setOrders(loaded);
                    boolean empty = loaded.isEmpty();
                    emptyContainer.setVisibility(
                            empty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(
                            empty ? View.GONE : View.VISIBLE);
                    if (!empty) {
                        resolveBuyerNames(loaded);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load history: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /** Resolves buyer display names; rows render first, then upgrade. */
    private void resolveBuyerNames(List<Redemption> loaded) {
        Set<String> userIds = new HashSet<>();
        for (Redemption order : loaded) {
            if (order.getUserId() != null) {
                userIds.add(order.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        Map<String, String> names = new HashMap<>();
        java.util.concurrent.atomic.AtomicInteger remaining =
                new java.util.concurrent.atomic.AtomicInteger(userIds.size());
        for (String id : userIds) {
            db.collection("users")
                    .document(id)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            names.put(id, name);
                        }
                        if (remaining.decrementAndGet() == 0) {
                            adapter.setBuyerNames(names);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) {
                            adapter.setBuyerNames(names);
                        }
                    });
        }
    }
}
