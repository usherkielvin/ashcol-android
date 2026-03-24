package app.hub.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.UpdateLocationRequest;
import app.hub.api.UpdateLocationResponse;
import app.hub.util.GooglePlayServicesUtils;
import app.hub.util.TokenManager;

public class EmployeeMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double customerLatitude, customerLongitude;
    private String customerAddress, ticketId;
    private TextView tvCustomerAddress, tvDistance, tvETA;
    private Button btnBack, btnStartNavigation;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private TokenManager tokenManager;
    
    private Marker employeeMarker, customerMarker;
    private Polyline routePolyline;
    private LatLng currentEmployeeLocation;
    private Handler locationUpdateHandler;
    private Runnable locationUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_map);

        if (!GooglePlayServicesUtils.ensureAvailable(this)) {
            finish();
            return;
        }

        // Get data from intent
        customerLatitude = getIntent().getDoubleExtra("customer_latitude", 0.0);
        customerLongitude = getIntent().getDoubleExtra("customer_longitude", 0.0);
        customerAddress = getIntent().getStringExtra("customer_address");
        ticketId = getIntent().getStringExtra("ticket_id");

        initViews();
        MapsInitializer.initialize(getApplicationContext());
        setupMap();
        setupLocationServices();
    }

    private void initViews() {
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvETA = findViewById(R.id.tvETA);
        btnBack = findViewById(R.id.btnBack);
        btnStartNavigation = findViewById(R.id.btnStartNavigation);

        tokenManager = new TokenManager(this);

        if (customerAddress != null) {
            tvCustomerAddress.setText(customerAddress);
        }

        btnBack.setOnClickListener(v -> finish());
        btnStartNavigation.setOnClickListener(v -> startNavigation());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationUpdateHandler = new Handler();
        
        // Create location request
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000) // 5 seconds
                .setFastestInterval(2000); // 2 seconds

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateEmployeeLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                    updateLocationOnServer(location.getLatitude(), location.getLongitude());
                }
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (customerLatitude != 0 && customerLongitude != 0) {
            LatLng customerLocation = new LatLng(customerLatitude, customerLongitude);
            
            // Add customer marker
            customerMarker = mMap.addMarker(new MarkerOptions()
                    .position(customerLocation)
                    .title("Customer Location")
                    .snippet(customerAddress != null ? customerAddress : "Service Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Start location updates to get employee location
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        mMap.setMyLocationEnabled(true);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        
        // Get current location immediately
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateEmployeeLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                updateLocationOnServer(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void updateEmployeeLocation(LatLng employeeLocation) {
        currentEmployeeLocation = employeeLocation;
        
        // Update or create employee marker
        if (employeeMarker != null) {
            employeeMarker.setPosition(employeeLocation);
        } else {
            employeeMarker = mMap.addMarker(new MarkerOptions()
                    .position(employeeLocation)
                    .title("Your Location")
                    .snippet("Technician")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        // Update camera to show both locations
        if (customerMarker != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(employeeLocation);
            builder.include(customerMarker.getPosition());
            
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }

        // Calculate and display distance
        calculateDistance(employeeLocation, new LatLng(customerLatitude, customerLongitude));
        
        // Draw route (simplified straight line for now)
        drawRoute(employeeLocation, new LatLng(customerLatitude, customerLongitude));
    }

    private void calculateDistance(LatLng from, LatLng to) {
        float[] results = new float[1];
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results);
        
        float distanceInMeters = results[0];
        String distanceText;
        
        if (distanceInMeters < 1000) {
            distanceText = String.format("%.0f m", distanceInMeters);
        } else {
            distanceText = String.format("%.1f km", distanceInMeters / 1000);
        }
        
        tvDistance.setText("Distance: " + distanceText);
        
        // Estimate ETA (assuming average speed of 30 km/h)
        float distanceInKm = distanceInMeters / 1000;
        float etaInMinutes = (distanceInKm / 30) * 60;
        tvETA.setText(String.format("ETA: %.0f min", etaInMinutes));
    }

    private void drawRoute(LatLng from, LatLng to) {
        // Remove existing route
        if (routePolyline != null) {
            routePolyline.remove();
        }
        
        // Draw simple straight line route (in real app, use Google Directions API)
        List<LatLng> routePoints = new ArrayList<>();
        routePoints.add(from);
        routePoints.add(to);
        
        routePolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .color(Color.BLUE)
                .width(8f)
                .geodesic(true));
    }

    private void updateLocationOnServer(double latitude, double longitude) {
        String token = tokenManager.getToken();
        if (token == null) return;

        UpdateLocationRequest request = new UpdateLocationRequest(latitude, longitude);
        ApiService apiService = ApiClient.getApiService();
        Call<UpdateLocationResponse> call = apiService.updateLocation("Bearer " + token, request);

        call.enqueue(new Callback<UpdateLocationResponse>() {
            @Override
            public void onResponse(Call<UpdateLocationResponse> call, Response<UpdateLocationResponse> response) {
                // Location updated successfully (silent)
            }

            @Override
            public void onFailure(Call<UpdateLocationResponse> call, Throwable t) {
                // Handle error silently for now
            }
        });
    }

    private void startNavigation() {
        if (currentEmployeeLocation != null) {
            // In a real app, this would open Google Maps or other navigation app
            String uri = String.format("google.navigation:q=%f,%f", customerLatitude, customerLongitude);
            Toast.makeText(this, "Opening navigation to customer location", Toast.LENGTH_SHORT).show();
            
            // For now, just show a message
            Toast.makeText(this, "Navigation started to customer location", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
            locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required for navigation", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
