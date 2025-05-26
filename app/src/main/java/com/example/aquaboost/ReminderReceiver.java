
package com.example.aquaboost;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * BroadcastReceiver that handles scheduled hydration reminders.
 * This class is triggered by AlarmManager to show water drinking notifications.
 */
public class ReminderReceiver extends BroadcastReceiver {

    // Constants for better maintainability
    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Hydration Reminders";
    private static final int NOTIFICATION_ID = 1001;

    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     * This method runs on the main thread and should complete quickly.
     *
     * @param context The Context in which the receiver is running
     * @param intent The Intent being received
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the system's notification service
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8.0+ (API level 26+)
        // This is required for notifications to work on newer Android versions
        createNotificationChannel(manager);

        // Build and display the notification
        showHydrationNotification(context, manager);
    }

    /**
     * Creates a notification channel for Android O and above.
     * Channels allow users to control notification settings per category.
     *
     * @param manager The NotificationManager instance
     */
    private void createNotificationChannel(NotificationManager manager) {
        // Only create channel for Android 8.0 (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // High importance shows heads-up notification
            );

            // Optional: Customize channel behavior
            channel.setDescription("Reminders to drink water throughout the day");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Creates and displays the hydration reminder notification.
     *
     * @param context The application context
     * @param manager The NotificationManager instance
     */
    private void showHydrationNotification(Context context, NotificationManager manager) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_water)
                .setContentTitle("Hydration Reminder")
                .setContentText("Time to drink some water! 💧")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Notification dismisses when tapped
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Sound, vibration, lights

        // Display the notification with a unique ID
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}