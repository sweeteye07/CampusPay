package com.example.campuspay;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ReceiveMoneyActivity extends AppCompatActivity {

    private TextView tvMyEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_money);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarReceiveMoney);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvMyEmail = findViewById(R.id.tvMyEmail);
        MaterialButton btnCopyEmail = findViewById(R.id.btnCopyEmail);

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                        .toLowerCase()
                : "";

        if (email.isEmpty()) {
            tvMyEmail.setText("Email unavailable — please log in again");
            btnCopyEmail.setEnabled(false);
        } else {
            tvMyEmail.setText(email);
        }

        btnCopyEmail.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("email", tvMyEmail.getText()));
            Toast.makeText(this, "Email copied", Toast.LENGTH_SHORT).show();
        });
    }
}
