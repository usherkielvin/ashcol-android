package app.hub.common;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import app.hub.ForgotPasswordActivity;
import app.hub.R;

import app.hub.admin.AdminDashboardActivity;
import app.hub.api.ApiClient;
import app.hub.api.ApiService;
import app.hub.api.ErrorResponse;
import app.hub.api.GoogleSignInRequest;
import app.hub.api.GoogleSignInResponse;
import app.hub.api.LoginRequest;
import app.hub.api.LoginResponse;
import app.hub.employee.EmployeeDashboardActivity;
import app.hub.manager.ManagerDashboardActivity;
import app.hub.user.DashboardActivity;
import app.hub.util.FCMTokenHelper;
import app.hub.util.GoogleSignInHelper;
import app.hub.util.LocationHelper;
import app.hub.util.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;

    private TokenManager tokenManager;
    private LocationHelper locationHelper;
    private GoogleSignInHelper googleSignInHelper;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        app.hub.util.EdgeToEdgeHelper.enable(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);
        locationHelper = new LocationHelper(this);
        googleSignInHelper = new GoogleSignInHelper(this);

        // Keep the splash screen visible for this Activity
        splashScreen.setKeepOnScreenCondition(() -> {
            checkLoginStatus();
            return false;
        });

        // setupPasswordToggle(); // Handled by TextInputLayout in XML
        setupLoginButton();
        setupForgotPassword();
        setupRegisterLink();
        setupSocialLoginButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for existing Google Sign-In account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.d(TAG, getString(R.string.user_already_signed_in));
            // Optional: Auto-login or update UI logic could go here
        }
    }

    private void checkLoginStatus() {
        if (tokenManager.isLoggedIn()) {
            String role = tokenManager.getRole();
            Log.d(TAG, String.format(getString(R.string.user_logged_in_roles), role));

            // Fetch latest location on login
            updateLocationAndNavigate(role, tokenManager.getEmail());
        } else {
            Log.d(TAG, getString(R.string.user_not_logged_in));
            // Stay on MainActivity
        }
    }

    private void updateLocationAndNavigate(String role, String email) {
        Log.d(TAG, "Starting location update and navigation for role: " + role + ", email: " + email);

        navigateToDashboard(role);

        if (locationHelper.isLocationPermissionGranted()) {
            locationHelper.getCurrentLocation((Location location) -> {
                if (location != null && email != null) {
                    Log.d(TAG, "Updating location with lat: " + location.getLatitude() + ", lng: "
                            + location.getLongitude());
                    FCMTokenHelper.updateLocation(this, email, location.getLatitude(), location.getLongitude());
                } else {
                    Log.w(TAG, "Location is null or email is null, skipping location update");
                }
            });
        } else {
            Log.d(TAG, "Location permission not granted, skipping location update");
        }
    }

    private void updateLocationAndNavigate(GoogleSignInResponse.User user) {
        updateLocationAndNavigate(user.getRole(), user.getEmail());
    }

    private void navigateToDashboard(String role) {
        if (role == null) {
            // Default to user dashboard if role is missing
            Log.w(TAG, "User role is null, defaulting to customer");
            role = "customer"; // Changed from "user" to "customer" to match actual role names
        }

        Intent intent;
        switch (role.toLowerCase()) { // Added toLowerCase() to handle case sensitivity
            case "admin" -> intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
            case "manager" -> intent = new Intent(MainActivity.this, ManagerDashboardActivity.class);
            case "technician", "employee" -> // Added "employee" as alternate for technician
                intent = new Intent(MainActivity.this, EmployeeDashboardActivity.class);
            default -> intent = new Intent(MainActivity.this, DashboardActivity.class);
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        finish();
    }

    /*
     * private void setupPasswordToggle() {
     * // Handled by TextInputLayout in XML
     * }
     */

    private void setupLoginButton() {
        loginButton = findViewById(R.id.loginButton);
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> performLogin());
        }
    }

    private void setupForgotPassword() {
        TextView forgotPassword = findViewById(R.id.forgotpassbtn); // Updated ID
        if (forgotPassword != null) {
            forgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRegisterLink() {
        TextView registerLink = findViewById(R.id.registerButton); // Updated ID
        if (registerLink != null) {
            registerLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, app.hub.common.RegisterActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupSocialLoginButtons() {
        Button googleButton = findViewById(R.id.btnGoogle);
        if (googleButton != null) {
            googleButton.setOnClickListener(v -> signInWithGoogle());
        }
    }

    private void signInWithGoogle() {
        if (!isGooglePlayServicesAvailable()) {
            return;
        }

        if (loginButton != null) {
            loginButton.setEnabled(false);
            loginButton.setText(R.string.logging_in);
        }

        if (googleSignInHelper != null) {
            // Sign out first to force account picker to show
            googleSignInHelper.signOut(() -> {
                Log.d(TAG, "Signed out from Google, launching account picker");
                Intent signInIntent = googleSignInHelper.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            return true;
        }

        if (GoogleApiAvailability.getInstance().isUserResolvableError(status)) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, status, 9000).show();
        } else {
            Toast.makeText(this, "Google Play services is unavailable on this device.", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "handleGoogleSignInResult called");
        try {
            GoogleSignInAccount account = googleSignInHelper.handleSignInResult(completedTask);
            if (account != null) {
                String email = account.getEmail();
                String givenName = account.getGivenName();
                String familyName = account.getFamilyName();
                String idToken = account.getIdToken();

                Log.d(TAG, "Google Sign-In successful - Email: " + email + ", GivenName: " + givenName
                        + ", FamilyName: " + familyName);

                // Call backend API to login with Google
                loginWithGoogle(email, givenName, familyName, idToken);
            }
        } catch (ApiException e) {
            Log.e(TAG, String.format(getString(R.string.google_sign_in_failed_code), e.getStatusCode()), e);
            String errorMessage;
            switch (e.getStatusCode()) {
                case 10 -> // DEVELOPER_ERROR
                    errorMessage = getString(R.string.google_sign_in_config_error);
                case 12500 -> // SIGN_IN_FAILED
                    errorMessage = getString(R.string.google_sign_in_failed);
                case 12501 -> // SIGN_IN_CANCELLED
                    errorMessage = getString(R.string.google_sign_in_cancelled);
                case 7 -> // NETWORK_ERROR
                    errorMessage = getString(R.string.network_error);
                case 8 -> // INTERNAL_ERROR
                    errorMessage = getString(R.string.google_sign_in_internal_error);
                default ->
                    errorMessage = String.format(getString(R.string.google_sign_in_failed_code), e.getStatusCode());
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            
            // Re-enable login button after Google Sign-In failure/cancellation
            if (loginButton != null) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText(R.string.login);
                });
            }
        }
    }

    private void loginWithGoogle(String email, String firstName, String lastName, String idToken) {
        Log.d(TAG, "loginWithGoogle called - Email: " + email + ", First: " + firstName + ", Last: " + lastName);

        ApiService apiService = ApiClient.getApiService();
        GoogleSignInRequest request = new GoogleSignInRequest(
                idToken != null ? idToken : "",
                email,
                firstName != null ? firstName : "",
                lastName != null ? lastName : "",
                "" // No phone on login
        );

        Log.d(TAG, "Making API call to googleSignIn endpoint");
        Call<GoogleSignInResponse> call = apiService.googleSignIn(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<GoogleSignInResponse> call,
                    @NonNull Response<GoogleSignInResponse> response) {
                Log.d(TAG, "Google Sign-In response received - Code: " + response.code() + ", Successful: "
                        + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    GoogleSignInResponse signInResponse = response.body();
                    Log.d(TAG, "Google Sign-In response body - Success: " + signInResponse.isSuccess() + ", Message: "
                            + signInResponse.getMessage());

                    if (signInResponse.isSuccess()) {
                        Log.d(TAG, "Google Sign-In successful, handling login");
                        handleGoogleLoginSuccess(signInResponse);
                    } else {
                        // API returned success: false - treat as "account not found" and send user to registration
                        Log.w(TAG, "Google Sign-In failed - API returned success=false, redirecting to registration");
                        runOnUiThread(() -> {
                            if (loginButton != null) {
                                loginButton.setEnabled(true);
                                loginButton.setText(R.string.login);
                            }

                            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                            intent.putExtra("google_email", email);
                            intent.putExtra("google_given_name", firstName);
                            intent.putExtra("google_family_name", lastName);
                            intent.putExtra("google_display_name", buildFullName(firstName, lastName));
                            intent.putExtra("google_id_token", idToken != null ? idToken : "");
                            startActivity(intent);
                            finish();
                        });
                        signOutFromGoogle();
                    }
                } else {
                    // Handle 404 - Account not found
                    Log.w(TAG, "Google Sign-In failed - HTTP " + response.code());
                    if (response.code() == 404) {
                        Log.d(TAG, "Account not found (404), redirecting to registration flow");
                        runOnUiThread(() -> {
                            // Re-enable login button before navigating
                            if (loginButton != null) {
                                loginButton.setEnabled(true);
                                loginButton.setText(R.string.login);
                            }

                            // Launch RegisterActivity prefilled with Google data
                            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                            intent.putExtra("google_email", email);
                            intent.putExtra("google_given_name", firstName);
                            intent.putExtra("google_family_name", lastName);
                            intent.putExtra("google_display_name", buildFullName(firstName, lastName));
                            intent.putExtra("google_id_token", idToken != null ? idToken : "");
                            startActivity(intent);
                            // Optionally finish login screen so back doesn't return here
                            finish();
                        });
                        signOutFromGoogle();
                        return;
                    }

                    String errorMsg = parseGoogleLoginError(response);
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = getString(R.string.login_failed_try_again);
                    }
                    Log.e(TAG, "Google Sign-In error: " + errorMsg);
                    final String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> {
                        showError(getString(R.string.login_failed_title), finalErrorMsg);
                        
                        // Re-enable login button after Google Sign-In failure
                        if (loginButton != null) {
                            loginButton.setEnabled(true);
                            loginButton.setText(R.string.login);
                        }
                    });
                    signOutFromGoogle();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GoogleSignInResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Google Sign-In request failed", t);

                Log.e(TAG, String.format(getString(R.string.error_logging_in_google), t.getMessage()), t);
                runOnUiThread(() -> {
                    showError(getString(R.string.connection_error_title),
                        getString(R.string.connection_error_msg));
                    
                    // Re-enable login button after Google Sign-In failure
                    if (loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText(R.string.login);
                    }
                });
                signOutFromGoogle();
            }
        });
    }

    private void handleGoogleLoginSuccess(GoogleSignInResponse response) {
        Log.d(TAG, "handleGoogleLoginSuccess called");

        if (response.getData() == null || response.getData().getUser() == null) {
            Log.e(TAG, "Google login success but missing user data");
            runOnUiThread(() -> showError(getString(R.string.login_failed_title),
                    getString(R.string.invalid_server_response)));
            return;
        }

        // Save token and user data
        GoogleSignInResponse.User user = response.getData().getUser();
        Log.d(TAG, "Saving user data - Email: " + user.getEmail() + ", Role: " + user.getRole());

        tokenManager.saveToken("Bearer " + response.getData().getToken());
        tokenManager.saveUserId(user.getId());
        tokenManager.saveEmail(user.getEmail());
        tokenManager.saveRole(user.getRole());
        if (user.getBranch() != null) {
            tokenManager.saveUserBranch(user.getBranch());
        }

        // Build and save name
        String fullName = buildFullName(user.getFirstName(), user.getLastName());
        if (!TextUtils.isEmpty(fullName)) {
            tokenManager.saveName(fullName);
            Log.d(TAG, "Saved user name: " + fullName);
        }

        // Force immediate token persistence
        tokenManager.forceCommit();

        // Register FCM token for push notifications
        FCMTokenHelper.registerTokenWithBackend(MainActivity.this);

        Log.d(TAG, "Navigating to dashboard for role: " + user.getRole());
        // Update location for signed-in user and navigate after completion
        updateLocationAndNavigate(user);
    }

    /**
     * Performs login action - validates inputs and calls login method
     */
    private void performLogin() {
        TextInputEditText emailInput = findViewById(R.id.Email_val);
        TextInputEditText passwordInput = findViewById(R.id.Pass_val);

        String email = emailInput != null && emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput != null && passwordInput.getText() != null ? passwordInput.getText().toString()
                : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show();
            return;
        }

        login(email, password);
    }

    private void login(String email, String password) {
        final MaterialButton loginButton = findViewById(R.id.loginButton);
        if (loginButton != null) {
            loginButton.setEnabled(false);
            loginButton.setText(R.string.logging_in);
        }

        ApiService apiService = ApiClient.getApiService();
        LoginRequest request = new LoginRequest(email, password);

        Call<LoginResponse> call = apiService.login(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                runOnUiThread(() -> {
                    if (loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText(R.string.login);
                    }
                });

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        // Save token and user data
                        LoginResponse.User user = loginResponse.getData().getUser();
                        tokenManager.saveToken("Bearer " + loginResponse.getData().getToken());
                        tokenManager.saveUserId(user.getId());

                        // Save email or connection status
                        String email = user.getEmail();
                        if (email != null && email.contains("@")) {
                            tokenManager.saveEmail(email);
                            tokenManager.clearConnectionStatus();
                        }

                        tokenManager.saveRole(user.getRole());
                        if (user.getBranch() != null) {
                            tokenManager.saveUserBranch(user.getBranch());
                        }

                        // Get name - prefer name field, fallback to firstName + lastName
                        String userName = user.getName();
                        if (TextUtils.isEmpty(userName)) {
                            userName = buildFullName(user.getFirstName(), user.getLastName());
                        } else {
                            userName = userName.trim();
                        }

                        if (!TextUtils.isEmpty(userName)) {
                            tokenManager.saveName(userName);
                        }

                        // Register FCM token for push notifications
                        FCMTokenHelper.registerTokenWithBackend(MainActivity.this);

                        // Navigate to dashboard
                        updateLocationAndNavigate(user.getRole(), user.getEmail());
                    } else {
                        final String message = loginResponse.getMessage() != null
                                ? loginResponse.getMessage()
                                : getString(R.string.login_failed_try_again);
                        runOnUiThread(() -> showError(getString(R.string.login_failed_title), message));
                    }
                } else {
                    // Handle error response - try to parse error body
                    String errorMsg = parseErrorResponse(response);
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        if (response.code() == 401) {
                            errorMsg = getString(R.string.invalid_email_password);
                        } else if (response.code() == 422) {
                            errorMsg = getString(R.string.invalid_input_check);
                        } else if (response.code() == 500) {
                            errorMsg = getString(R.string.server_error);
                        } else {
                            errorMsg = getString(R.string.login_failed_try_again);
                        }
                    }
                    final String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> {
                        showError(getString(R.string.login_failed_title), finalErrorMsg);
                        
                        // Re-enable login button after login failure
                        if (loginButton != null) {
                            loginButton.setEnabled(true);
                            loginButton.setText(R.string.login);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    if (loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText(R.string.login);
                    }
                });

                Log.e(TAG, "Error logging in: " + t.getMessage(), t);
                runOnUiThread(() -> showError(getString(R.string.connection_error_title),
                        getString(R.string.connection_error_msg)));
            }
        });
    }

    private String parseGoogleLoginError(Response<GoogleSignInResponse> response) {
        okhttp3.ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return null;
        }
        try (okhttp3.ResponseBody ignored = errorBody) {
            String errorString = errorBody.string();
            Gson gson = new Gson();
            GoogleSignInResponse errorResponse = gson.fromJson(errorString, GoogleSignInResponse.class);

            if (errorResponse != null && errorResponse.getMessage() != null
                    && !errorResponse.getMessage().isEmpty()) {
                return errorResponse.getMessage();
            }
        } catch (IOException | JsonSyntaxException e) {
            Log.e(TAG, "Error parsing Google Sign-In error response", e);
        }

        return null;
    }

    private String parseErrorResponse(Response<LoginResponse> response) {
        okhttp3.ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return null;
        }
        try (okhttp3.ResponseBody ignored = errorBody) {
            String errorString = errorBody.string();
            Gson gson = new Gson();
            ErrorResponse errorResponse = gson.fromJson(errorString, ErrorResponse.class);
            if (errorResponse != null && errorResponse.getMessage() != null
                    && !errorResponse.getMessage().isEmpty()) {
                return errorResponse.getMessage();
            }
        } catch (IOException | JsonSyntaxException e) {
            Log.e(TAG, "Error parsing error response", e);
        }
        return null;
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder nameBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(firstName)) {
            nameBuilder.append(firstName.trim());
        }
        if (!TextUtils.isEmpty(lastName)) {
            if (!TextUtils.isEmpty(nameBuilder)) {
                nameBuilder.append(" ");
            }
            nameBuilder.append(lastName.trim());
        }
        return nameBuilder.toString();
    }

    private void showError(String title, String message) {
        // Using a simple Toast for now, can be replaced with a dialog
        // Toast has no title support by default, so we just show the message prefixed
        Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
    }

    private void signOutFromGoogle() {
        if (googleSignInHelper != null) {
            googleSignInHelper.signOut(() -> Log.d(TAG, getString(R.string.google_sign_out_complete)));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle Google Sign-In result
        if (requestCode == RC_SIGN_IN) {
            // Re-enable login button regardless of result
            if (loginButton != null) {
                loginButton.setEnabled(true);
                loginButton.setText(R.string.login);
            }

            // Check result code first
            if (resultCode != RESULT_OK) {
                if (!isGooglePlayServicesAvailable()) {
                    return;
                }
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, getString(R.string.google_sign_in_cancelled), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show();
                }

                GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(this);
                if (lastAccount != null && lastAccount.getEmail() != null) {
                    Log.d(TAG, "Using last signed-in Google account after canceled result");
                    loginWithGoogle(
                            lastAccount.getEmail(),
                            lastAccount.getGivenName(),
                            lastAccount.getFamilyName(),
                            lastAccount.getIdToken());
                    return;
                }

                return;
            }

            if (data == null) {
                Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Google Sign-In returned null data intent");
                GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(this);
                if (lastAccount != null && lastAccount.getEmail() != null) {
                    Log.d(TAG, "Using last signed-in Google account after null data");
                    loginWithGoogle(
                            lastAccount.getEmail(),
                            lastAccount.getGivenName(),
                            lastAccount.getFamilyName(),
                            lastAccount.getIdToken());
                    return;
                }
                return;
            }

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }
}
