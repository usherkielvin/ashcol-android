package app.hub.employee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.imageview.ShapeableImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import app.hub.R;
import app.hub.common.MainActivity;
import app.hub.common.ProfileAboutUsFragment;
import app.hub.util.LoadingDialog;
import app.hub.util.TokenManager;
import app.hub.util.UiPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EmployeeProfileFragment extends Fragment {

    private static final long PROFILE_REFRESH_INTERVAL_MS = 15000;
    private long lastProfileFetchMs = 0L;

    private TokenManager tokenManager;
    private app.hub.common.FirebaseAuthManager authManager;
    private TextView tvName;
    private TextView tvUsername;
    private TextView tvBranch;
    private ShapeableImageView imgProfile;

    public EmployeeProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_employee_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Null check for context
        if (getContext() == null) {
            Log.e("EmployeeProfileFragment", "Context is null in onViewCreated");
            return;
        }

        tokenManager = new TokenManager(getContext());
        authManager = new app.hub.common.FirebaseAuthManager(getContext());
        UiPreferences.applyTheme(tokenManager.getThemePreference());

        // Appearance button
        View appearanceButton = view.findViewById(R.id.btn_appearance);
        if (appearanceButton != null) {
            appearanceButton.setOnClickListener(v -> showThemeToggler());
        }

        // Bind UI Elements with null checks
        tvName = view.findViewById(R.id.tv_name);
        tvUsername = view.findViewById(R.id.tv_username);
        tvBranch = view.findViewById(R.id.tv_branch);
        imgProfile = view.findViewById(R.id.img_profile);

        loadCachedProfileImage();
        loadProfile();

        // Button Listeners
        View notificationButton = view.findViewById(R.id.btn_notifications);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> showNotificationSettings());
        }

        // Language button
        View languageButton = view.findViewById(R.id.btn_language);
        if (languageButton != null) {
            languageButton.setOnClickListener(v -> showLanguageToggler());
        }

        View passwordPrivacyButton = view.findViewById(R.id.btn_password_privacy);
        if (passwordPrivacyButton != null) {
            passwordPrivacyButton.setOnClickListener(v -> navigateToFragment(new app.hub.user.ChangePasswordFragment()));
        }

        View personalInfoButton = view.findViewById(R.id.btn_personal_info);
        if (personalInfoButton != null) {
            personalInfoButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), app.hub.common.PersonalInfoActivity.class);
                startActivity(intent);
            });
        }

        View jobHistoryButton = view.findViewById(R.id.btn_help);
        if (jobHistoryButton != null) {
            jobHistoryButton.setOnClickListener(v -> navigateToFragment(new EmployeeJobHistoryFragment()));
        }

        View aboutUsButton = view.findViewById(R.id.btn_about_us);
        if (aboutUsButton != null) {
            aboutUsButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), app.hub.common.AboutUsActivity.class);
                startActivity(intent);
            });
        }

        setupPlaceholderButton(view, R.id.btn_edit_photo, "Edit Photo");

        View logoutButton = view.findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCachedProfileImage();
        loadProfile();
    }

    private void setupPlaceholderButton(View view, int id, String featureName) {
        View btn = view.findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> Toast
                    .makeText(getContext(), featureName + " feature coming soon!", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadProfile() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        lastProfileFetchMs = System.currentTimeMillis();

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded() || !documentSnapshot.exists()) {
                    return;
                }

                String firstName = documentSnapshot.getString("firstName");
                String lastName = documentSnapshot.getString("lastName");
                String name = documentSnapshot.getString("name");
                if (name == null || name.trim().isEmpty()) {
                    name = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                    name = name.trim();
                }

                String email = documentSnapshot.getString("email");
                String branch = documentSnapshot.getString("branch");
                String profilePhoto = documentSnapshot.getString("profilePhoto");

                if (tvName != null) {
                    tvName.setText(!name.isEmpty() ? name : "Unknown User");
                }

                if (tvUsername != null) {
                    tvUsername.setText(email != null ? email : "No email");
                }

                if (tvBranch != null) {
                    branch = branch != null ? branch : tokenManager.getCachedBranch();
                    if (branch != null && !branch.isEmpty()) {
                        tvBranch.setText("Technician | " + branch);
                        tvBranch.setVisibility(View.VISIBLE);
                    } else {
                        tvBranch.setText("Technician | ASHCOL");
                        tvBranch.setVisibility(View.VISIBLE);
                    }
                }

                if (imgProfile != null && profilePhoto != null && !profilePhoto.isEmpty()) {
                    loadProfileImageFromUrl(profilePhoto);
                }
            })
            .addOnFailureListener(e -> {
                // Keep UI stable on failure.
            });
    }

    private boolean shouldRefreshProfile() {
        return System.currentTimeMillis() - lastProfileFetchMs > PROFILE_REFRESH_INTERVAL_MS;
    }

    private void loadProfileImageFromUrl(String url) {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    if (bitmap != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (imgProfile != null) {
                                imgProfile.setImageBitmap(bitmap);
                            }
                        });
                        saveProfileImage(bitmap);
                    }
                    input.close();
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void saveProfileImage(android.graphics.Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(imageFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception ignored) {
        }
    }

    private void loadCachedProfileImage() {
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            if (imageFile.exists() && imgProfile != null) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void navigateToFragment(Fragment fragment) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showChangePasswordDialog() {
        if (getContext() == null)
            return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordInput);
        TextInputLayout currentPasswordLayout = dialogView.findViewById(R.id.currentPasswordLayout);
        TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmPasswordLayout = dialogView.findViewById(R.id.confirmPasswordLayout);

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle(getString(R.string.change_password))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), null)
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String currentPassword = currentPasswordInput != null ? currentPasswordInput.getText().toString() : "";
                String newPassword = newPasswordInput != null ? newPasswordInput.getText().toString() : "";
                String confirmPassword = confirmPasswordInput != null ? confirmPasswordInput.getText().toString() : "";

                if (currentPasswordLayout != null)
                    currentPasswordLayout.setError(null);
                if (newPasswordLayout != null)
                    newPasswordLayout.setError(null);
                if (confirmPasswordLayout != null)
                    confirmPasswordLayout.setError(null);

                if (validatePasswordInputs(currentPassword, newPassword, confirmPassword,
                        currentPasswordLayout, newPasswordLayout, confirmPasswordLayout)) {
                    changePassword(currentPassword, newPassword);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private boolean validatePasswordInputs(String currentPassword, String newPassword, String confirmPassword,
            TextInputLayout currentPasswordLayout, TextInputLayout newPasswordLayout,
            TextInputLayout confirmPasswordLayout) {
        boolean isValid = true;

        if (currentPassword.isEmpty()) {
            if (currentPasswordLayout != null) {
                currentPasswordLayout.setError("Current password is required");
            }
            isValid = false;
        }

        if (newPassword.isEmpty()) {
            if (newPasswordLayout != null) {
                newPasswordLayout.setError("New password is required");
            }
            isValid = false;
        } else if (newPassword.length() < 8) {
            if (newPasswordLayout != null) {
                newPasswordLayout.setError("Password must be at least 8 characters");
            }
            isValid = false;
        }

        if (!newPassword.equals(confirmPassword)) {
            if (confirmPasswordLayout != null) {
                confirmPasswordLayout.setError("Passwords do not match");
            }
            isValid = false;
        }

        return isValid;
    }

    private void changePassword(String currentPassword, String newPassword) {
        // Null safety checks
        if (!isAdded() || getContext() == null) {
            Log.w("EmployeeProfileFragment", "Fragment not attached or context is null");
            return;
        }
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(getContext(), "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        LoadingDialog loadingDialog = new LoadingDialog(requireContext());
        loadingDialog.show();

        // Re-authenticate user before changing password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                // Change password
                user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid1 -> {
                        if (isAdded()) {
                            loadingDialog.dismiss();
                            Toast.makeText(getContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            loadingDialog.dismiss();
                            Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    loadingDialog.dismiss();
                    Toast.makeText(getContext(), "Re-authentication failed. Please check your current password.", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void showNotificationSettings() {
        if (getContext() == null)
            return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                requireContext());
        View view = getLayoutInflater().inflate(R.layout.user_notificationstoggler, null);

        com.google.android.material.switchmaterial.SwitchMaterial switchPush = view.findViewById(R.id.switch_push);
        com.google.android.material.switchmaterial.SwitchMaterial switchEmail = view.findViewById(R.id.switch_email);
        com.google.android.material.switchmaterial.SwitchMaterial switchSms = view.findViewById(R.id.switch_sms);

        if (switchPush != null) {
            switchPush.setChecked(tokenManager.isPushEnabled());
            switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> tokenManager.setPushEnabled(isChecked));
        }

        if (switchEmail != null) {
            switchEmail.setChecked(tokenManager.isEmailNotifEnabled());
            switchEmail.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> tokenManager.setEmailNotifEnabled(isChecked));
        }

        if (switchSms != null) {
            switchSms.setChecked(tokenManager.isSmsNotifEnabled());
            switchSms.setOnCheckedChangeListener((buttonView, isChecked) -> tokenManager.setSmsNotifEnabled(isChecked));
        }

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void logout() {
        Log.d("EmployeeProfileFragment", "logout() method called");
        
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            Log.w("EmployeeProfileFragment", "Fragment not attached or activity is null, cannot logout");
            return;
        }

        // Show loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(requireContext());
        loadingDialog.show();

        Log.d("EmployeeProfileFragment", "Clearing user data immediately");
        // Clear user data immediately (this is the most important part)
        clearUserData();

        try {
            if (authManager != null) {
                authManager.signOut();
            } else {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            }
        } catch (Exception e) {
            Log.e("EmployeeProfileFragment", "Error signing out of Firebase: " + e.getMessage());
        }

        loadingDialog.dismiss();
        performFinalCleanup();
    }
    
    private void dismissProgressDialog(android.app.ProgressDialog progressDialog) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            Log.w("EmployeeProfileFragment", "Error dismissing progress dialog: " + e.getMessage());
        }
    }

    private void performFinalCleanup() {
        Log.d("EmployeeProfileFragment", "performFinalCleanup() called");

        try {
            Log.d("EmployeeProfileFragment", "Starting Google sign out");
            // Sign out from Google (this can be slow, but we'll do it synchronously with
            // timeout)
            signOutFromGoogle();

            Log.d("EmployeeProfileFragment", "Starting navigation to login");
            // Navigate to login
            navigateToLogin();
        } catch (Exception e) {
            Log.e("EmployeeProfileFragment", "Error during final cleanup: " + e.getMessage(), e);
            // Still navigate to login even if cleanup fails
            try {
                navigateToLogin();
            } catch (Exception navException) {
                Log.e("EmployeeProfileFragment",
                        "Error during navigation after cleanup failure: " + navException.getMessage(), navException);
            }
        }

        Log.d("EmployeeProfileFragment", "performFinalCleanup() completed");
    }

    private void signOutFromGoogle() {
        Log.d("EmployeeProfileFragment", "signOutFromGoogle called");

        try {
            if (getActivity() != null) {
                Log.d("EmployeeProfileFragment", "Setting up Google Sign-In client");
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestProfile()
                        .build();
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

                Log.d("EmployeeProfileFragment", "Starting Google sign out with timeout");

                // Perform sign out with timeout
                java.util.concurrent.CompletableFuture<Void> signOutFuture = java.util.concurrent.CompletableFuture
                        .runAsync(() -> {
                            try {
                                Log.d("EmployeeProfileFragment", "Executing Google signOut()");
                                googleSignInClient.signOut().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("EmployeeProfileFragment", "Google sign out successful");
                                    } else {
                                        Log.w("EmployeeProfileFragment",
                                                "Google sign out failed: " + task.getException());
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.w("EmployeeProfileFragment", "Google sign out error: " + e.getMessage());
                                });
                            } catch (Exception e) {
                                Log.w("EmployeeProfileFragment", "Google sign out exception: " + e.getMessage());
                            }
                        });

                // Wait for sign out with timeout (don't block forever)
                try {
                    Log.d("EmployeeProfileFragment", "Waiting for Google sign out (3 second timeout)");
                    signOutFuture.get(3, java.util.concurrent.TimeUnit.SECONDS);
                    Log.d("EmployeeProfileFragment", "Google sign out completed within timeout");
                } catch (java.util.concurrent.TimeoutException e) {
                    Log.w("EmployeeProfileFragment", "Google sign out timed out");
                } catch (Exception e) {
                    Log.w("EmployeeProfileFragment", "Google sign out interrupted: " + e.getMessage());
                }
            } else {
                Log.w("EmployeeProfileFragment", "Activity is null, skipping Google sign out");
            }
        } catch (Exception e) {
            Log.e("EmployeeProfileFragment", "Error during Google sign out: " + e.getMessage(), e);
        }

        Log.d("EmployeeProfileFragment", "signOutFromGoogle completed");
    }

    private void navigateToLogin() {
        Log.d("EmployeeProfileFragment", "navigateToLogin called");

        // Check if fragment is still attached and has activity
        if (!isAdded() || getActivity() == null) {
            Log.w("EmployeeProfileFragment", "Fragment not attached or activity is null, cannot navigate to login");
            return;
        }

        try {
            Log.d("EmployeeProfileFragment", "Creating intent to MainActivity");
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.d("EmployeeProfileFragment", "Starting MainActivity");
            startActivity(intent);
            Log.d("EmployeeProfileFragment", "Finishing current activity");
            getActivity().finish();
            Log.d("EmployeeProfileFragment", "Navigation completed");
        } catch (Exception e) {
            Log.e("EmployeeProfileFragment", "Error navigating to login: " + e.getMessage(), e);
            // If navigation fails, at least clear the activity stack
            try {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            } catch (Exception finishException) {
                Log.e("EmployeeProfileFragment", "Error finishing activity: " + finishException.getMessage(),
                        finishException);
            }
        }
    }

    private void clearUserData() {
        try {
            Log.d("EmployeeProfileFragment", "Starting clearUserData");

            // Clear token manager data
            File imageFile = tokenManager != null ? tokenManager.getProfileImageFile(requireContext())
                    : new File(requireContext().getFilesDir(), "profile_image.jpg");

            if (tokenManager != null) {
                Log.d("EmployeeProfileFragment", "Clearing token manager data");
                tokenManager.clear();
                Log.d("EmployeeProfileFragment", "Token manager cleared");
            }

            // Delete locally stored profile photo
            if (imageFile.exists()) {
                Log.d("EmployeeProfileFragment", "Deleting profile image file");
                imageFile.delete();
                Log.d("EmployeeProfileFragment", "Profile image file deleted");
            }

            Log.d("EmployeeProfileFragment", "clearUserData completed successfully");
        } catch (Exception e) {
            Log.e("EmployeeProfileFragment", "Error clearing user data: " + e.getMessage(), e);
            // Continue with logout even if clearing data fails
        }
    }

    private void showThemeToggler() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.profile_themetoggler, null);
        
        // Set up radio buttons
        android.widget.RadioGroup radioGroup = view.findViewById(R.id.radioGroupTheme);
        android.widget.RadioButton rbLight = view.findViewById(R.id.rbLight);
        android.widget.RadioButton rbDark = view.findViewById(R.id.rbDark);
        android.widget.RadioButton rbSystem = view.findViewById(R.id.rbSystem);
        
        // Load current theme preference
        String currentTheme = tokenManager.getThemePreference();
        if ("light".equals(currentTheme)) {
            rbLight.setChecked(true);
        } else if ("dark".equals(currentTheme)) {
            rbDark.setChecked(true);
        } else {
            rbSystem.setChecked(true);
        }
        
        // Handle theme selection
        if (radioGroup != null) {
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                String selectedTheme = "system";
                if (checkedId == R.id.rbLight) {
                    selectedTheme = "light";
                } else if (checkedId == R.id.rbDark) {
                    selectedTheme = "dark";
                }
                
                tokenManager.setThemePreference(selectedTheme);
                UiPreferences.applyTheme(selectedTheme);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
                Toast.makeText(getContext(), "Theme updated to " + selectedTheme, Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            });
        }
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showLanguageToggler() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.profile_languagetoggler, null);
        
        // Set up radio buttons
        android.widget.RadioGroup radioGroup = view.findViewById(R.id.radioGroupLanguage);
        android.widget.RadioButton rbCebuano = view.findViewById(R.id.rbCebuano);
        android.widget.RadioButton rbFilipino = view.findViewById(R.id.rbFilipino);
        android.widget.RadioButton rbEnglishUS = view.findViewById(R.id.rbEnglishUS);
        android.widget.RadioButton rbEnglishUK = view.findViewById(R.id.rbEnglishUK);
        
        // Load current language preference
        String currentLanguage = tokenManager.getLanguagePreference();
        if ("cebuano".equals(currentLanguage)) {
            rbCebuano.setChecked(true);
        } else if ("filipino".equals(currentLanguage)) {
            rbFilipino.setChecked(true);
        } else if ("english_us".equals(currentLanguage)) {
            rbEnglishUS.setChecked(true);
        } else if ("english_uk".equals(currentLanguage)) {
            rbEnglishUK.setChecked(true);
        } else {
            rbFilipino.setChecked(true); // Default
        }
        
        // Handle language selection
        if (radioGroup != null) {
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                String selectedLanguage = "filipino";
                if (checkedId == R.id.rbCebuano) {
                    selectedLanguage = "cebuano";
                } else if (checkedId == R.id.rbFilipino) {
                    selectedLanguage = "filipino";
                } else if (checkedId == R.id.rbEnglishUS) {
                    selectedLanguage = "english_us";
                } else if (checkedId == R.id.rbEnglishUK) {
                    selectedLanguage = "english_uk";
                }
                
                tokenManager.setLanguagePreference(selectedLanguage);
                Toast.makeText(getContext(), "Language updated", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            });
        }
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showLogoutConfirmation() {
        if (getContext() == null) return;

        // Create overlay view
        View overlayView = getLayoutInflater().inflate(R.layout.logout_accval, null);
        
        // Create dialog with transparent background
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(overlayView)
                .setCancelable(true)
                .create();
        
        // Make dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Set up buttons
        android.widget.TextView btnConfirmLogout = overlayView.findViewById(R.id.btnConfirmLogout);
        android.widget.TextView btnCancelLogout = overlayView.findViewById(R.id.btnCancelLogout);
        
        if (btnConfirmLogout != null) {
            btnConfirmLogout.setOnClickListener(v -> {
                dialog.dismiss();
                logout();
            });
        }
        
        if (btnCancelLogout != null) {
            btnCancelLogout.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Handle background click to dismiss
        overlayView.setOnClickListener(v -> dialog.dismiss());
        
        // Prevent clicks on the content from dismissing
        View contentView = overlayView.findViewById(R.id.LogoutTitle);
        if (contentView != null && contentView.getParent() instanceof View) {
            ((View) contentView.getParent()).setOnClickListener(v -> {
                // Do nothing - prevent dismissal
            });
        }
        
        dialog.show();
    }
}
