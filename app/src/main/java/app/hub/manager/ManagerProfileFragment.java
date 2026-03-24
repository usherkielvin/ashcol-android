package app.hub.manager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import app.hub.R;
import app.hub.common.MainActivity;
import app.hub.employee.EmployeePersonalInfoFragment;
import app.hub.util.LoadingDialog;
import app.hub.util.TokenManager;
import app.hub.util.UiPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import app.hub.api.LogoutResponse;
import app.hub.api.UserResponse;

public class ManagerProfileFragment extends Fragment {

    private static final long PROFILE_REFRESH_INTERVAL_MS = 15000;
    private long lastProfileFetchMs = 0L;

    private TokenManager tokenManager;
    private app.hub.common.FirebaseAuthManager authManager;
    private TextView tvName;
    private TextView tvUsername;
    private TextView tvManagerRole;
    private ShapeableImageView imgProfile;

    public ManagerProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tokenManager = new TokenManager(requireContext());
        authManager = new app.hub.common.FirebaseAuthManager(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manager_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initializeViews(view);

        // Load user data
        loadUserData();

        // Setup click listeners for all buttons
        setupClickListeners(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileImage();
        fetchUserData();
    }

    private void initializeViews(View view) {
        tvName = view.findViewById(R.id.tv_name);
        tvUsername = view.findViewById(R.id.tv_username);
        tvManagerRole = view.findViewById(R.id.tv_manager_role);
        imgProfile = view.findViewById(R.id.img_profile);
    }

    private void loadUserData() {
        loadCachedUserData();
        loadProfileImage();
        fetchUserData();
        UiPreferences.applyTheme(tokenManager.getThemePreference());
    }

    private void loadCachedUserData() {
        String name = tokenManager.getName();
        String email = tokenManager.getEmail();
        String role = tokenManager.getRole();

        if (tvName != null) {
            if (name != null && !name.isEmpty()) {
                tvName.setText(name);
            } else {
                tvName.setText("Manager");
            }
        }

        if (tvUsername != null) {
            if (email != null && !email.isEmpty()) {
                tvUsername.setText(email);
            } else {
                tvUsername.setText("manager@ashcol.com");
            }
        }

        if (tvManagerRole != null) {
            String branchName = ManagerDataManager.getCachedBranchName();
            if (branchName != null && !branchName.isEmpty() && !branchName.equals("No Branch Assigned")) {
                tvManagerRole.setText("Manager of " + branchName);
            } else {
                tvManagerRole.setText("Branch Manager");
            }
        }

        Log.d("ManagerProfile", "Loaded cached data - Name: " + name + ", Email: " + email + ", Role: " + role);
    }

    private void fetchUserData() {
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
                    tvName.setText(!name.isEmpty() ? name : "Manager");
                }

                if (tvUsername != null) {
                    tvUsername.setText(email != null && !email.isEmpty() ? email : "manager@ashcol.com");
                }

                if (tvManagerRole != null) {
                    if (branch != null && !branch.trim().isEmpty()) {
                        tvManagerRole.setText("Manager of " + branch);
                    } else {
                        tvManagerRole.setText("Branch Manager");
                    }
                }

                if (!name.isEmpty()) {
                    tokenManager.saveName(name);
                }
                if (email != null && !email.isEmpty()) {
                    tokenManager.saveEmail(email);
                }

                if (profilePhoto != null && !profilePhoto.isEmpty()) {
                    loadProfileImageFromUrl(profilePhoto);
                } else {
                    if (imgProfile != null) {
                        imgProfile.setImageResource(R.mipmap.ic_launchericons_round);
                    }
                    clearCachedProfileImage();
                }
            })
            .addOnFailureListener(e -> {
                // Keep cached UI on failure.
            });
    }

    private boolean shouldRefreshProfile() {
        return System.currentTimeMillis() - lastProfileFetchMs > PROFILE_REFRESH_INTERVAL_MS;
    }

    private void loadProfileImage() {
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            if (imageFile.exists() && imgProfile != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void loadProfileImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || imgProfile == null) {
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (bitmap != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (imgProfile != null) {
                            imgProfile.setImageBitmap(bitmap);
                            saveProfileImage(bitmap);
                        }
                    });
                }
            } catch (Exception e) {
                loadProfileImage();
            }
        }).start();
    }

    private void saveProfileImage(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            // Ignore cache write errors.
        }
    }

    private void clearCachedProfileImage() {
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            if (imageFile.exists()) {
                imageFile.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private void setupClickListeners(View view) {
        // Edit Photo Button (this one is actually a MaterialButton)
        MaterialButton editPhotoButton = view.findViewById(R.id.btn_edit_photo);
        if (editPhotoButton != null) {
            editPhotoButton.setOnClickListener(v -> showEditPhotoOptions());
        }

        // Appearance Button (LinearLayout)
        View appearanceButton = view.findViewById(R.id.btn_appearance);
        if (appearanceButton != null) {
            appearanceButton.setOnClickListener(v -> showThemeToggler());
        }

        // Notifications Button (LinearLayout)
        View notificationsButton = view.findViewById(R.id.btn_notifications);
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> showNotificationSettings());
        }

        // Language Button (LinearLayout)
        View languageButton = view.findViewById(R.id.btn_language);
        if (languageButton != null) {
            languageButton.setOnClickListener(v -> showLanguageToggler());
        }

        // Personal Info Button (LinearLayout)
        View personalInfoButton = view.findViewById(R.id.btn_personal_info);
        if (personalInfoButton != null) {
            personalInfoButton.setOnClickListener(v -> showPersonalInfo());
        }

        // Password/Privacy Button (LinearLayout)
        View passwordPrivacyButton = view.findViewById(R.id.btn_password_privacy);
        if (passwordPrivacyButton != null) {
            passwordPrivacyButton.setOnClickListener(v -> navigateToChangePassword());
        }

        // Payroll Button (LinearLayout)
        View payrollButton = view.findViewById(R.id.btn_payroll);
        if (payrollButton != null) {
            payrollButton.setOnClickListener(v -> showPayrollInfo());
        }

        // About Us Button (LinearLayout)
        View aboutUsButton = view.findViewById(R.id.btn_help);
        if (aboutUsButton != null) {
            aboutUsButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), app.hub.common.AboutUsActivity.class);
                startActivity(intent);
            });
        }

        // Logout Button (LinearLayout)
        View logoutButton = view.findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void showEditPhotoOptions() {
        if (getContext() == null)
            return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Change Profile Photo")
                .setItems(new String[] { "Take Photo", "Choose from Gallery", "Remove Photo" }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(getContext(), "Camera feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(getContext(), "Gallery feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(getContext(), "Remove photo feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private void showPersonalInfo() {
        Intent intent = new Intent(getActivity(), app.hub.common.PersonalInfoActivity.class);
        startActivity(intent);
    }

    private void navigateToFragment(Fragment fragment) {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showPayrollInfo() {
        if (getContext() == null)
            return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Payroll Information")
                .setMessage("Payroll and compensation details will be available in a future update.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHelpAndFeedback() {
        if (getContext() == null)
            return;

        String[] options = { "📞 Contact Support", "💬 Send Feedback", "🐛 Report a Bug", "❓ FAQ", "📖 User Guide" };

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Help & Feedback")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Contact Support
                            showContactSupport();
                            break;
                        case 1: // Send Feedback
                            Toast.makeText(getContext(), "Feedback feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 2: // Report Bug
                            Toast.makeText(getContext(), "Bug reporting feature coming soon", Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case 3: // FAQ
                            showFAQ();
                            break;
                        case 4: // User Guide
                            Toast.makeText(getContext(), "User guide coming soon", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showContactSupport() {
        if (getContext() == null)
            return;

        String supportInfo = "📞 ASHCOL SUPPORT\n\n" +
                "📧 Email: support@ashcol.com\n" +
                "📱 Phone: +63 (2) 8123-4567\n" +
                "🕒 Hours: Mon-Fri 8AM-6PM\n" +
                "🕒 Sat: 8AM-12PM\n\n" +
                "For urgent technical issues, please call our hotline.";

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Contact Support")
                .setMessage(supportInfo)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showFAQ() {
        if (getContext() == null)
            return;

        String faqContent = "❓ FREQUENTLY ASKED QUESTIONS\n\n" +
            "Q: How do I assign tickets to technicians?\n" +
            "A: Go to Work tab → Select ticket → Assign Technician\n\n" +
            "Q: How do I view technician performance?\n" +
            "A: Go to Technician tab → Select technician → View Details\n\n" +
                "Q: How do I generate reports?\n" +
                "A: Go to Records tab → Select date range → Generate\n\n" +
                "Q: How do I change my password?\n" +
                "A: Profile tab → Password & Privacy → Change Password\n\n" +
                "For more help, contact support.";

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("FAQ")
                .setMessage(faqContent)
                .setPositiveButton("OK", null)
                .show();
    }

    private void navigateToChangePassword() {
        if (getContext() == null)
            return;

        showChangePasswordDialog();
    }

    private void showChangePasswordDialog() {
        if (getContext() == null)
            return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        com.google.android.material.textfield.TextInputEditText currentPasswordInput = dialogView
                .findViewById(R.id.currentPasswordInput);
        com.google.android.material.textfield.TextInputEditText newPasswordInput = dialogView
                .findViewById(R.id.newPasswordInput);
        com.google.android.material.textfield.TextInputEditText confirmPasswordInput = dialogView
                .findViewById(R.id.confirmPasswordInput);
        com.google.android.material.textfield.TextInputLayout currentPasswordLayout = dialogView
                .findViewById(R.id.currentPasswordLayout);
        com.google.android.material.textfield.TextInputLayout newPasswordLayout = dialogView
                .findViewById(R.id.newPasswordLayout);
        com.google.android.material.textfield.TextInputLayout confirmPasswordLayout = dialogView
                .findViewById(R.id.confirmPasswordLayout);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String currentPassword = currentPasswordInput != null ? currentPasswordInput.getText().toString() : "";
                String newPassword = newPasswordInput != null ? newPasswordInput.getText().toString() : "";
                String confirmPassword = confirmPasswordInput != null ? confirmPasswordInput.getText().toString() : "";

                // Clear previous errors
                if (currentPasswordLayout != null)
                    currentPasswordLayout.setError(null);
                if (newPasswordLayout != null)
                    newPasswordLayout.setError(null);
                if (confirmPasswordLayout != null)
                    confirmPasswordLayout.setError(null);

                // Validate inputs
                boolean isValid = true;

                if (currentPassword.isEmpty()) {
                    if (currentPasswordLayout != null)
                        currentPasswordLayout.setError("Current password is required");
                    isValid = false;
                }

                if (newPassword.isEmpty()) {
                    if (newPasswordLayout != null)
                        newPasswordLayout.setError("New password is required");
                    isValid = false;
                } else if (newPassword.length() < 6) {
                    if (newPasswordLayout != null)
                        newPasswordLayout.setError("Password must be at least 6 characters");
                    isValid = false;
                }

                if (confirmPassword.isEmpty()) {
                    if (confirmPasswordLayout != null)
                        confirmPasswordLayout.setError("Please confirm your new password");
                    isValid = false;
                } else if (!newPassword.equals(confirmPassword)) {
                    if (confirmPasswordLayout != null)
                        confirmPasswordLayout.setError("Passwords do not match");
                    isValid = false;
                }

                if (isValid) {
                    // TODO: Implement actual password change API call
                    Toast.makeText(getContext(), "Password change functionality will be implemented soon",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void logout() {
        // Show loading dialog
        if (getActivity() == null)
            return;

        LoadingDialog loadingDialog = new LoadingDialog(requireContext());
        loadingDialog.show();

        try {
            if (authManager != null) {
                authManager.signOut();
            } else {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            }
        } catch (Exception e) {
            Log.e("ManagerProfileFragment", "Error signing out of Firebase: " + e.getMessage());
        }

        loadingDialog.dismiss();
        performLogoutCleanup();
    }

    private void performLogoutCleanup() {
        // Run cleanup operations in background to prevent blocking UI
        new Thread(() -> {
            try {
                // Clear user data first (fast operation)
                clearUserData();

                // Sign out from Google (this can be slow)
                signOutFromGoogle();

                // Navigate to login on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::navigateToLogin);
                }
            } catch (Exception e) {
                Log.e("ManagerProfileFragment", "Error during logout cleanup: " + e.getMessage(), e);
                // Still navigate to login even if cleanup fails
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::navigateToLogin);
                }
            }
        }).start();
    }

    private void signOutFromGoogle() {
        try {
            if (getActivity() != null) {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestProfile()
                        .build();
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

                // Perform sign out with timeout
                java.util.concurrent.CompletableFuture<Void> signOutFuture = java.util.concurrent.CompletableFuture
                        .runAsync(() -> {
                            try {
                                googleSignInClient.signOut().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("ManagerProfileFragment", "Google sign out successful");
                                    } else {
                                        Log.w("ManagerProfileFragment",
                                                "Google sign out failed: " + task.getException());
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.w("ManagerProfileFragment", "Google sign out error: " + e.getMessage());
                                });
                            } catch (Exception e) {
                                Log.w("ManagerProfileFragment", "Google sign out exception: " + e.getMessage());
                            }
                        });

                // Wait for sign out with timeout (don't block forever)
                try {
                    signOutFuture.get(3, java.util.concurrent.TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    Log.w("ManagerProfileFragment", "Google sign out timed out");
                } catch (Exception e) {
                    Log.w("ManagerProfileFragment", "Google sign out interrupted: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("ManagerProfileFragment", "Error during Google sign out: " + e.getMessage(), e);
        }
    }

    private void navigateToLogin() {
        if (getActivity() == null)
            return;
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void clearUserData() {
        File imageFile = tokenManager.getProfileImageFile(requireContext());

        // Clear token manager data
        tokenManager.clear();

        // Delete locally stored profile photo
        try {
            if (imageFile.exists()) {
                imageFile.delete();
            }
        } catch (Exception e) {
            // Ignore errors when clearing profile photo
        }
    }

    private void showLogoutConfirmation() {
        if (getContext() == null) return;

        // Create overlay view
        View overlayView = getLayoutInflater().inflate(R.layout.logout_accval, null);
        
        // Create dialog with transparent background
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
