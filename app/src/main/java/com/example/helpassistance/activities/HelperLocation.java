package com.example.helpassistance.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

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

public class HelperLocation extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private FirebaseUser user;
    private Map<Marker, Helper> markerHelperMap = new HashMap<>();
    private LocationManager locationManager;
    private String hEmail;
    private LatLng userLocation, helperLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_location);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.hMap);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        hEmail = getIntent().getStringExtra("hEmail");

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateLocationUI();
        mMap.setOnMarkerClickListener(marker -> {
            Helper helper = markerHelperMap.get(marker);
            if (helper != null) {
                showHelperArrivalTime();
            }
            return false;
        });
    }

    private void showHelperArrivalTime() {
        Toast.makeText(this, "Estimated Arrival Time is:\n" + calculateArrivalTime(userLocation, helperLocation), Toast.LENGTH_SHORT).show();
    }

    private String calculateArrivalTime(LatLng customerLocation, LatLng helperLocation) {
        final int AVERAGE_SPEED_KMH = 50; // You can adjust this value as needed
        double distance = calculateDistance(customerLocation, helperLocation);
        double timeInMinutes = (distance / AVERAGE_SPEED_KMH) * 60; // Convert hours to minutes

        int hours = (int) timeInMinutes / 60;
        int minutes = (int) timeInMinutes % 60;

        if (hours > 0) {
            return hours + " hours and " + minutes + " minutes";
        } else {
            return minutes + " minutes";
        }
    }

    private double calculateDistance(LatLng start, LatLng end) {
        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(end.latitude - start.latitude);
        double lngDistance = Math.toRadians(end.longitude - start.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
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
                    showUserLocation(location);
                    fetchHelperLocation();
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, this);
                }
            }
        }
    }

    private void showUserLocation(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            userLocation = new LatLng(latitude, longitude);
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
                            Toast.makeText(HelperLocation.this, "Oops! Something went wrong. Check your internet connection.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(HelperLocation.this, "Oops! Something went wrong. Check your internet connection.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void fetchHelperLocation() {
        Config.helper_db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot helperSnapshot : snapshot.getChildren()) {
                    Helper helper = helperSnapshot.getValue(Helper.class);
                    if (helper != null && helper.getEmail().equals(hEmail)) {
                        helperLocation = new LatLng(helper.getLatitude(), helper.getLongitude());
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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        showUserLocation(location);
        fetchHelperLocation();
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