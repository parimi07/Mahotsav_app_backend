package com.eventadmin;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView seriesIdText;
    private TextView totalRegsText, todayRegsText, monthRegsText, totalMoneyText;
    private EditText nameInput, emailInput, phoneInput, amountInput;
    private Button registerButton, listButton;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private RequestQueue requestQueue;
    private android.widget.FrameLayout celebrationOverlay;
    private com.airbnb.lottie.LottieAnimationView celebrationAnimation;
    private TextView celebrationText;
    
    private static final String API_BASE_URL = "http://10.10.214.62:3000/api";
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        SharedPreferences prefs = getSharedPreferences("EventAdminPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", null);
        
        if (authToken == null) {
            logout();
            return;
        }
        
        initializeViews();
        requestQueue = Volley.newRequestQueue(this);
        
        // Request notification permission for Android 13+
        requestNotificationPermission();
        
        loadDashboardData();
        
        registerButton.setOnClickListener(v -> handleRegister());
        listButton.setOnClickListener(v -> openListActivity());
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);
        
        // Check if opened from notification
        checkNotificationIntent();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent();
    }
    
    private void checkNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("show_celebration", false)) {
            int milestone = intent.getIntExtra("milestone", 0);
            if (milestone > 0) {
                // Delay to ensure views are fully initialized
                new Handler().postDelayed(() -> {
                    if (celebrationOverlay != null && celebrationAnimation != null && celebrationText != null) {
                        showCelebrationAnimation(milestone);
                    }
                }, 1000);
            }
            intent.removeExtra("show_celebration");
        }
    }
    
    private void initializeViews() {
        seriesIdText = findViewById(R.id.seriesIdText);
        totalRegsText = findViewById(R.id.totalRegsText);
        todayRegsText = findViewById(R.id.todayRegsText);
        monthRegsText = findViewById(R.id.monthRegsText);
        totalMoneyText = findViewById(R.id.totalMoneyText);
        
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        amountInput = findViewById(R.id.amountInput);
        registerButton = findViewById(R.id.registerButton);
        listButton = findViewById(R.id.listButton);
        
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        celebrationOverlay = findViewById(R.id.celebrationOverlay);
        celebrationAnimation = findViewById(R.id.celebrationAnimation);
        celebrationText = findViewById(R.id.celebrationText);
        
        // Set up overlay click listener
        if (celebrationOverlay != null) {
            celebrationOverlay.setOnClickListener(v -> hideCelebration());
        }
    }
    
    private String lastUserId = null;
    private Handler autoRefreshHandler = new Handler();
    private Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadDashboardData();
            autoRefreshHandler.postDelayed(this, 5000); // Refresh every 5 seconds
        }
    };

    private void loadDashboardData() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Fetch latest registration from MongoDB via backend
        JsonObjectRequest seriesRequest = new JsonObjectRequest(
            Request.Method.GET,
            API_BASE_URL + "/current-series",
            null,
            response -> {
                try {
                    String latestUserId = response.getString("seriesId");
                    
                    // Animate if userId changed
                    if (lastUserId != null && !lastUserId.equals(latestUserId)) {
                        animateUserIdChange(lastUserId, latestUserId);
                    } else {
                        seriesIdText.setText(latestUserId);
                    }
                    lastUserId = latestUserId;
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error parsing userId", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                Toast.makeText(this, "Cannot connect to server. Check network.", Toast.LENGTH_LONG).show();
                seriesIdText.setText("Connection Error");
            }
        );
        
        // Load stats from MongoDB
        JsonObjectRequest statsRequest = new JsonObjectRequest(
            Request.Method.GET,
            API_BASE_URL + "/stats",
            null,
            response -> {
                try {
                    int totalReg = response.getInt("totalRegistrations");
                    int totalMoney = response.getInt("totalMoney");
                    int todayReg = response.getInt("todayRegistrations");
                    int monthReg = response.getInt("monthRegistrations");
                    
                    totalRegsText.setText(String.valueOf(totalReg));
                    totalMoneyText.setText("₹" + totalMoney);
                    todayRegsText.setText(String.valueOf(todayReg));
                    monthRegsText.setText(String.valueOf(monthReg));
                    
                    // Update widget with total money
                    MoneyWidgetProvider.updateWidgetMoney(DashboardActivity.this, totalMoney);
                    
                    // Check for milestone notifications
                    MilestoneNotificationManager.checkAndNotifyMilestone(DashboardActivity.this, totalReg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            },
            error -> {
                Toast.makeText(this, "Stats unavailable. Check backend connection.", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }
        );
        
        requestQueue.add(seriesRequest);
        requestQueue.add(statsRequest);
    }
    
    private void loadStatsFromMongo() {
        String mongoDataApiUrl = "https://ap-south-1.aws.data.mongodb-api.com/app/data-cvswohk/endpoint/data/v1/action/find";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("dataSource", "events");
            requestBody.put("database", "eventadmin");
            requestBody.put("collection", "registrations");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        JsonObjectRequest statsRequest = new JsonObjectRequest(
            Request.Method.POST,
            mongoDataApiUrl,
            requestBody,
            response -> {
                try {
                    if (response.has("documents")) {
                        JSONArray docs = response.getJSONArray("documents");
                        int total = docs.length();
                        totalRegsText.setText(String.valueOf(total));
                        totalMoneyText.setText("₹" + (total * 200));
                        todayRegsText.setText("0");
                        monthRegsText.setText(String.valueOf(total));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {}
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("api-key", "qNE1w24XHDzjFNDpJ4nh0sVqfP0rR5bOGX8Cf2rQSk6cK8L9ZlNGxWp5mIvY6f5T");
                return headers;
            }
        };
        
        requestQueue.add(statsRequest);
    }
    
    private void animateUserIdChange(String oldId, String newId) {
        try {
            String oldNum = oldId.replace("MH26", "");
            String newNum = newId.replace("MH26", "");
            
            int oldValue = Integer.parseInt(oldNum);
            int newValue = Integer.parseInt(newNum);
            
            ValueAnimator animator = ValueAnimator.ofInt(oldValue, newValue);
            animator.setDuration(1500);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                String formatted = String.format("MH26%06d", val);
                seriesIdText.setText(formatted);
            });
            animator.start();
        } catch (Exception e) {
            seriesIdText.setText(newId);
        }
    }
    
    private void handleRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);
        
        JSONObject registerData = new JSONObject();
        try {
            registerData.put("name", name);
            registerData.put("email", email);
            registerData.put("phone", phone);
            registerData.put("amount", amount);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            API_BASE_URL + "/register",
            registerData,
            response -> {
                try {
                    String seriesId = response.getString("seriesId");
                    Toast.makeText(this, "Registered! Series ID: " + seriesId, Toast.LENGTH_LONG).show();
                    
                    // Clear inputs
                    nameInput.setText("");
                    emailInput.setText("");
                    phoneInput.setText("");
                    amountInput.setText("");
                    
                    // Reload data
                    loadDashboardData();
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);
            },
            error -> {
                handleError(error);
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);
            }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
    
    private void handleError(com.android.volley.VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
            logout();
            return;
        }
        
        String errorMsg = "Error loading data";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String errorResponse = new String(error.networkResponse.data);
                JSONObject errorJson = new JSONObject(errorResponse);
                errorMsg = errorJson.optString("message", errorMsg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Start auto-refresh when screen is visible
        autoRefreshHandler.post(autoRefreshRunnable);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh when screen is not visible
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        } else if (item.getItemId() == R.id.action_test_notification) {
            testMilestoneNotification();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. Notifications won't work.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void testMilestoneNotification() {
        // Test notification with current registration count
        String currentCount = totalRegsText.getText().toString();
        try {
            int count = Integer.parseInt(currentCount);
            showCelebrationAnimation(count);
            MilestoneNotificationManager.sendNotificationOnly(this, count);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    public void showCelebrationAnimation(int milestone) {
        runOnUiThread(() -> {
            try {
                if (celebrationOverlay == null || celebrationAnimation == null || celebrationText == null) {
                    Toast.makeText(this, String.format("🎉 %,d Registrations! 🎉", milestone), Toast.LENGTH_LONG).show();
                    return;
                }
                
                celebrationText.setText(String.format("🎉 %,d Registrations! 🎉", milestone));
                celebrationOverlay.setVisibility(View.VISIBLE);
                celebrationOverlay.bringToFront();
                
                Toast.makeText(this, "🎉 Celebration overlay shown!", Toast.LENGTH_SHORT).show();
                android.util.Log.d("Dashboard", "Overlay visibility set to VISIBLE");
                
                // Try to load animation, but don't let it crash the app
                try {
                    if (celebrationAnimation != null) {
                        celebrationAnimation.setAnimation("miles_stone_2.json");
                        celebrationAnimation.playAnimation();
                        android.util.Log.d("Dashboard", "Animation loaded and playing successfully");
                        Toast.makeText(this, "Animation loaded!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("Dashboard", "Animation load failed: " + e.getMessage(), e);
                    Toast.makeText(this, "Animation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Animation failed, but continue with text display
                }
                
                // Auto-hide after 8 seconds
                new Handler().postDelayed(() -> hideCelebration(), 8000);
            } catch (Exception e) {
                android.util.Log.e("Dashboard", "Animation error: " + e.getMessage(), e);
                Toast.makeText(this, String.format("🎉 %,d Registrations! 🎉", milestone), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void hideCelebration() {
        if (celebrationOverlay != null) {
            celebrationOverlay.setVisibility(View.GONE);
        }
        if (celebrationAnimation != null) {
            celebrationAnimation.cancelAnimation();
        }
    }

    
    private void openListActivity() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }
    
    private void logout() {
        SharedPreferences prefs = getSharedPreferences("EventAdminPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("authToken").apply();
        
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
