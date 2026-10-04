package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
            return;
}

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();

            // Passwords are used exactly as typed: trimming them would
            // silently change the credential the account was created with
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(
                                    this,
                                    "Login failed: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        Button btnForgotPassword = findViewById(R.id.btnForgotPassword);

        btnForgotPassword.setOnClickListener(v -> promptPasswordReset());
    }

    // ------------------------------------------------------------------
    // Password reset
    // ------------------------------------------------------------------
    private void promptPasswordReset() {

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Email");

        FrameLayout wrapper = new FrameLayout(this);
        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (20 * density);
        wrapper.setPadding(padding, 0, padding, 0);
        wrapper.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Reset password")
                .setMessage("Enter your account email and we will send a reset link.")
                .setView(wrapper)
                .setCancelable(true)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send link",
                        (dialog, which) -> sendPasswordReset(input))
                .show();
    }

    private void sendPasswordReset(EditText input) {

        String email = input.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {

                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "Reset link sent to " + email,
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                this,
                                "Could not send reset link: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}