package com.example.calculator; // Correct package

import static com.example.calculator.ChangePasswordActivity.PREFS_NAME;
import static com.example.calculator.LoginActivity.KEY_PIN;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button changePasswordButton = findViewById(R.id.changePasswordButton); // Assuming you have the button
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String storedPin = prefs.getString(KEY_PIN, "");

        if(TextUtils.isEmpty(storedPin)){
            changePasswordButton.setEnabled(false);
            changePasswordButton.setText("Set a password first");
        }else{
            changePasswordButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChangePasswordActivity.class);
                startActivity(intent);
            });
        }


        // ... (Add listeners for other settings options later)
    }
}