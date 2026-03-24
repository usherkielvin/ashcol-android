package app.hub.map;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import app.hub.R;
import app.hub.util.GooglePlayServicesUtils;
import app.hub.util.LocationUtils;

public class MapSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapSelectionActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int MAP_LOAD_TIMEOUT = 10000; // 10 seconds
    private static final double MAX_SERVICE_DISTANCE_KM = 50.0; // 50km from nearest branch
    
    // ASHCOL Branch locations
    private static final LatLng[] ASHCOL_BRANCHES = {
        new LatLng(14.5176, 121.0509), // ASHCOL TAGUIG
        new LatLng(14.7000, 120.9822), // ASHCOL Valenzuela  
        new LatLng(14.7297, 121.1572), // ASHCOL Rodriguez Rizal
        new LatLng(15.0794, 120.6200), // ASHCOL PAMPANGA
        new LatLng(14.7942, 120.8794), // ASHCOL Bulacan
        new LatLng(14.2456, 120.8830), // ASHCOL GENTRI CAVITE
        new LatLng(14.3294, 120.9367), // ASHCOL DASMARINAS CAVITE
        new LatLng(14.2691, 121.0359), // ASHCOL STA ROSA – TAGAYTAY RD
        new LatLng(14.2691, 121.0359), // ASHCOL LAGUNA
        new LatLng(13.7565, 121.0583), // ASHCOL BATANGAS
        new LatLng(13.9317, 121.4227)  // ASHCOL CANDELARIA QUEZON PROVINCE
    };
    
    private static final String[] BRANCH_NAMES = {
        "ASHCOL TAGUIG", "ASHCOL Valenzuela", "ASHCOL Rodriguez Rizal",
        "ASHCOL PAMPANGA", "ASHCOL Bulacan", "ASHCOL GENTRI CAVITE",
        "ASHCOL DASMARINAS CAVITE", "ASHCOL STA ROSA – TAGAYTAY RD",
        "ASHCOL LAGUNA", "ASHCOL BATANGAS", "ASHCOL CANDELARIA QUEZON PROVINCE"
    };

    private GoogleMap mMap;
    private Button btnFinish;
    private TextView tvSelectedAddress;
    private com.google.android.material.card.MaterialCardView bottomSheet;
    private LinearLayout bottomSheetContent;
    private View dragHandle;
    private com.google.android.material.button.MaterialButton fabCurrentLocation;
    private FusedLocationProviderClient fusedLocationClient;

    private LatLng currentCenterLatLng;
    private String selectedAddress = "";
    private boolean mapReady = false;

    // Bottom sheet sliding variables
    private float initialY;
    private float initialTranslationY;
    private boolean isExpanded = false;
    private int peekHeight = 200; // dp
    private int expandedHeight = 400; // dp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_map_selection);
            Log.d(TAG, "MapSelectionActivity onCreate started");

            // Check Google Play Services availability first
            if (!GooglePlayServicesUtils.ensureAvailable(this)) {
                Log.e(TAG, "Google Play Services not available");
                Toast.makeText(this, "Google Play Services is required for maps", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            btnFinish = findViewById(R.id.btnFinish);
            tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
            bottomSheet = findViewById(R.id.bottomSheet);
            bottomSheetContent = findViewById(R.id.bottomSheetContent);
            dragHandle = findViewById(R.id.dragHandle);
            fabCurrentLocation = findViewById(R.id.fabCurrentLocation);

            // Initialize location client
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Setup close button
            android.widget.ImageButton closeButton = findViewById(R.id.closeButton);
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> {
                    Log.d(TAG, "Close button clicked");
                    setResult(RESULT_CANCELED);
                    finish();
                });
            } else {
                Log.w(TAG, "Close button not found in layout");
            }

            // Setup bottom sheet sliding
            setupBottomSheetSliding();

            // Initialize Maps SDK (recommended before map fragment)
            try {
                MapsInitializer.initialize(getApplicationContext());
                Log.d(TAG, "Maps SDK initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Maps SDK", e);
                Toast.makeText(this, "Maps initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            // Initialize the map with timeout
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                Log.d(TAG, "Map fragment found, getting map async");

                // Set up a timeout for map loading
                android.os.Handler timeoutHandler = new android.os.Handler();
                Runnable timeoutRunnable = () -> {
                    if (!mapReady) {
                        Log.e(TAG, "Map loading timed out");
                        Toast.makeText(this,
                                "Map loading timed out. Please check your internet connection and try again.",
                                Toast.LENGTH_LONG).show();
                        // Enable manual coordinate entry as fallback
                        enableManualCoordinateEntry();
                    }
                };
                timeoutHandler.postDelayed(timeoutRunnable, MAP_LOAD_TIMEOUT);

                // Add a delay to ensure the fragment is fully initialized
                mapFragment.getView().post(() -> {
                    Log.d(TAG, "Map fragment view is ready, calling getMapAsync");
                    mapFragment.getMapAsync(this);
                });
            } else {
                Log.e(TAG, "Map fragment not found - check layout");
                Toast.makeText(this, "Map failed to load. Check API key and internet.", Toast.LENGTH_LONG).show();
                // Enable manual coordinate entry as fallback
                enableManualCoordinateEntry();
            }

            // Finish button click handler
            btnFinish.setOnClickListener(v -> {
                Log.d(TAG, "Finish button clicked");
                if (currentCenterLatLng != null) {
                    // Check if location is within service area
                    if (isWithinServiceArea(currentCenterLatLng)) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("latitude", currentCenterLatLng.latitude);
                        resultIntent.putExtra("longitude", currentCenterLatLng.longitude);
                        resultIntent.putExtra("address", selectedAddress);
                        Log.d(TAG, "Returning result - lat: " + currentCenterLatLng.latitude +
                                ", lng: " + currentCenterLatLng.longitude + ", address: " + selectedAddress);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        // Show service area restriction
                        showServiceAreaDialog();
                    }
                } else {
                    Log.w(TAG, "Map is not ready yet, currentCenterLatLng is null");
                    Toast.makeText(this, "Map is not ready yet", Toast.LENGTH_SHORT).show();
                }
            });

            // Current location button click handler
            fabCurrentLocation.setOnClickListener(v -> {
                Log.d(TAG, "Current location button clicked");
                getCurrentLocation();
            });

            Log.d(TAG, "MapSelectionActivity onCreate completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupBottomSheetSliding() {
        // Convert dp to pixels
        float density = getResources().getDisplayMetrics().density;
        peekHeight = (int) (peekHeight * density);
        expandedHeight = (int) (expandedHeight * density);

        // Set initial position (collapsed)
        bottomSheet.post(() -> {
            int fullHeight = bottomSheet.getHeight();
            bottomSheet.setTranslationY(fullHeight - peekHeight);
        });

        // Handle drag gestures
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = event.getRawY();
                        initialTranslationY = bottomSheet.getTranslationY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - initialY;
                        float newTranslationY = initialTranslationY + deltaY;

                        // Constrain movement
                        int fullHeight = bottomSheet.getHeight();
                        float minTranslation = 0; // Fully expanded
                        float maxTranslation = fullHeight - peekHeight; // Collapsed

                        newTranslationY = Math.max(minTranslation, Math.min(maxTranslation, newTranslationY));
                        bottomSheet.setTranslationY(newTranslationY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Snap to expanded or collapsed based on position
                        float currentTranslation = bottomSheet.getTranslationY();
                        int fullHeight2 = bottomSheet.getHeight();
                        float midPoint = (fullHeight2 - peekHeight) / 2f;

                        if (currentTranslation < midPoint) {
                            // Snap to expanded
                            animateBottomSheet(0);
                            isExpanded = true;
                        } else {
                            // Snap to collapsed
                            animateBottomSheet(fullHeight2 - peekHeight);
                            isExpanded = false;
                        }
                        return true;
                }
                return false;
            }
        });

        // Also handle taps on drag handle
        dragHandle.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse
                int fullHeight = bottomSheet.getHeight();
                animateBottomSheet(fullHeight - peekHeight);
                isExpanded = false;
            } else {
                // Expand
                animateBottomSheet(0);
                isExpanded = true;
            }
        });
    }

    private void animateBottomSheet(float targetTranslationY) {
        ValueAnimator animator = ValueAnimator.ofFloat(bottomSheet.getTranslationY(), targetTranslationY);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float value = (Float) animation.getAnimatedValue();
            bottomSheet.setTranslationY(value);
        });
        animator.start();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        try {
            Log.d(TAG, "onMapReady called");
            mMap = googleMap;
            mapReady = true; // Set flag to indicate map is ready

            // Restrict map to Philippines only
            mMap.setMinZoomPreference(5.0f);

            // Set initial camera to center of Philippines
            LatLng philippinesCenter = new LatLng(12.8797, 121.7740);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(philippinesCenter, 10));
            currentCenterLatLng = philippinesCenter;

            Log.d(TAG, "Map camera set to Philippines center");

            // Get initial address with real Plus Code accuracy
            getAccurateAddressWithRealPlusCode(philippinesCenter);

            // Listen for camera movements to update the center location
            mMap.setOnCameraIdleListener(() -> {
                try {
                    LatLng centerLatLng = mMap.getCameraPosition().target;

                    if (isWithinPhilippines(centerLatLng)) {
                        // Snap to nearest road
                        snapToNearestRoad(centerLatLng);
                    } else {
                        if (currentCenterLatLng != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentCenterLatLng));
                        }
                        Toast.makeText(this, "Please select a location within the Philippines", Toast.LENGTH_SHORT)
                                .show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in camera idle listener", e);
                }
            });

            // Disable map click listeners since we're using the pin approach
            mMap.setOnMapClickListener(null);
            mMap.setOnMapLongClickListener(null);

            Log.d(TAG, "Map setup completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady", e);
            Toast.makeText(this, "Error setting up map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void enableManualCoordinateEntry() {
        Log.d(TAG, "Enabling manual coordinate entry as fallback");

        // Set a default location (Manila) as fallback
        currentCenterLatLng = new LatLng(14.5995, 120.9842);
        selectedAddress = "Manila, Philippines (Default Location)";

        if (tvSelectedAddress != null) {
            tvSelectedAddress.setText(selectedAddress);
        }

        Toast.makeText(this, "Using default location. You can still proceed with ticket creation.", Toast.LENGTH_LONG)
                .show();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request location permissions
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Use getCurrentLocation (High Accuracy) instead of getLastLocation
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Check if user location is within Philippines
                        if (isWithinPhilippines(userLocation)) {
                            // ZOOM INCREASED TO 18f HERE
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f));
                            currentCenterLatLng = userLocation;
                            getAddressFromCoordinates(userLocation);
                        } else {
                            // Even if location is found, if it's out of bounds...
                            // But usually current location logic override allows user to see where they are
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f));
                            if (!isWithinPhilippines(userLocation)) {
                                Toast.makeText(this, "Your current location is outside the Philippines",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Unable to get current location. Ensure GPS is on.", Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isWithinPhilippines(LatLng latLng) {
        return LocationUtils.isWithinPhilippines(latLng.latitude, latLng.longitude);
    }

    private void getAddressFromCoordinates(LatLng latLng) {
        tvSelectedAddress.setText("Loading address...");

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MapSelectionActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder addressBuilder = new StringBuilder();

                        // User requested format: [Plus Code/Feature Name] + [Address]
                        // e.g. "g28x+f9m, Taguig..."

                        // 1. Always start with Feature Name/Plus Code if available
                        if (address.getFeatureName() != null) {
                            addressBuilder.append(address.getFeatureName());
                        }

                        // 2. Add Locality/City
                        if (address.getLocality() != null) {
                            if (addressBuilder.length() > 0)
                                addressBuilder.append(", ");
                            addressBuilder.append(address.getLocality());
                        }

                        // 3. Add Sub-Admin Area (District/Province) if different
                        if (address.getSubAdminArea() != null
                                && !address.getSubAdminArea().equals(address.getLocality())) {
                            if (addressBuilder.length() > 0)
                                addressBuilder.append(", ");
                            addressBuilder.append(address.getSubAdminArea());
                        }

                        // 4. Add Admin Area (Region)
                        if (address.getAdminArea() != null) {
                            if (addressBuilder.length() > 0)
                                addressBuilder.append(", ");
                            addressBuilder.append(address.getAdminArea());
                        }

                        // 5. Add Country
                        if (addressBuilder.length() > 0)
                            addressBuilder.append(", ");
                        addressBuilder.append("Philippines");

                        selectedAddress = addressBuilder.toString();
                        tvSelectedAddress.setText(selectedAddress);
                    } else {
                        // Fallback only if Geocoder returns nothing
                        selectedAddress = String.format("Coordinates: %.6f, %.6f",
                                latLng.latitude, latLng.longitude);
                        tvSelectedAddress.setText(selectedAddress);
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error getting address", e);
                runOnUiThread(() -> {
                    // Fallback on network error
                    selectedAddress = String.format("Coordinates: %.6f, %.6f",
                            latLng.latitude, latLng.longitude);
                    tvSelectedAddress.setText(selectedAddress);
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (isExpanded) {
            // Collapse the bottom sheet first
            int fullHeight = bottomSheet.getHeight();
            animateBottomSheet(fullHeight - peekHeight);
            isExpanded = false;
        } else {
            super.onBackPressed();
        }
    }

    // Road snapping functionality with accurate Plus Codes
    private void snapToNearestRoad(LatLng originalLocation) {
        // Don't move the pin - keep user's exact selection
        // Just get the most accurate address for that exact location
        currentCenterLatLng = originalLocation;
        getAccurateAddressWithRealPlusCode(originalLocation);
    }

    // Calculate address quality score for road snapping
    private int calculateAddressScore(Address address) {
        int score = 0;
        
        // High score for street-level addresses
        if (address.getThoroughfare() != null && !address.getThoroughfare().isEmpty() &&
            !address.getThoroughfare().matches(".*[Ll]\\d+.*")) {
            score += 10; // Street name
        }
        
        if (address.getSubThoroughfare() != null && !address.getSubThoroughfare().isEmpty() &&
            !address.getSubThoroughfare().matches(".*[Ll]\\d+.*")) {
            score += 5; // Street number
        }
        
        // Medium score for locality
        if (address.getSubLocality() != null && !address.getSubLocality().isEmpty() &&
            !address.getSubLocality().matches(".*[Ll]\\d+.*")) {
            score += 3;
        }
        
        if (address.getLocality() != null && !address.getLocality().isEmpty()) {
            score += 2;
        }
        
        // Low score for admin area only
        if (address.getAdminArea() != null && !address.getAdminArea().isEmpty()) {
            score += 1;
        }
        
        return score;
    }

    // Service area validation
    private boolean isWithinServiceArea(LatLng location) {
        double minDistance = Double.MAX_VALUE;
        
        for (LatLng branch : ASHCOL_BRANCHES) {
            double distance = calculateDistance(location, branch);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        Log.d(TAG, "Minimum distance to branch: " + minDistance + " km");
        return minDistance <= MAX_SERVICE_DISTANCE_KM;
    }

    // Calculate distance between two points using Haversine formula
    private double calculateDistance(LatLng point1, LatLng point2) {
        final int R = 6371; // Radius of the Earth in km
        
        double latDistance = Math.toRadians(point2.latitude - point1.latitude);
        double lonDistance = Math.toRadians(point2.longitude - point1.longitude);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }

    // Show service area restriction dialog
    private void showServiceAreaDialog() {
        // Find nearest branch
        double minDistance = Double.MAX_VALUE;
        String nearestBranch = "";
        
        for (int i = 0; i < ASHCOL_BRANCHES.length; i++) {
            double distance = calculateDistance(currentCenterLatLng, ASHCOL_BRANCHES[i]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestBranch = BRANCH_NAMES[i];
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Service Not Available")
                .setMessage("Sorry, this service is not available in your selected area.\n\n" +
                           "The nearest ASHCOL branch is " + nearestBranch + " (" + 
                           String.format("%.1f", minDistance) + " km away).\n\n" +
                           "Please select a location closer to one of our service areas.")
                .setPositiveButton("View Service Areas", (dialog, which) -> {
                    showServiceAreas();
                })
                .setNegativeButton("OK", null)
                .show();
    }

    // Show service areas on map with auto-hide
    private void showServiceAreas() {
        if (mMap != null) {
            // Clear existing markers and add branch markers
            mMap.clear();
            
            for (int i = 0; i < ASHCOL_BRANCHES.length; i++) {
                com.google.android.gms.maps.model.MarkerOptions markerOptions = 
                    new com.google.android.gms.maps.model.MarkerOptions()
                        .position(ASHCOL_BRANCHES[i])
                        .title(BRANCH_NAMES[i])
                        .snippet("Service available within " + MAX_SERVICE_DISTANCE_KM + " km");
                mMap.addMarker(markerOptions);
                
                // Add service area circle
                com.google.android.gms.maps.model.CircleOptions circleOptions = 
                    new com.google.android.gms.maps.model.CircleOptions()
                        .center(ASHCOL_BRANCHES[i])
                        .radius(MAX_SERVICE_DISTANCE_KM * 1000) // Convert km to meters
                        .strokeColor(0x550000FF)
                        .fillColor(0x220000FF)
                        .strokeWidth(2);
                mMap.addCircle(circleOptions);
            }
            
            // Zoom to show all branches
            com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder = 
                new com.google.android.gms.maps.model.LatLngBounds.Builder();
            for (LatLng branch : ASHCOL_BRANCHES) {
                boundsBuilder.include(branch);
            }
            
            try {
                com.google.android.gms.maps.model.LatLngBounds bounds = boundsBuilder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } catch (Exception e) {
                Log.e(TAG, "Error showing service areas", e);
            }
            
            Toast.makeText(this, "Blue circles show our service areas", Toast.LENGTH_SHORT).show();
            
            // Auto-hide service areas after 3 seconds
            new android.os.Handler().postDelayed(() -> {
                if (mMap != null) {
                    mMap.clear();
                    // Return to normal map view
                    if (currentCenterLatLng != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCenterLatLng, 15));
                    }
                }
            }, 3000); // 3 seconds
        }
    }

    // Accurate address with REAL Plus Codes (Google's format)
    private void getAccurateAddressWithRealPlusCode(LatLng latLng) {
        tvSelectedAddress.setText("Getting precise location...");
        
        new Thread(() -> {
            try {
                // Generate the REAL Plus Code for this exact location
                String realPlusCode = generateRealPlusCode(latLng);
                
                // Try to get address but don't trust it if it's far away
                Geocoder geocoder = new Geocoder(MapSelectionActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 3);
                
                runOnUiThread(() -> {
                    String finalAddress;
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        // Check if any address is actually close to our location
                        Address closestAddress = null;
                        double minDistance = Double.MAX_VALUE;
                        
                        for (Address address : addresses) {
                            if (address.hasLatitude() && address.hasLongitude()) {
                                double distance = calculateDistance(
                                    latLng, 
                                    new LatLng(address.getLatitude(), address.getLongitude())
                                );
                                
                                if (distance < minDistance && distance < 0.1) { // Within 100 meters
                                    minDistance = distance;
                                    closestAddress = address;
                                }
                            }
                        }
                        
                        if (closestAddress != null) {
                            // Use the close address with Plus Code
                            String addressText = formatCloseAddress(closestAddress);
                            finalAddress = realPlusCode + ", " + addressText;
                        } else {
                            // Address is too far, use Plus Code with general area
                            String generalArea = getGeneralArea(addresses.get(0));
                            finalAddress = realPlusCode + ", " + generalArea;
                        }
                    } else {
                        // No address found, use Plus Code with coordinates
                        finalAddress = realPlusCode + ", Philippines";
                    }
                    
                    selectedAddress = finalAddress;
                    tvSelectedAddress.setText(selectedAddress);
                    Log.d(TAG, "Final address: " + finalAddress);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting accurate address", e);
                runOnUiThread(() -> {
                    String realPlusCode = generateRealPlusCode(latLng);
                    selectedAddress = realPlusCode + ", Philippines";
                    tvSelectedAddress.setText(selectedAddress);
                });
            }
        }).start();
    }

    // Generate accurate Plus Code using Google's official Open Location Code library
    private String generateRealPlusCode(LatLng latLng) {
        double lat = latLng.latitude;
        double lng = latLng.longitude;
        
        // Log coordinates for debugging
        Log.d(TAG, "Generating Plus Code for: " + lat + ", " + lng);
        
        try {
            // Use Google's official Open Location Code library
            com.google.openlocationcode.OpenLocationCode olc = 
                new com.google.openlocationcode.OpenLocationCode(lat, lng);
            
            String plusCode = olc.getCode();
            Log.d(TAG, "Generated Plus Code: " + plusCode);
            return plusCode;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating Plus Code", e);
            // Fallback to coordinates if library fails
            return String.format("%.6f,%.6f", lat, lng);
        }
    }

    // Format address only if it's actually close to the pin
    private String formatCloseAddress(Address address) {
        StringBuilder result = new StringBuilder();
        
        String locality = address.getLocality();
        String subLocality = address.getSubLocality();
        String adminArea = address.getAdminArea();
        
        if (subLocality != null && !subLocality.isEmpty() && 
            !subLocality.matches(".*[Ll]\\d+.*")) {
            result.append(subLocality);
        } else if (locality != null && !locality.isEmpty()) {
            result.append(locality);
        } else if (adminArea != null && !adminArea.isEmpty()) {
            result.append(adminArea);
        } else {
            result.append("Philippines");
        }
        
        return result.toString();
    }

    // Get general area when exact address is not close
    private String getGeneralArea(Address address) {
        String locality = address.getLocality();
        String adminArea = address.getAdminArea();
        
        if (locality != null && !locality.isEmpty()) {
            return locality + ", Philippines";
        } else if (adminArea != null && !adminArea.isEmpty()) {
            return adminArea + ", Philippines";
        } else {
            return "Philippines";
        }
    }
}
