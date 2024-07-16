package com.example.helpassistance.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import com.example.helpassistance.R;
import com.example.helpassistance.helpers.Config;
import com.example.helpassistance.models.Customer;
import com.example.helpassistance.models.Helper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerLocation extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private FirebaseUser user;
    private Map<Marker, Customer> markerHelperMap = new HashMap<>();
    private LocationManager locationManager;
    private String hEmail;
    private LatLng yourLocation, customerLocation;
    private Dialog dialog;
    private Helper helper;
    private Button updateStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_location);

        updateStatus = findViewById(R.id.update_status);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        hEmail = getIntent().getStringExtra("cEmail");
        helper = new Helper();
        Config.helper_db.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    helper = snapshot.getValue(Helper.class);
                    helper.setStatus("Not Available");
                    Config.helper_db.child(user.getUid()).setValue(helper);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerLocation.this, "Oops! Something went wrong.", Toast.LENGTH_SHORT).show();
            }
        });

        updateStatus.setOnClickListener(v -> {
            helper.setStatus("Available!");
            Config.helper_db.child(user.getUid()).setValue(helper)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(CustomerLocation.this, "Congratulations! You have successfully complete your task.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(CustomerLocation.this, "Oops! Something went wrong.", Toast.LENGTH_SHORT).show());
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateLocationUI();
        mMap.setOnMarkerClickListener(marker -> {
            Customer customer = markerHelperMap.get(marker);
            if (customer != null) {
                showCustomerDetails(customer);
            }
            return false;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocationUI();
            } else {
                Toast.makeText(this, "Permission required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateLocationUI() {
        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    showHelperLocation(location);
                    fetchCustomerLocation();
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, this);
                }
            }
        }
    }

    private void showHelperLocation(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            yourLocation = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(yourLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(yourLocation, 15.0f));
            Config.helper_db.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Helper helper = snapshot.getValue(Helper.class);
                        if (helper != null) {
                            helper.setLongitude(latitude);
                            helper.setLongitude(longitude);
                            Config.helper_db.child(user.getUid()).setValue(helper);
                        } else {
                            Toast.makeText(CustomerLocation.this, "Oops! Something went wrong. Check your internet connection.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CustomerLocation.this, "Oops! Something went wrong. Check your internet connection.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void fetchCustomerLocation() {
        Config.customer_db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot helperSnapshot : snapshot.getChildren()) {
                    Customer customer = helperSnapshot.getValue(Customer.class);
                    if (customer != null && customer.getEmail().equals(hEmail)) {
                        customerLocation = new LatLng(customer.getLatitude(), customer.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions().position(customerLocation).title(customer.getName()));
                        markerHelperMap.put(marker, customer);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errorf
            }
        });
    }

    private void showCustomerDetails(Customer customer) {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.info_dialog);
        dialog.setCancelable(true);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        AppCompatButton btnCall = dialog.findViewById(R.id.call_helper);
        TextView hName = dialog.findViewById(R.id.h_name);
        TextView hEmail = dialog.findViewById(R.id.h_email);
        TextView hNum = dialog.findViewById(R.id.h_num);
        TextView hStatus = dialog.findViewById(R.id.h_status);
        hName.setText(customer.getName());
        hEmail.setText(customer.getEmail());
        hNum.setText(customer.getContact());
        hStatus.setVisibility(View.GONE);
        btnCall.setVisibility(View.GONE);
        dialog.show();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        showHelperLocation(location);
        fetchCustomerLocation();
    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}