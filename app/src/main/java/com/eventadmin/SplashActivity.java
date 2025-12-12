package com.eventadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashImage = findViewById(R.id.splashImage);

        // Create rotation animation
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(2000); // 2 seconds per rotation
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        
        splashImage.startAnimation(rotate);

        // Navigate after 3 seconds
        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("EventAdminPrefs", Context.MODE_PRIVATE);
            String token = prefs.getString("authToken", null);
            
            Intent intent;
            if (token != null) {
                // Already logged in, go to Dashboard
                intent = new Intent(SplashActivity.this, DashboardActivity.class);
            } else {
                // Not logged in, go to Login
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
