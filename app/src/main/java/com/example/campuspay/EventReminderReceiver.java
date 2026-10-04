package com.example.campuspay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Fired by the daily AlarmManager alarm and on device boot.
 * Delegates to EventReminderScheduler for the actual Firestore scan.
 */
public class EventReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        EventReminderScheduler.checkRemindersNow(context);
        // Re-schedule in case the alarm was cleared on reboot.
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            EventReminderScheduler.scheduleDaily(context);
        }
    }
}
