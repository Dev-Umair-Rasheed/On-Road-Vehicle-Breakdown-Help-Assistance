package com.example.helpassistance.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.helpassistance.R;
import com.example.helpassistance.helpers.Config;
import com.example.helpassistance.models.Customer;
import com.example.helpassistance.models.Helper;
import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    TextView go_to_login;
    EditText username_edt, password_edt, email_edt, contact_edt, speciality_edt, experience_edt, vehicle_no_edt;
    Button sign_up_btn;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;
    SharedPreferences preferences;
    String isHelper;
    private Location location;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please wait...");

        go_to_login = findViewById(R.id.go_to_login);
        email_edt = findViewById(R.id.email_edt);
        username_edt = findViewById(R.id.username_edt);
        password_edt = findViewById(R.id.password_edt);
        sign_up_btn = findViewById(R.id.sign_up_btn);
        contact_edt = findViewById(R.id.contact_edt);
        speciality_edt = findViewById(R.id.speciality_edt);
        experience_edt = findViewById(R.id.experience_edt);
        vehicle_no_edt = findViewById(R.id.vehicle_no_edt);

        firebaseAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences(Config.preference, MODE_PRIVATE);
        isHelper = preferences.getString(Config.isHelper, "");

        if (isHelper.equals("no")) {
            speciality_edt.setVisibility(View.GONE);
            experience_edt.setVisibility(View.GONE);
        } else if (isHelper.equals("yes")) {
            speciality_edt.setVisibility(View.VISIBLE);
            experience_edt.setVisibility(View.VISIBLE);
        }

        sign_up_btn.setOnClickListener(v -> {
            if (Validate()) {
                if (isHelper.equals("yes")) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // Request location permission
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            userSignUp();
                        }
                    }
                } else {
                    userSignUp();
                }
            }

        });
        go_to_login.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    userSignUp();
                }
            } else {
                Toast.makeText(this, "Permission is required for helpers!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void userSignUp() {
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email_edt.getText().toString().trim()
                        , password_edt.getText().toString().trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String key = task.getResult().getUser().getUid();
                        if (isHelper.equals("no")) {
                            Customer customer = new Customer();
                            customer.setEmail(email_edt.getText().toString());
                            customer.setName(username_edt.getText().toString());
                            customer.setPassword(password_edt.getText().toString());
                            customer.setContact(contact_edt.getText().toString());
                            customer.setVehicle_no(vehicle_no_edt.getText().toString());
                            customer.setKey(key);
                            customer.setLatitude(0.0);
                            customer.setLongitude(0.0);
                            Config.customer_db.child(key).setValue(customer).addOnSuccessListener(unused -> {
                                progressDialog.dismiss();
                                startActivity(new Intent(this, CustomerMapActivity.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(SignupActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show();
                            });
                        } else if (isHelper.equals("yes")) {
                            Helper helper = new Helper();
                            helper.setEmail(email_edt.getText().toString());
                            helper.setName(username_edt.getText().toString());
                            helper.setPassword(password_edt.getText().toString());
                            helper.setContact(contact_edt.getText().toString());
                            helper.setVehicle_no(vehicle_no_edt.getText().toString());
                            helper.setSpeciality(speciality_edt.getText().toString());
                            helper.setExperience(experience_edt.getText().toString());
                            helper.setStatus("Available");
                            helper.setKey(key);
                            helper.setLatitude(location.getLatitude());
                            helper.setLongitude(location.getLongitude());
                            Config.helper_db.child(key).setValue(helper).addOnSuccessListener(unused -> {
                                progressDialog.dismiss();
                                startActivity(new Intent(this, HelperDashboard.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(SignupActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(SignupActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignupActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean Validate() {
        if (username_edt.getText().toString().isEmpty()) {
            username_edt.setError("PLease enter name!");
            return false;
        }
        if (email_edt.getText().toString().isEmpty()) {
            email_edt.setError("PLease enter email!");
            return false;
        }
        if (contact_edt.getText().toString().isEmpty()) {
            contact_edt.setError("PLease enter Contact!");
            return false;
        }
        if (isHelper.equals("yes")) {
            if (speciality_edt.getText().toString().isEmpty()) {
                speciality_edt.setError("PLease enter speciality!");
                return false;
            }
            if (experience_edt.getText().toString().isEmpty()) {
                experience_edt.setError("PLease enter experience!");
                return false;
            }
        }
        if (vehicle_no_edt.getText().toString().isEmpty()) {
            vehicle_no_edt.setError("PLease enter vehicle number!");
            return false;
        }
        if (password_edt.getText().toString().isEmpty()) {
            password_edt.setError("PLease enter password!");
            return false;
        }
        return true;
    }
}