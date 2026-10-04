package com.example.campuspay;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * System-notification helper. Creates 4 channels and posts notifications
 * for payments, credits, new events and reminders.
 */
public final class NotificationHelper {

    public static final String CHANNEL_PAYMENTS = "campuspay_payments";
    public static final String CHANNEL_CREDITS = "campuspay_credits";
    public static final String CHANNEL_EVENTS = "campuspay_events";
    public static final String CHANNEL_REMINDERS = "campuspay_reminders";

    private NotificationHelper() {
        // no instances
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        createChannel(manager, CHANNEL_PAYMENTS, "Payments");
        createChannel(manager, CHANNEL_CREDITS, "Credits");
        createChannel(manager, CHANNEL_EVENTS, "New events");
        createChannel(manager, CHANNEL_REMINDERS, "Event reminders");
    }

    private static void createChannel(
            NotificationManager manager,
            String id,
            String name
    ) {
        NotificationChannel channel = new NotificationChannel(
                id, name, NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);
    }

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void notifyPaymentReceived(
            Context context, long credits, String senderEmail) {
        String message = "Received " + credits + " credits"
                + (senderEmail != null ? " from " + senderEmail : "") + ".";
        post(context, CHANNEL_PAYMENTS, "Payment received", message,
                HistoryActivity.class, (int) (System.currentTimeMillis() % 100000));
    }

    public static void notifyCreditsEarned(
            Context context, long credits, String eventTitle) {
        String message = "Earned " + credits + " credits"
                + (eventTitle != null ? " for " + eventTitle : "") + ".";
        post(context, CHANNEL_CREDITS, "Credits earned", message,
                HistoryActivity.class, (int) (System.currentTimeMillis() % 100000) + 1);
    }

    public static void notifyNewEvent(
            Context context, String title, long credits) {
        String message = title + " (+" + credits + " pts). Open Campus Events to register.";
        post(context, CHANNEL_EVENTS, "New event added", message,
                EventsActivity.class, title.hashCode());
    }

    public static void notifyNewItem(
            Context context, String title, long price) {
        String message = title + " (" + price + " pts). Open Campus Marketplace to redeem.";
        post(context, CHANNEL_EVENTS, "New marketplace reward", message,
                MarketplaceActivity.class, ("item_" + title).hashCode());
    }

    public static void notifyEventReminder(
            Context context, String title, String date) {
        String message = title + " is on " + date + ". Don't miss it!";
        post(context, CHANNEL_REMINDERS, "Event reminder", message,
                MyEventsActivity.class, ("reminder_" + title).hashCode());
    }

    private static void post(
            Context context,
            String channelId,
            String title,
            String message,
            Class<?> target,
            int notificationId
    ) {
        ensureChannels(context);
        if (!canPost(context)) {
            return;
        }
        Intent intent = new Intent(context, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Permission revoked between check and post.
        }
    }
}
