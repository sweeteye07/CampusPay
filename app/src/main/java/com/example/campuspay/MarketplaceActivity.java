package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketplaceActivity extends AppCompatActivity
        implements MarketAdapter.OnMarketActionListener {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private MarketAdapter adapter;

    private final List<MarketItem> items = new ArrayList<>();
    private final List<MarketItem> allItems = new ArrayList<>();

    private boolean isOrganizer = false;
    private boolean isStudent = true;
    private String currentUserId = null;
    private com.google.android.material.button.MaterialButton btnAddItem;
    private com.google.android.material.button.MaterialButton btnScanPickup;
    private com.google.android.material.button.MaterialButton btnMyOrders;
    private View layoutProviderActions;
    private android.widget.TextView tvEmpty;
    private androidx.recyclerview.widget.RecyclerView recyclerView;

    private static final String SORT_NEWEST = "Newest";
    private static final String SORT_PRICE_LOW = "Price: low to high";
    private static final String SORT_PRICE_HIGH = "Price: high to low";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarMarketplace);

        toolbar.setNavigationOnClickListener(v -> finish());

        btnAddItem = findViewById(R.id.btnMarketAddItem);
        btnScanPickup = findViewById(R.id.btnMarketScanPickup);
        btnMyOrders = findViewById(R.id.btnMarketMyOrders);
        layoutProviderActions =
                findViewById(R.id.layoutMarketplaceProviderActions);

        btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(this, CreateItemActivity.class)));

        findViewById(R.id.btnMarketMyOrders).setOnClickListener(v ->
                startActivity(new Intent(this, MyOrdersActivity.class)));

        btnScanPickup.setOnClickListener(v ->
                startActivity(new Intent(this, ScanPickupActivity.class)));

        RecyclerView recyclerView = findViewById(R.id.marketplaceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MarketAdapter(items, this);
        recyclerView.setAdapter(adapter);

        this.recyclerView = recyclerView;
        tvEmpty = findViewById(R.id.tvMarketplaceEmpty);

        setupSearchAndSort();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resolveRoleThenLoad();
    }

    /** Role check drives List/Scan buttons + per-row Edit/Delete visibility. */
    private void resolveRoleThenLoad() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.getString("role");
                    isOrganizer = UserRole.isOrganizer(role);
                    isStudent = UserRole.isStudent(role);
                    adapter.setAdminMode(isOrganizer, currentUserId);
                    adapter.setStudentView(isStudent);
                    adapter.setUnrestricted(UserRole.isAdmin(role));
                    // Students get My Orders; providers get the highlighted
                    // List Item + Scan Pickup quick actions instead.
                    btnMyOrders.setVisibility(
                            isStudent ? View.VISIBLE : View.GONE);
                    layoutProviderActions.setVisibility(
                            isOrganizer ? View.VISIBLE : View.GONE);
                    loadItems();
                })
                .addOnFailureListener(e -> {
                    // Fail closed: student view if role can't be verified.
                    isOrganizer = false;
                    isStudent = true;
                    adapter.setAdminMode(false, currentUserId);
                    adapter.setStudentView(true);
                    adapter.setUnrestricted(false);
                    btnMyOrders.setVisibility(View.VISIBLE);
                    layoutProviderActions.setVisibility(View.GONE);
                    loadItems();
                });
    }

    private void loadItems() {
        db.collection("market_items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allItems.clear();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        MarketItem item = MarketItem.fromDocument(document);
                        // Hidden items are only visible to their organizer.
                        if (!item.isActive()
                                && !canManageItem(item)) {
                            continue;
                        }
                        allItems.add(item);
                    }

                    applyFilter();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load marketplace: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void setupSearchAndSort() {
        com.google.android.material.textfield.TextInputEditText etSearch =
                findViewById(R.id.etMarketSearch);
        android.widget.AutoCompleteTextView actvSort =
                findViewById(R.id.actvMarketSort);

        actvSort.setAdapter(new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{SORT_NEWEST, SORT_PRICE_LOW, SORT_PRICE_HIGH}));
        actvSort.setText(SORT_NEWEST, false);
        actvSort.setOnItemClickListener((parent, view, position, id) ->
                applyFilter());

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                    int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                    int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    /** Client-side search + sort over the loaded items. */
    private void applyFilter() {
        com.google.android.material.textfield.TextInputEditText etSearch =
                findViewById(R.id.etMarketSearch);
        android.widget.AutoCompleteTextView actvSort =
                findViewById(R.id.actvMarketSort);

        String query = etSearch.getText() != null
                ? etSearch.getText().toString().trim().toLowerCase(
                        java.util.Locale.getDefault())
                : "";
        String sort = actvSort.getText() != null
                ? actvSort.getText().toString() : SORT_NEWEST;

        items.clear();
        for (MarketItem item : allItems) {
            if (!query.isEmpty()) {
                String title = item.getTitle() != null
                        ? item.getTitle().toLowerCase(
                                java.util.Locale.getDefault()) : "";
                String desc = item.getDescription() != null
                        ? item.getDescription().toLowerCase(
                                java.util.Locale.getDefault()) : "";
                if (!title.contains(query) && !desc.contains(query)) {
                    continue;
                }
            }
            items.add(item);
        }

        if (SORT_PRICE_LOW.equals(sort)) {
            java.util.Collections.sort(items, (a, b) ->
                    Long.compare(priceOf(a), priceOf(b)));
        } else if (SORT_PRICE_HIGH.equals(sort)) {
            java.util.Collections.sort(items, (a, b) ->
                    Long.compare(priceOf(b), priceOf(a)));
        }

        adapter.notifyDataSetChanged();

        boolean empty = items.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            tvEmpty.setText(!query.isEmpty() && !allItems.isEmpty()
                    ? "No rewards match your search."
                    : "No rewards listed yet.");
        }
    }

    private static long priceOf(MarketItem item) {
        return item.getPrice() != null ? item.getPrice() : 0L;
    }

    private boolean canManageItem(MarketItem item) {
        if (!isOrganizer) {
            return false;
        }
        String createdBy = item.getCreatedBy();
        return createdBy == null || createdBy.equals(currentUserId);
    }

    @Override
    public void onRedeem(MarketItem item) {
        // Redeeming is students-only.
        if (!isStudent) {
            Toast.makeText(this, "Redeeming is for students",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!item.isInStock()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Redeem reward?")
                .setMessage("Redeem \"" + item.getTitle() + "\" for "
                        + item.getPrice() + " credits?\n\n"
                        + "Show your pickup QR to an organizer to collect it.")
                .setPositiveButton("Redeem",
                        (dialog, which) -> redeemItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * One atomic transaction: debit buyer, hold one unit of stock,
     * create the pending pickup order, write the spend ledger entry.
     * The security rules verify all four legs match (same price).
     */
    private void redeemItem(MarketItem item) {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        DocumentReference userRef = db.collection("users").document(userId);
        DocumentReference itemRef =
                db.collection("market_items").document(item.getId());
        DocumentReference redemptionRef =
                db.collection("redemptions").document();
        DocumentReference ledgerRef =
                db.collection("transactions").document();

        db.runTransaction(transaction -> {
                    DocumentSnapshot userSnapshot = transaction.get(userRef);
                    DocumentSnapshot itemSnapshot = transaction.get(itemRef);

                    if (!userSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Profile not found",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    if (!itemSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Item no longer available",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    Boolean active = itemSnapshot.getBoolean("active");
                    Long price = itemSnapshot.getLong("price");
                    Long stock = itemSnapshot.getLong("stock");
                    String title = itemSnapshot.getString("title");

                    if (active != null && !active) {
                        throw new FirebaseFirestoreException(
                                "Item is no longer listed",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }
                    if (price == null || price <= 0) {
                        throw new FirebaseFirestoreException(
                                "Item price not configured",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }
                    if (stock == null || stock <= 0) {
                        throw new FirebaseFirestoreException(
                                "Item is sold out",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }

                    Long balance = userSnapshot.getLong("credits");
                    long current = balance != null ? balance : 0L;
                    if (current < price) {
                        throw new FirebaseFirestoreException(
                                "Insufficient balance. You have "
                                        + current + " credits.",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }

                    transaction.update(userRef, "credits",
                            FieldValue.increment(-price));
                    transaction.update(itemRef, "stock",
                            FieldValue.increment(-1));

                    Map<String, Object> redemption = new HashMap<>();
                    redemption.put("userId", userId);
                    redemption.put("itemId", item.getId());
                    redemption.put("itemTitle", title);
                    redemption.put("price", price);
                    redemption.put("status", Redemption.STATUS_PENDING);
                    redemption.put("redeemedAt", FieldValue.serverTimestamp());
                    transaction.set(redemptionRef, redemption);

                    Map<String, Object> ledger = new HashMap<>();
                    ledger.put("type", "spend");
                    ledger.put("direction", "out");
                    ledger.put("userId", userId);
                    ledger.put("itemId", item.getId());
                    ledger.put("itemTitle", title);
                    ledger.put("credits", price);
                    ledger.put("description",
                            "Redeemed " + title + " for " + price + " credits");
                    ledger.put("createdAt", FieldValue.serverTimestamp());
                    transaction.set(ledgerRef, ledger);

                    return redemptionRef.getId();
                })
                .addOnSuccessListener(redemptionId -> {
                    loadItems();
                    Toast.makeText(this,
                            "Redeemed! Show your pickup QR to collect.",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, OrderQRActivity.class);
                    intent.putExtra(OrderQRActivity.EXTRA_REDEMPTION_ID,
                            redemptionId);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Redeem failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public void onEdit(MarketItem item) {
        Intent intent = new Intent(this, CreateItemActivity.class);
        intent.putExtra(CreateItemActivity.EXTRA_ITEM_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(MarketItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete item?")
                .setMessage("Remove \"" + item.getTitle() + "\" from the marketplace?\n\n"
                        + "Past pickup orders are kept for records.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(MarketItem item) {
        db.collection("market_items")
                .document(item.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    allItems.remove(item);
                    applyFilter();
                    Toast.makeText(this, "Item deleted",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
