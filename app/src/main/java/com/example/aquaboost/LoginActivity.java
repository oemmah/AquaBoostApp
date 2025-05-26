package com.example.aquaboost;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginActivity handles user authentication and initial app setup.
 * Key responsibilities:
 * - Firebase email/password authentication
 * - Automatic redirection for authenticated users
 * - Hydration reminder scheduling
 * - Notification permission handling (Android 13+)
 */
public class LoginActivity extends AppCompatActivity {

    // UI Components
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton, signupButton;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Bind UI components
        initializeViews();

        // Set up system services and permissions
        initializeSystemServices();
    }

    /**
     * Initializes all view components and sets up their click listeners
     */
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);

        // Set up button click listeners
        loginButton.setOnClickListener(v -> loginUser());
        signupButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }

    /**
     * Initializes system-level services and checks permissions
     */
    private void initializeSystemServices() {
        scheduleHydrationReminder();
        requestNotificationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkExistingAuthentication();
    }

    /**
     * Checks if user is already authenticated and redirects to Dashboard if true
     */
    private void checkExistingAuthentication() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            redirectToDashboard();
        }
    }

    /**
     * Handles the user login process with email and password
     * Includes validation and Firebase authentication
     */
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    showToast("Login successful");
                    redirectToDashboard();
                })
                .addOnFailureListener(e -> {
                    showToast("Login failed: " + e.getMessage());
                });
    }

    /**
     * Validates email and password inputs
     * @return true if inputs are valid, false otherwise
     */
    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please fill all fields");
            return false;
        }
        // Add additional validation if needed (email format, password strength)
        return true;
    }

    /**
     * Schedules repeating hydration reminders every 3 hours
     * Uses AlarmManager with RTC_WAKEUP to trigger even in doze mode
     */
    private void scheduleHydrationReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        long intervalMillis = 3 * 60 * 60 * 1000; // 3 hours in milliseconds
        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

        // Set repeating alarm (note: inexact on newer Android versions)
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
        );
    }

    /**
     * Requests notification permission for Android 13+ (Tiramisu)
     * Required for showing hydration reminder notifications
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    /**
     * Helper method to redirect to DashboardActivity
     */
    private void redirectToDashboard() {
        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        finish(); // Prevent returning to login with back button
    }

    /**
     * Helper method to show Toast messages
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}