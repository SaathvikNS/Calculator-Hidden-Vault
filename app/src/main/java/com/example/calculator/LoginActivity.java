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

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        passwordEditText = findViewById(R.id.passwordEditText);
        setPasswordButton = findViewById(R.id.setPasswordButton);
        loginButton = findViewById(R.id.loginButton);

        boolean pinSet = prefs.getBoolean(KEY_PIN_SET, false);

        if (pinSet) {
            setPasswordButton.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
        } else {
            setPasswordButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
        }

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

            String hashedPin = BCrypt.hashpw(newPin, BCrypt.gensalt());
            prefs.edit().putString(KEY_PIN, hashedPin).putBoolean(KEY_PIN_SET, true).apply();

            Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "You can access the vault by long pressing the '=' button", Toast.LENGTH_LONG).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> {
            String enteredPin = passwordEditText.getText().toString();
            String storedPin = prefs.getString(KEY_PIN, "");

            if (BCrypt.checkpw(enteredPin, storedPin)) {
                startActivity(new Intent(this, VaultActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
