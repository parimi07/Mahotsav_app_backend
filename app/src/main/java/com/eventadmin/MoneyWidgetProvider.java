package com.eventadmin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MoneyWidgetProvider extends AppWidgetProvider {

    private static final String WIDGET_PREFS = "WidgetPrefs";
    private static final String KEY_TOTAL_MONEY = "totalMoney";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Schedule periodic updates using WorkManager
        scheduleWidgetUpdate(context);

        // Update all widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        // First widget added, start periodic updates
        scheduleWidgetUpdate(context);
        
        // Trigger immediate update
        Intent intent = new Intent(context, WidgetUpdateWorker.class);
        intent.setAction("com.eventadmin.UPDATE_WIDGET");
        context.sendBroadcast(intent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Last widget removed, cancel periodic updates
        WorkManager.getInstance(context).cancelAllWorkByTag("widget_update");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        if ("com.eventadmin.UPDATE_WIDGET".equals(intent.getAction())) {
            // Manual update triggered
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, MoneyWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    private void scheduleWidgetUpdate(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WidgetUpdateWorker.class,
                15, TimeUnit.MINUTES
        )
        .addTag("widget_update")
        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_update",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Get stored money value
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        String totalMoney = prefs.getString(KEY_TOTAL_MONEY, "₹0");

        // Create RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_money);
        views.setTextViewText(R.id.widgetMoneyText, totalMoney);

        // Set up click to open app
        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent);

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void updateWidgetMoney(Context context, int totalMoney) {
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOTAL_MONEY, "₹" + totalMoney).apply();

        // Trigger widget update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MoneyWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
