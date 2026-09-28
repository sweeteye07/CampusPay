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

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnCreateAccount;

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnCreateAccount.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                ).show();
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