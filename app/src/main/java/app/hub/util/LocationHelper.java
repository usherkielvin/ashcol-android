package app.hub.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    private Context context;
    private ExecutorService executor;
    private Handler mainHandler;

    public interface LocationCallback {
        void onLocationReceived(String cityName);

        void onLocationError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isLocationPermissionGranted() {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public interface LocationResultCallback {
        void onLocationReceived(Location location);
    }

    public void getCurrentLocation(LocationResultCallback callback) {
        if (!isLocationPermissionGranted()) {
            mainHandler.post(() -> callback.onLocationReceived(null));
            return;
        }

        executor.execute(() -> {
            try {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager == null) {
                    mainHandler.post(() -> callback.onLocationReceived(null));
                    return;
                }

                Location location = null;
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                final Location finalLocation = location;
                mainHandler.post(() -> callback.onLocationReceived(finalLocation));

            } catch (SecurityException e) {
                Log.e(TAG, "Security exception: " + e.getMessage());
                mainHandler.post(() -> callback.onLocationReceived(null));
            } catch (Exception e) {
                Log.e(TAG, "Error getting location: " + e.getMessage());
                mainHandler.post(() -> callback.onLocationReceived(null));
            }
        });
    }

    public void getCurrentLocation(LocationCallback callback) {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            mainHandler.post(() -> callback.onLocationError("Location permission not granted"));
            return;
        }

        executor.execute(() -> {
            try {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                if (locationManager == null) {
                    mainHandler.post(() -> callback.onLocationError("Location service not available"));
                    return;
                }

                // Try to get last known location from GPS first, then Network
                Location location = null;

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }

                if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (location == null) {
                    mainHandler.post(() -> callback.onLocationError("Unable to get current location"));
                    return;
                }

                // Get city name from coordinates
                getCityFromLocation(location.getLatitude(), location.getLongitude(), callback);

            } catch (SecurityException e) {
                Log.e(TAG, "Security exception: " + e.getMessage());
                mainHandler.post(() -> callback.onLocationError("Location permission denied"));
            } catch (Exception e) {
                Log.e(TAG, "Error getting location: " + e.getMessage());
                mainHandler.post(() -> callback.onLocationError("Error getting location: " + e.getMessage()));
            }
        });
    }

    private void getCityFromLocation(double latitude, double longitude, LocationCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            if (!Geocoder.isPresent()) {
                mainHandler.post(() -> callback.onLocationError("Geocoder not available"));
                return;
            }

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Try to get city name in order of preference
                String cityName = null;

                // First try locality (city)
                if (address.getLocality() != null && !address.getLocality().isEmpty()) {
                    cityName = address.getLocality();
                }
                // Then try sub-administrative area (like district)
                else if (address.getSubAdminArea() != null && !address.getSubAdminArea().isEmpty()) {
                    cityName = address.getSubAdminArea();
                }
                // Then try administrative area (province/state)
                else if (address.getAdminArea() != null && !address.getAdminArea().isEmpty()) {
                    cityName = address.getAdminArea();
                }
                // Finally try feature name
                else if (address.getFeatureName() != null && !address.getFeatureName().isEmpty()) {
                    cityName = address.getFeatureName();
                }

                if (cityName != null) {
                    // Clean up the city name (remove numbers, special chars)
                    cityName = cleanCityName(cityName);
                    Log.d(TAG, "Location found: " + cityName);

                    final String finalCityName = cityName;
                    mainHandler.post(() -> callback.onLocationReceived(finalCityName));
                } else {
                    mainHandler.post(() -> callback.onLocationError("Could not determine city name"));
                }
            } else {
                mainHandler.post(() -> callback.onLocationError("No address found for location"));
            }

        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
            mainHandler.post(() -> callback.onLocationError("Network error getting location"));
        } catch (Exception e) {
            Log.e(TAG, "Error getting city name: " + e.getMessage());
            mainHandler.post(() -> callback.onLocationError("Error getting city name"));
        }
    }

    private String cleanCityName(String cityName) {
        if (cityName == null)
            return null;

        // Remove common prefixes/suffixes and clean up
        cityName = cityName.trim();

        // Remove numbers and special characters, keep only letters and spaces
        cityName = cityName.replaceAll("[^a-zA-Z\\s]", "");

        // Remove extra spaces
        cityName = cityName.replaceAll("\\s+", " ").trim();

        // Capitalize first letter of each word
        String[] words = cityName.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0)
                    result.append(" ");
                result.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
