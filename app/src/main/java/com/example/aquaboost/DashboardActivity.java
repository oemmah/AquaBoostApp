package com.example.aquaboost;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DashboardActivity extends AppCompatActivity {

    private MaterialCardView hydrationCard;
    private Vibrator vibrator;
    private Ringtone notificationSound;
    private FirebaseAnalytics firebaseAnalytics;

    private static final long POPUP_INITIAL_DELAY = 2000;
    private static final long POPUP_DISPLAY_DURATION = 4000;
    private static final long POPUP_REAPPEAR_DELAY = 2700000;
    private static final long[] VIBRATION_PATTERN = {0, 500, 200, 500};

    private final Handler handler = new Handler();

    // TensorFlow Lite Interpreter
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize system services
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize notification sound
        try {
            Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationSound = RingtoneManager.getRingtone(this, notificationUri);
        } catch (Exception e) {
            logAnalyticsEvent("sound_error", "Failed to load notification sound");
        }

        hydrationCard = findViewById(R.id.hydrationCard);

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile());
            logAnalyticsEvent("model_loaded", "TFLite model loaded successfully");
        } catch (IOException e) {
            logAnalyticsEvent("model_load_error", "Failed to load TFLite model: " + e.getMessage());
        }

        // Start core functionality
        showHydrationPopup();
        setupNavigation();
        setupLogoutButton();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void runModel(float[] input) {
        if (tflite == null) {
            logAnalyticsEvent("inference_error", "TFLite interpreter is not initialized");
            return;
        }

        float[][] output = new float[1][1]; // Adjust size based on model output
        try {
            tflite.run(input, output);
            logAnalyticsEvent("model_inference", "Model output: " + output[0][0]);
        } catch (Exception e) {
            logAnalyticsEvent("inference_error", "Error running model: " + e.getMessage());
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, destination.getLabel().toString());
                bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "DashboardActivity");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
            });
        }
    }

    private void setupLogoutButton() {
        FloatingActionButton fabLogout = findViewById(R.id.fabLogout);
        View fabMenuLayout = findViewById(R.id.fabMenuLayout);
        View logoutAction = findViewById(R.id.logoutAction);

        if (logoutAction != null) {
            logoutAction.setOnClickListener(v -> {
                if (fabMenuLayout != null) fabMenuLayout.setVisibility(View.GONE);
                showLogoutConfirmationDialog();
            });
        }

        if (fabLogout != null && fabMenuLayout != null) {
            fabLogout.setOnClickListener(v -> {
                if (fabMenuLayout.getVisibility() == View.VISIBLE) {
                    fabMenuLayout.setVisibility(View.GONE);
                } else {
                    fabMenuLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    logAnalyticsEvent("logout_confirmed", "User confirmed logout");
                    logoutUser();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    logAnalyticsEvent("logout_cancelled", "User cancelled logout");
                    dialog.dismiss();
                })
                .setIcon(R.drawable.ic_logout)
                .show();
    }

    private void logoutUser() {
        logAnalyticsEvent("logout_success", "User logged out successfully");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showHydrationPopup() {
        handler.postDelayed(() -> {
            logAnalyticsEvent("hydration_reminder_shown", "Hydration reminder displayed");

            if (notificationSound != null) {
                try {
                    notificationSound.play();
                } catch (Exception e) {
                    logAnalyticsEvent("sound_error", "Failed to play notification sound");
                }
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1));
                    } else {
                        vibrator.vibrate(VIBRATION_PATTERN, -1);
                    }
                } catch (Exception e) {
                    logAnalyticsEvent("vibration_error", "Failed to vibrate");
                }
            }

            hydrationCard.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(500);
            hydrationCard.startAnimation(fadeIn);

            handler.postDelayed(() -> {
                AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
                fadeOut.setDuration(500);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {
                        hydrationCard.setVisibility(View.GONE);
                        logAnalyticsEvent("hydration_reminder_dismissed", "Reminder hidden");
                        handler.postDelayed(DashboardActivity.this::showHydrationPopup, POPUP_REAPPEAR_DELAY);
                    }
                });
                hydrationCard.startAnimation(fadeOut);
            }, POPUP_DISPLAY_DURATION);
        }, POPUP_INITIAL_DELAY);
    }

    private void logAnalyticsEvent(String eventName, String eventDescription) {
        Bundle params = new Bundle();
        params.putString("event_name", eventName);
        params.putString("event_description", eventDescription);
        params.putLong("timestamp", System.currentTimeMillis());
        firebaseAnalytics.logEvent(eventName, params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notificationSound != null && notificationSound.isPlaying()) {
            notificationSound.stop();
        }

        handler.removeCallbacksAndMessages(null);

        if (tflite != null) {
            tflite.close();
        }
    }
}
