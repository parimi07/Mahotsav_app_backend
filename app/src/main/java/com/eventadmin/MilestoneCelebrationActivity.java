package com.eventadmin;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;

public class MilestoneCelebrationActivity extends AppCompatActivity {
    
    private LottieAnimationView lottieAnimation;
    private TextView milestoneText;
    private MediaPlayer mediaPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make it full-screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_milestone_celebration);
        
        lottieAnimation = findViewById(R.id.lottieAnimation);
        milestoneText = findViewById(R.id.milestoneText);
        
        // Get milestone from intent
        int milestone = getIntent().getIntExtra("milestone", 100000);
        milestoneText.setText(String.format("🎉 %,d Registrations! 🎉", milestone));
        
        // Play Lottie animation from assets
        try {
            lottieAnimation.setAnimation("miles_stone_2.json");
            lottieAnimation.setRepeatCount(2);  // Play 3 times total
            lottieAnimation.playAnimation();
        } catch (Exception e) {
            android.util.Log.e("MilestoneCelebration", "Animation load failed", e);
        }
        
        // Play sound LOUD even if phone is silent
        playMilestoneSound(milestone);
        
        // Close on click
        findViewById(R.id.celebrationLayout).setOnClickListener(v -> finish());
    }
    
    private void playMilestoneSound(int milestone) {
        try {
            // Get sound resource based on build flavor
            String soundFileName = getString(R.string.sound_file);
            int soundResId = getResources().getIdentifier(soundFileName, "raw", getPackageName());
            
            mediaPlayer = MediaPlayer.create(this, soundResId);
            
            if (mediaPlayer != null) {
                // Set audio attributes to ALARM to bypass silent mode
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();
                    mediaPlayer.setAudioAttributes(audioAttributes);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
                
                // Set volume to maximum
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                
                mediaPlayer.setVolume(1.0f, 1.0f);  // Max volume
                mediaPlayer.start();
                
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
