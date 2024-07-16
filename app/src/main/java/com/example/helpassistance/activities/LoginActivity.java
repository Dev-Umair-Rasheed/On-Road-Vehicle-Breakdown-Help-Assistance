package com.example.helpassistance.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.helpassistance.R;
import com.example.helpassistance.helpers.Config;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    TextView go_to_signup, forgot_pass_txt;
    EditText username_edt, password_edt;
    Button login_btn;
    CheckBox remember_me_chkbox;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;
    SharedPreferences Preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please wait...");

        remember_me_chkbox = findViewById(R.id.remember_me_chkbox);
        forgot_pass_txt = findViewById(R.id.forgot_pass_txt);
        go_to_signup = findViewById(R.id.go_to_signup);
        username_edt = findViewById(R.id.username_edt);
        password_edt = findViewById(R.id.password_edt);
        login_btn = findViewById(R.id.login_btn);

        Preferences = getSharedPreferences(Config.preference, MODE_PRIVATE);
        firebaseAuth = FirebaseAuth.getInstance();

        remember_me_chkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = Preferences.edit();
            if (isChecked) {
                editor.putString(Config.remember_me, "true");
            } else {
                editor.putString(Config.remember_me, "false");
            }
            editor.apply();
        });

        login_btn.setOnClickListener(v -> {
            if (Validate()) {
//                if (username_edt.getText().toString().equals("admin@gmail.com")
//                        && password_edt.getText().toString().equals("admin")) {
//                    adminLogin();
//                } else {
                userLogin();
//                }
            }

        });

        forgot_pass_txt.setOnClickListener(view -> {
            recover_password_dialog();
        });
        go_to_signup.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
    }

    private void recover_password_dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset password");
        LinearLayout linearLayout = new LinearLayout(this);
        final EditText emailedt = new EditText(this);
        emailedt.setHint("Email");
        emailedt.setWidth(350);
        emailedt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        linearLayout.addView(emailedt);
        linearLayout.setPadding(18, 18, 18, 18);
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            if (emailedt.getText().toString().isEmpty()) {
                emailedt.setError("Please enter email");
            } else {
                String pattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
                if (emailedt.getText().toString().matches(pattern)) {
                    String email = emailedt.getText().toString().trim();
                    recover_password(email);
                } else {
                    emailedt.setError("Enter a valid email");
                }
            }
        }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void recover_password(String email) {
        progressDialog.show();
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(LoginActivity.this, "Email sent, Please check the email to reset password.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, "Failed to sent email", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            e.printStackTrace();
        });
    }


    private void userLogin() {
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(username_edt.getText().toString().trim()
                        , password_edt.getText().toString().trim())
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        SharedPreferences preferences = getSharedPreferences(Config.preference, MODE_PRIVATE);
                        if (preferences.getString(Config.isHelper, "").equals("no")) {
                            startActivity(new Intent(this, CustomerMapActivity.class));
                            finish();
                        } else if (preferences.getString(Config.isHelper, "").equals("yes")) {
                            startActivity(new Intent(this, HelperDashboard.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Cannot login", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Cannot login", Toast.LENGTH_SHORT).show();
                });
    }

    private void adminLogin() {
        Toast.makeText(LoginActivity.this, "Login success", Toast.LENGTH_SHORT).show();
//        startActivity(new Intent(this, AdminPanel.class));
//        finish();
    }

    private boolean Validate() {
        if (username_edt.getText().toString().isEmpty()) {
            username_edt.setError("PLease enter username!");
            return false;
        }
        if (password_edt.getText().toString().isEmpty()) {
            password_edt.setError("PLease enter password!");
            return false;
        }
        return true;
    }
}