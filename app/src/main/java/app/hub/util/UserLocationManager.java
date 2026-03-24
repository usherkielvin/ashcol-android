package app.hub.util;

import android.content.Context;
import android.util.Log;


public class UserLocationManager {
    private static final String TAG = "UserLocationManager";
    private Context context;
    private TokenManager tokenManager;
    private LocationHelper locationHelper;

    public interface LocationUpdateCallback {
        void onLocationUpdated(String location);
        void onLocationUpdateFailed(String error);
    }

    public UserLocationManager(Context context) {
        this.context = context;
        this.tokenManager = new TokenManager(context);
        this.locationHelper = new LocationHelper(context);
    }

    public void updateUserLocation(LocationUpdateCallback callback) {
        String token = tokenManager.getToken();
        if (token == null) {
            if (callback != null) {
                callback.onLocationUpdateFailed("User not authenticated");
            }
            return;
        }

        // Get current location
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(String cityName) {
                Log.d(TAG, "Location received: " + cityName);
                
                // Update location on server
                updateLocationOnServer(token, cityName, callback);
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
                if (callback != null) {
                    callback.onLocationUpdateFailed("Could not get location: " + error);
                }
            }
        });
    }

    private void updateLocationOnServer(String token, String location, LocationUpdateCallback callback) {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            if (callback != null) callback.onLocationUpdateFailed("User not authenticated");
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
            .document(user.getUid())
            .update("location", location)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Location updated in Firestore: " + location);
                if (callback != null) {
                    callback.onLocationUpdated(location);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Firestore update failed: " + e.getMessage());
                if (callback != null) {
                    callback.onLocationUpdateFailed("Database error: " + e.getMessage());
                }
            });
    }

    public void cleanup() {
        if (locationHelper != null) {
            locationHelper.cleanup();
        }
    }
}
