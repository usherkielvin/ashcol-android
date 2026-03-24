package app.hub.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;
import java.util.HashMap;

/**
 * Helper class to manage FCM token registration with Firestore
 */
public class FCMTokenHelper {
    private static final String TAG = "FCMTokenHelper";

    /**
     * Get FCM token and register it with Firestore
     */
    public static void registerTokenWithBackend(Context context) {
        TokenManager tokenManager = new TokenManager(context);

        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping FCM token registration");
            return;
        }

        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String fcmToken = task.getResult();

                        if (fcmToken == null || fcmToken.isEmpty()) {
                            Log.w(TAG, "FCM token is null or empty");
                            return;
                        }

                        // Save token locally
                        tokenManager.saveFCMToken(fcmToken);

                        // Send token to Firestore
                        sendTokenToBackend(context, fcmToken);
                    }
                });
    }

    /**
     * Send FCM token to Firestore
     */
    private static void sendTokenToBackend(Context context, String fcmToken) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "No Firebase user available");
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
            .document(user.getUid())
            .update("fcm_token", fcmToken)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token registered in Firestore"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to register FCM token in Firestore", e));
    }

    /**
     * Update user location on Firestore
     */
    public static void updateLocation(Context context, String email, double latitude, double longitude) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "No Firebase user available");
            return;
        }

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("location_updated_at", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("users")
            .document(user.getUid())
            .update(locationData)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated in Firestore"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update location in Firestore", e));
    }
}
