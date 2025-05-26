package com.example.aquaboost;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    // UI Components
    private TextInputEditText nameEditText, emailEditText, passwordEditText;
    private MaterialButton signupButton;

    // Firebase Components
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestoreDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance();
        firestoreDb = FirebaseFirestore.getInstance();

        // Bind UI components
        initializeViews();

        // Set click listener for signup button
        signupButton.setOnClickListener(v -> registerUser());
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.signupButton);
    }

    private void registerUser() {
        // Get input values
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(name, email, password)) {
            return;
        }

        // Show loading indicator (consider adding this)
        signupButton.setEnabled(false);

        // Create user with email and password
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), name, email);
                        }
                    } else {
                        // Signup failed
                        handleSignupFailure(task.getException());
                    }
                });
    }

    private boolean validateInputs(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("createdAt", FieldValue.serverTimestamp()); // Add timestamp

        // Consider adding more fields like profile image URL, etc.

        firestoreDb.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                })
                .addOnFailureListener(e -> {
                    Log.e("SignupActivity", "Failed to save user info", e);
                    Toast.makeText(this,
                            "Account created but failed to save additional info",
                            Toast.LENGTH_LONG).show();
                    navigateToDashboard(); // Still navigate as auth succeeded
                });
    }

    private void handleSignupFailure(Exception exception) {
        Log.e("SignupActivity", "Signup failed", exception);

        String errorMessage = "Signup failed";
        if (exception != null) {
            if (exception.getMessage().contains("email address is already in use")) {
                errorMessage = "Email already in use";
            } else if (exception.getMessage().contains("badly formatted")) {
                errorMessage = "Invalid email format";
            }
            // Add more specific error cases as needed
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        signupButton.setEnabled(true); // Re-enable button if you disabled it
    }

    private void navigateToDashboard() {
        startActivity(new Intent(SignupActivity.this, DashboardActivity.class));
        finish(); // Prevent returning to signup with back button
    }
}