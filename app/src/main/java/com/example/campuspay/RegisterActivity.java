package com.example.campuspay;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import android.util.Patterns;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnCreateAccount;

    private com.google.android.material.textfield.TextInputLayout
            tilName, tilEmail, tilPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        tilName = findViewById(R.id.tilRegisterName);
        tilEmail = findViewById(R.id.tilRegisterEmail);
        tilPassword = findViewById(R.id.tilRegisterPassword);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarRegister);

        toolbar.setNavigationOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnCreateAccount.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            tilName.setError(null);
            tilEmail.setError(null);
            tilPassword.setError(null);

            // Name checks
            if (name.isEmpty()) {
                tilName.setError("Name is required");
                etName.requestFocus();
                return;
            }

            if (name.length() < 2) {
                tilName.setError("Name is too short");
                etName.requestFocus();
                return;
            }

            // Email checks
            if (email.isEmpty()) {
                tilEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Password checks
            if (password.isEmpty()) {
                tilPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                tilPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            String userId = mAuth.getCurrentUser().getUid();

                            Map<String, Object> user = new HashMap<>();
                            user.put("name", name);
                            user.put("email", email);
                            user.put("role", "student");
                            user.put("credits", 0);

                            db.collection("users")
                                    .document(userId)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {

                                        Toast.makeText(
                                                this,
                                                "Account created successfully",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        finish();
                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(
                                                this,
                                                "Profile creation failed: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });

                        } else {

                            Toast.makeText(
                                    this,
                                    "Registration failed: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });
    }
}