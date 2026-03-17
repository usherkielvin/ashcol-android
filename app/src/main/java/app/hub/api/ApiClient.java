package app.hub.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";

    // Change this to your Laravel API URL
    // For Android emulator: http://10.0.2.2:8000/
    // For physical device: http://YOUR_COMPUTER_IP:8000/
    // Example: http://192.168.1.100:8000/
    // Note: Trailing slash is required when endpoints don't start with /
    // LOCAL DEVELOPMENT - Using local Laravel server
    // Make sure to run: php artisan serve --host=0.0.0.0 --port=8000
    private static final String BASE_URL = "http://10.25.185.250:8000/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Create logging interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create OkHttpClient with longer timeouts for debugging
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // Connection timeout: 60 seconds
                    .readTimeout(60, TimeUnit.SECONDS) // Read timeout: 60 seconds
                    .writeTimeout(60, TimeUnit.SECONDS) // Write timeout: 60 seconds
                    .retryOnConnectionFailure(true) // Auto-retry on connection failure
                    .addInterceptor(loggingInterceptor)
                    .build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            Log.d(TAG, "Retrofit client initialized with BASE_URL: " + BASE_URL);
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        Log.d(TAG, "Creating ApiService with BASE_URL: " + BASE_URL);
        return getClient().create(ApiService.class);
    }

}
