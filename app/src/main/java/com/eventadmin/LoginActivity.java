package com.eventadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    
    // Update this with your computer's IP address when testing on physical device
    // For emulator use: http://10.0.2.2:3000
    private static final String API_BASE_URL = "http://10.10.214.62:3000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if already logged in
        SharedPreferences prefs = getSharedPreferences("EventAdminPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("authToken", null);
        if (token != null) {
            navigateToDashboard();
            return;
        }
        
        setContentView(R.layout.activity_login);
        
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        
        requestQueue = Volley.newRequestQueue(this);
        
        loginButton.setOnClickListener(v -> handleLogin());
    }
    
    private void handleLogin() {
        String userId = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter User ID and Password", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        
        // Mock credential validation - ID: 001, Password: Maruthi
        if (userId.equals("001") && password.equals("Maruthi")) {
            // Save mock login state
            SharedPreferences prefs = getSharedPreferences("EventAdminPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("authToken", "mock_token_001").apply();
            
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToDashboard();
            
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        } else {
            Toast.makeText(this, "Invalid credentials. Use ID: 001, Password: Maruthi", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }
    }
    
    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
