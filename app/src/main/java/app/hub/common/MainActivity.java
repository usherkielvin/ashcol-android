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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import app.hub.ForgotPasswordActivity;
import app.hub.R;

import app.hub.admin.AdminDashboardActivity;
import app.hub.employee.EmployeeDashboardActivity;
import app.hub.manager.ManagerDashboardActivity;
import app.hub.user.DashboardActivity;
import app.hub.util.FCMTokenHelper;
import app.hub.util.LocationHelper;
import app.hub.util.TokenManager;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private TokenManager tokenManager;
    private LocationHelper locationHelper;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        app.hub.util.EdgeToEdgeHelper.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager(this);
        firestoreManager = new FirestoreManager(this);
        tokenManager = new TokenManager(this);
        locationHelper = new LocationHelper(this);

        splashScreen.setKeepOnScreenCondition(() -> {
            checkLoginStatus();
            return false;
        });

        setupLoginButton();
        setupForgotPassword();
        setupRegisterLink();
        setupSocialLoginButtons();
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User is signed in: " + currentUser.getEmail());
            // Fetch role from TokenManager or Firestore, or default to "customer"
            String role = tokenManager.getRole();
            if (role == null || role.isEmpty()) {
                role = "customer";
            }
            updateLocationAndNavigate(role, currentUser.getEmail());
        } else {
            Log.d(TAG, getString(R.string.user_not_logged_in));
        }
    }

    private void updateLocationAndNavigate(String role, String email) {
        navigateToDashboard(role);
        if (locationHelper.isLocationPermissionGranted()) {
            locationHelper.getCurrentLocation((Location location) -> {
                if (location != null && email != null) {
                    FCMTokenHelper.updateLocation(this, email, location.getLatitude(), location.getLongitude());
                }
            });
        }
    }

    private void navigateToDashboard(String role) {
        if (role == null) role = "customer";

        Intent intent;
        switch (role.toLowerCase()) {
            case "admin": intent = new Intent(MainActivity.this, AdminDashboardActivity.class); break;
            case "manager": intent = new Intent(MainActivity.this, ManagerDashboardActivity.class); break;
            case "technician":
            case "employee": intent = new Intent(MainActivity.this, EmployeeDashboardActivity.class); break;
            default: intent = new Intent(MainActivity.this, DashboardActivity.class); break;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        finish();
    }

    private void setupLoginButton() {
        loginButton = findViewById(R.id.loginButton);
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> performLogin());
        }
    }

    private void setupForgotPassword() {
        TextView forgotPassword = findViewById(R.id.forgotpassbtn);
        if (forgotPassword != null) {
            forgotPassword.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class));
            });
        }
    }

    private void setupRegisterLink() {
        TextView registerLink = findViewById(R.id.registerButton);
        if (registerLink != null) {
            registerLink.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
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
        if (!isGooglePlayServicesAvailable()) return;

        if (loginButton != null) {
            loginButton.setEnabled(false);
            loginButton.setText(R.string.logging_in);
        }

        // Launch sign in from FirebaseAuthManager
        Intent signInIntent = authManager.getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) return true;
        
        if (GoogleApiAvailability.getInstance().isUserResolvableError(status)) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, status, 9000).show();
        } else {
            Toast.makeText(this, "Google Play services is unavailable on this device.", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void performLogin() {
        TextInputEditText emailInput = findViewById(R.id.Email_val);
        TextInputEditText passwordInput = findViewById(R.id.Pass_val);

        String email = emailInput != null && emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput != null && passwordInput.getText() != null ? passwordInput.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loginButton != null) {
            loginButton.setEnabled(false);
            loginButton.setText(R.string.logging_in);
        }

        authManager.signInWithEmail(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = authManager.getCurrentUser();
                    if (user != null) {
                        tokenManager.saveEmail(user.getEmail());
                        tokenManager.saveUid(user.getUid());
                        
                        // Fetch role from Firestore
                        firestoreManager.getUserProfile(user.getUid(), new FirestoreManager.UserProfileListener() {
                            @Override
                            public void onProfileLoaded(DocumentSnapshot doc) {
                                String role = (doc != null && doc.exists()) ? doc.getString("role") : "customer";
                                if (role == null || role.isEmpty()) role = "customer";
                                
                                tokenManager.saveRole(role);
                                updateLocationAndNavigate(role, user.getEmail());
                            }

                            @Override
                            public void onError(Exception e) {
                                // Default to customer if profile fetch fails
                                String role = "customer";
                                tokenManager.saveRole(role);
                                updateLocationAndNavigate(role, user.getEmail());
                            }
                        });
                    }
                } else {
                    if (loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText(R.string.login);
                    }
                    showError(getString(R.string.login_failed_title), task.getException() != null ? task.getException().getMessage() : "Authentication failed");
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (loginButton != null) {
                loginButton.setEnabled(true);
                loginButton.setText(R.string.login);
            }

            if (resultCode != RESULT_OK) {
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, getString(R.string.google_sign_in_cancelled), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show();
                }
                return;
            }

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    authManager.signInWithGoogle(account)
                        .addOnCompleteListener(this, authTask -> {
                            if (authTask.isSuccessful()) {
                                FirebaseUser user = authManager.getCurrentUser();
                                if (user != null) {
                                    tokenManager.saveEmail(user.getEmail());
                                    tokenManager.saveUid(user.getUid());
                                    
                                    // Fetch role from Firestore
                                    firestoreManager.getUserProfile(user.getUid(), new FirestoreManager.UserProfileListener() {
                                        @Override
                                        public void onProfileLoaded(DocumentSnapshot doc) {
                                            String role = doc.getString("role");
                                            if (role == null) role = "customer";
                                            
                                            tokenManager.saveRole(role);
                                            updateLocationAndNavigate(role, user.getEmail());
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            // Default to customer if profile fetch fails
                                            String role = "customer";
                                            tokenManager.saveRole(role);
                                            updateLocationAndNavigate(role, user.getEmail());
                                        }
                                    });
                                }
                            } else {
                                showError(getString(R.string.login_failed_title), authTask.getException() != null ? authTask.getException().getMessage() : "Authentication failed");
                            }
                        });
                }
            } catch (ApiException e) {
                showError("Google Sign In Failed", "Code: " + e.getStatusCode());
            }
        }
    }

    private void showError(String title, String message) {
        Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
    }
}
