package com.example.helpassistance.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.example.helpassistance.R;
import com.example.helpassistance.helpers.Config;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    Button user_btn, helper_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user_btn = findViewById(R.id.user_btn);
        helper_btn = findViewById(R.id.helper_btn);

        SharedPreferences preferences = getSharedPreferences(Config.preference, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (preferences.getString(Config.remember_me, "").equals("true")) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                if (preferences.getString(Config.isHelper, "").equals("no")) {
                    startActivity(new Intent(this, CustomerMapActivity.class));
                    finish();
                } else if (preferences.getString(Config.isHelper, "").equals("yes")) {
                    startActivity(new Intent(this, HelperDashboard.class));
                    finish();
                }
            }
        }

        user_btn.setOnClickListener(view -> {
            editor.putString(Config.isHelper, "no").apply();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        helper_btn.setOnClickListener(view -> {
            editor.putString(Config.isHelper, "yes").apply();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });


    }


}