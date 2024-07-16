package com.example.helpassistance.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.helpassistance.R;
import com.example.helpassistance.databinding.ActivityCustomerMapBinding;
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

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ActivityCustomerMapBinding binding;
    private LocationManager locationManager;
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 1;
    private static final long minTime = 5000;
    private static final float minDistance = 10;
    private FirebaseUser user;
    private Map<Marker, Helper> markerHelperMap = new HashMap<>();
    private Dialog dialog;
    private Button hLocTime;

    Location my_location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        user = FirebaseAuth.getInstance().getCurrentUser();

        binding.helperLoc.setOnClickListener(v -> {
            Dialog dialogCheck = new Dialog(this);
            dialogCheck.setContentView(R.layout.check_loc_dialog);
            dialogCheck.setCancelable(true);
            dialogCheck.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            AppCompatButton btnCall = dialogCheck.findViewById(R.id.call_helper);
            Button btncheck = dialogCheck.findViewById(R.id.btnCheck);
            EditText edtEmail = dialogCheck.findViewById(R.id.helperEmail);
            btncheck.setOnClickListener(v1 -> {
                if(edtEmail.getText().toString().isEmpty()){
                    edtEmail.setError("PLease enter email!");
                }else{
                    Intent i = new Intent(CustomerMapActivity.this, HelperLocation.class);
                    i.putExtra("hEmail",edtEmail.getText().toString());
                    startActivity(i);
                }
            });
            dialogCheck.show();
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocationUI();
            } else {
                Toast.makeText(this, "Permission required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateLocationUI() {
        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
            } else {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    showUserLocation(location);
                    fetchNearbyHelpers();
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateLocationUI();
        mMap.setOnMarkerClickListener(marker -> {
            Helper helper = markerHelperMap.get(marker);
            if (helper != null) {
                showHelperDetails(helper);
            }
            return false;
        });
    }

    private void showUserLocation(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng userLocation = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f));
            Config.customer_db.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Customer customer = snapshot.getValue(Customer.class);
                        if (customer != null) {
                            customer.setLongitude(latitude);
                            customer.setLongitude(longitude);
                            Config.customer_db.child(user.getUid()).setValue(customer);
                        } else {
                            Toast.makeText(CustomerMapActivity.this, "Oops! Something went wrong. Check your internet connection.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CustomerMapActivity.this, "Oops! Something went wrong. Check your internet connection.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void fetchNearbyHelpers() {
        Config.helper_db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot helperSnapshot : snapshot.getChildren()) {
                    Helper helper = helperSnapshot.getValue(Helper.class);
                    if (helper != null) {
                        LatLng helperLocation = new LatLng(helper.getLatitude(), helper.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions().position(helperLocation).title(helper.getName()));
                        markerHelperMap.put(marker, helper);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errorf
            }
        });
    }

    private void showHelperDetails(Helper helper) {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.info_dialog);
        dialog.setCancelable(true);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        AppCompatButton btnCall = dialog.findViewById(R.id.call_helper);
        TextView hName = dialog.findViewById(R.id.h_name);
        TextView hEmail = dialog.findViewById(R.id.h_email);
        TextView hNum = dialog.findViewById(R.id.h_num);
        TextView hStatus = dialog.findViewById(R.id.h_status);
        hName.setText(helper.getName());
        hEmail.setText(helper.getEmail());
        hNum.setText(helper.getContact());
        hStatus.setText(helper.getStatus());
        btnCall.setOnClickListener(v -> callHelper(helper.getContact()));
        dialog.show();
    }

    private void callHelper(String num) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + num));
        if (ContextCompat.checkSelfPermission(CustomerMapActivity.this,
                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            dialog.dismiss();
            startActivity(callIntent);
        } else {
            dialog.dismiss();
            ActivityCompat.requestPermissions(CustomerMapActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE}, 2);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        showUserLocation(location);
        fetchNearbyHelpers();
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