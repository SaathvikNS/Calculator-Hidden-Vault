package com.example.calculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    static final String PREF_NAME = "VaultPrefs";
    static final String KEY_PIN_SET = "pin_set";
    static final String KEY_PIN = "pin";

    private SharedPreferences prefs;
    private EditText passwordEditText;
    private Button setPasswordButton;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Get SharedPreferences to check if a password is already set
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        passwordEditText = findViewById(R.id.passwordEditText);
        setPasswordButton = findViewById(R.id.setPasswordButton);
        loginButton = findViewById(R.id.loginButton);

        boolean pinSet = prefs.getBoolean(KEY_PIN_SET, false);

        // If a PIN is already set, hide the set password button and show login button
        if (pinSet) {
            setPasswordButton.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
        } else {
            // If no PIN is set, show only the set password button
            setPasswordButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
        }

        // When the user clicks on "Set Password", save the new PIN and proceed to the main activity
        setPasswordButton.setOnClickListener(v -> {
            String newPin = passwordEditText.getText().toString();

            if (newPin.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() < 4) {
                Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hash the new PIN with BCrypt for secure storage
            String hashedPin = BCrypt.hashpw(newPin, BCrypt.gensalt());
            prefs.edit().putString(KEY_PIN, hashedPin).putBoolean(KEY_PIN_SET, true).apply();

            Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show();

            // Proceed to MainActivity after setting the password
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // When the user clicks on "Login", check if the entered PIN matches the stored PIN
        loginButton.setOnClickListener(v -> {
            String enteredPin = passwordEditText.getText().toString();
            String storedPin = prefs.getString(KEY_PIN, "");

            // Check if the entered PIN matches the stored hashed PIN
            if (BCrypt.checkpw(enteredPin, storedPin)) {
                // If the password matches, proceed to the main activity
                startActivity(new Intent(this, VaultActivity.class));
                finish();
            } else {
                // Show an error if the password is incorrect
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
