package app.hub;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.squareup.picasso.Picasso;

public class ServiceHubApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Enable Firestore offline persistence
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        firestore.setFirestoreSettings(settings);
        
        // Enable Picasso logging for debugging image loading issues
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.listener((picasso, uri, exception) -> {
            android.util.Log.e("Picasso", "Failed to load image: " + uri, exception);
        });
        
        // Enable indicators to show where images are loaded from (red=network, blue=disk, green=memory)
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        
        try {
            Picasso.setSingletonInstance(built);
        } catch (IllegalStateException ignored) {
            // Picasso instance was already set
        }
    }
}
