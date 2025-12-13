package com.eventadmin;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WidgetUpdateWorker extends Worker {

    private static final String TAG = "WidgetUpdateWorker";
    private static final String API_BASE_URL = "https://mahotsav-app-backend.onrender.com/api";

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Widget update worker started");
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                API_BASE_URL + "/stats",
                null,
                response -> {
                    try {
                        int totalMoney = response.getInt("totalMoney");
                        int totalRegistrations = response.getInt("totalRegistrations");
                        Log.d(TAG, "Fetched total money: " + totalMoney + ", registrations: " + totalRegistrations);
                        
                        // Update widget
                        MoneyWidgetProvider.updateWidgetMoney(getApplicationContext(), totalMoney);
                        
                        // Check for milestone notifications
                        MilestoneNotificationManager.checkAndNotifyMilestone(getApplicationContext(), totalRegistrations);
                        
                        success[0] = true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                    }
                    latch.countDown();
                },
                error -> {
                    Log.e(TAG, "Error fetching widget data", error);
                    latch.countDown();
                }
        );

        requestQueue.add(request);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.failure();
        }

        return success[0] ? Result.success() : Result.retry();
    }
}
