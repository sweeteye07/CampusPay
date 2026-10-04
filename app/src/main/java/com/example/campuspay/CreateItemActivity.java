package com.example.campuspay;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateItemActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "itemId";

    private TextInputLayout tilTitle;
    private TextInputLayout tilDescription;
    private TextInputLayout tilPrice;
    private TextInputLayout tilStock;

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etPrice;
    private TextInputEditText etStock;

    private SwitchMaterial switchActive;

    private MaterialButton btnPublish;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String editItemId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarCreateItem);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilTitle = findViewById(R.id.tilItemTitle);
        tilDescription = findViewById(R.id.tilItemDescription);
        tilPrice = findViewById(R.id.tilItemPrice);
        tilStock = findViewById(R.id.tilItemStock);

        etTitle = findViewById(R.id.etItemTitle);
        etDescription = findViewById(R.id.etItemDescription);
        etPrice = findViewById(R.id.etItemPrice);
        etStock = findViewById(R.id.etItemStock);

        switchActive = findViewById(R.id.switchItemActive);

        btnPublish = findViewById(R.id.btnPublishItem);

        editItemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        boolean isEdit = editItemId != null && !editItemId.isEmpty();

        if (isEdit) {
            toolbar.setTitle("Edit Item");
            btnPublish.setText("Save Changes");
            btnPublish.setEnabled(false);
            loadItemForEdit(editItemId);
        }

        btnPublish.setOnClickListener(v -> publishItem());
    }

    /** Pre-fill the form when opened from the marketplace. */
    private void loadItemForEdit(String itemId) {
        db.collection("market_items")
                .document(itemId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Item not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (etTitle.getText() == null) {
                        return;
                    }
                    etTitle.setText(doc.getString("title"));
                    etDescription.setText(doc.getString("description"));
                    Long price = doc.getLong("price");
                    etPrice.setText(price != null ? String.valueOf(price) : "");
                    Long stock = doc.getLong("stock");
                    etStock.setText(stock != null ? String.valueOf(stock) : "");
                    Boolean active = doc.getBoolean("active");
                    switchActive.setChecked(active == null || active);
                    btnPublish.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load item: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void publishItem() {
        String title = etTitle.getText() != null
                ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";
        String priceRaw = etPrice.getText() != null
                ? etPrice.getText().toString().trim() : "";
        String stockRaw = etStock.getText() != null
                ? etStock.getText().toString().trim() : "";
        boolean active = switchActive.isChecked();

        tilTitle.setError(null);
        tilDescription.setError(null);
        tilPrice.setError(null);
        tilStock.setError(null);

        if (title.isEmpty()) {
            tilTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (title.length() > 100) {
            tilTitle.setError("Title cannot exceed 100 characters");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            tilDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        long price;
        try {
            price = Long.parseLong(priceRaw);
        } catch (NumberFormatException e) {
            tilPrice.setError("Enter a valid number");
            etPrice.requestFocus();
            return;
        }

        if (price <= 0) {
            tilPrice.setError("Price must be greater than 0");
            etPrice.requestFocus();
            return;
        }

        // The Firestore rules cap item prices at 10000 credits
        if (price > 10000) {
            tilPrice.setError("Price cannot be more than 10000");
            etPrice.requestFocus();
            return;
        }

        long stock;
        try {
            stock = Long.parseLong(stockRaw);
        } catch (NumberFormatException e) {
            tilStock.setError("Enter a valid number");
            etStock.requestFocus();
            return;
        }

        if (stock < 0 || stock > 1000) {
            tilStock.setError("Stock must be between 0 and 1000");
            etStock.requestFocus();
            return;
        }

        boolean isEdit = editItemId != null && !editItemId.isEmpty();
        if (isEdit) {
            updateItem(title, description, price, stock, active);
            return;
        }

        String creatorId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;

        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("price", price);
        item.put("stock", stock);
        item.put("active", active);
        item.put("createdBy", creatorId);
        item.put("createdAt", FieldValue.serverTimestamp());

        btnPublish.setEnabled(false);

        db.collection("market_items")
                .add(item)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(
                            this,
                            "Item listed",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Publish failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /** Update path for Marketplace -> Edit. Keeps createdBy/createdAt intact. */
    private void updateItem(String title, String description, long price,
            long stock, boolean active) {
        btnPublish.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("stock", stock);
        updates.put("active", active);

        db.collection("market_items")
                .document(editItemId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Item updated",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
