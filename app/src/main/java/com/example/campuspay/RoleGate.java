package com.example.campuspay;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * One-shot role fetch for entry-point gating. A missing profile or a
 * failed read resolves to null, which UserRole treats as a student —
 * the same fail-open convention used by the list screens.
 */
public final class RoleGate {

    public interface RoleCallback {
        void onRole(String role);
    }

    private RoleGate() {
        // no instances
    }

    public static void fetchRole(
            FirebaseFirestore db,
            String userId,
            RoleCallback callback
    ) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                        doc -> callback.onRole(doc.getString("role")))
                .addOnFailureListener(e -> callback.onRole(null));
    }
}
