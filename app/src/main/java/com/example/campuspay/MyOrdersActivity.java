package com.example.campuspay;

import android.content.Intent;
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

public class MyOrdersActivity extends AppCompatActivity
        implements OrderAdapter.OnOrderActionListener {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    private OrderAdapter adapter;

    private final List<Redemption> orders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarMyOrders);

        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.myOrdersRecyclerView);
        tvEmpty = findViewById(R.id.tvMyOrdersEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrderAdapter(orders, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("redemptions")
                .whereEqualTo("userId", userId)
                .orderBy("redeemedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    orders.clear();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        orders.add(Redemption.fromDocument(document));
                    }

                    if (orders.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load orders: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    @Override
    public void onShowQr(Redemption redemption) {
        Intent intent = new Intent(this, OrderQRActivity.class);
        intent.putExtra(OrderQRActivity.EXTRA_REDEMPTION_ID,
                redemption.getId());
        startActivity(intent);
    }
}
