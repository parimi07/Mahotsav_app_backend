package com.eventadmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class MilestoneNotificationManager {
    
    private static final String CHANNEL_ID = "milestone_celebrations";
    private static final String CHANNEL_NAME = "Milestone Celebrations";
    private static final int NOTIFICATION_ID = 1000;
    private static final String PREFS_NAME = "milestone_prefs";
    private static final String LAST_MILESTONE_KEY = "last_milestone";
    
    public static void checkAndNotifyMilestone(Context context, int totalRegistrations) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastCount = prefs.getInt(LAST_MILESTONE_KEY, 0);
        
        // TESTING MODE: Notify on EVERY registration increment
        if (totalRegistrations > lastCount) {
            // Registration count increased!
            // Show animation on dashboard if it's DashboardActivity
            if (context instanceof DashboardActivity) {
                ((DashboardActivity) context).showCelebrationAnimation(totalRegistrations);
            }
            sendNotificationOnly(context, totalRegistrations);
            
            // Save this count
            prefs.edit().putInt(LAST_MILESTONE_KEY, totalRegistrations).apply();
        }
        
        // PRODUCTION MODE (comment out above, uncomment below):
        // Check if we've hit a new lakh (100,000 milestone)
        // int currentMilestone = (totalRegistrations / 100000) * 100000;
        // if (currentMilestone > 0 && currentMilestone > lastMilestone && totalRegistrations >= currentMilestone) {
        //     if (context instanceof DashboardActivity) {
        //         ((DashboardActivity) context).showCelebrationAnimation(currentMilestone);
        //     }
        //     sendNotificationOnly(context, currentMilestone);
        //     prefs.edit().putInt(LAST_MILESTONE_KEY, currentMilestone).apply();
        // }
    }
    
    public static void sendNotificationOnly(Context context, int milestone) {
        showMilestoneNotification(context, milestone);
    }
    
    private static void showMilestoneNotification(Context context, int milestone) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification channel with ALARM importance to bypass silent mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete existing channel to recreate with new sound settings
            try {
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
            } catch (Exception e) {
                android.util.Log.e("MilestoneNotification", "Error deleting channel", e);
            }
            
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            
            // Use default notification sound
            channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setDescription("Notifications for registration milestones");
            
            notificationManager.createNotificationChannel(channel);
        }
        
        // Create intent to open dashboard activity
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra("show_celebration", true);
        intent.putExtra("milestone", milestone);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        String title = "🎉 MILESTONE ACHIEVED! 🎉";
        String message = String.format("We've reached %,d registrations!", milestone);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.garuda)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL);
        
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    

    
    // Manual reset for testing
    public static void resetMilestone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(LAST_MILESTONE_KEY, 0).apply();
    }
}
