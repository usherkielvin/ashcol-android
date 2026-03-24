package app.hub.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import app.hub.R;
import app.hub.api.LogoutResponse;
import app.hub.api.ProfilePhotoResponse;
import app.hub.api.UserResponse;
import app.hub.common.AboutUsActivity;
import app.hub.common.MainActivity;
import app.hub.common.PersonalInfoActivity;
import app.hub.common.ProfileAboutUsFragment;
import app.hub.employee.EmployeePersonalInfoFragment;
import app.hub.util.LoadingDialog;
import app.hub.util.TokenManager;
import app.hub.util.UiPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import app.hub.common.FirestoreManager;

public class UserProfileFragment extends Fragment {
    private static final String TAG = "UserProfileFragment";

    private TokenManager tokenManager;
    private FirestoreManager firestoreManager;
    private app.hub.common.FirebaseAuthManager authManager;

    private String currentName;
    private String currentEmail;
    private String currentBranch;
    private String connectionStatus;
    private TextView tvName, tvUsername, tvBranch;
    private ShapeableImageView imgProfile;
    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user__profile, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLaunchers();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());
        firestoreManager = new FirestoreManager(requireContext());
        authManager = new app.hub.common.FirebaseAuthManager(requireContext());
        UiPreferences.applyTheme(tokenManager.getThemePreference());
        initializeViews(view);
        loadCachedData();
        loadProfileImage();

        // Start real-time listener
        firestoreManager.listenToUserProfile(new FirestoreManager.UserProfileListener() {
            @Override
            public void onProfileUpdated(UserResponse.Data profile) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        processUserData(profile);
                        updateUI();
                        // Also update token manager with latest data
                        if (profile.getName() != null)
                            tokenManager.saveName(profile.getName());
                        if (profile.getBranch() != null)
                            tokenManager.saveBranchInfo(profile.getBranch(), 0);
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Firestore listen error: " + e.getMessage());
                // Fallback to API if Firestore fails
                fetchUserData();
            }
        });

        setupClickListeners(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileImage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreManager != null) {
            firestoreManager.stopListening();
        }
    }

    private void initializeLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            setProfileImage(selectedImage);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && cameraImageUri != null) {
                        setProfileImage(cameraImageUri);
                    }
                });
    }

    private void initializeViews(View view) {
        tvName = view.findViewById(R.id.tv_name);
        tvUsername = view.findViewById(R.id.tv_username);
        tvBranch = view.findViewById(R.id.tv_branch);
        imgProfile = view.findViewById(R.id.img_profile);
        if (tvBranch != null) {
            tvBranch.setVisibility(View.GONE);
        }
        
        // Hide My Addresses and Payments buttons
        View btnAddresses = view.findViewById(R.id.btn_help);
        if (btnAddresses != null) {
            btnAddresses.setVisibility(View.GONE);
        }
        View btnPayments = view.findViewById(R.id.btn_payments);
        if (btnPayments != null) {
            btnPayments.setVisibility(View.GONE);
        }
    }

    private void loadCachedData() {
        // Load and display cached name immediately
        String cachedName = getCachedName();
        if (isValidName(cachedName)) {
            currentName = cachedName;
            if (tvName != null) {
                tvName.setText(cachedName);
            }
        }

        // Load and display cached email/connection status immediately
        String cachedEmail = getCachedEmail();
        String cachedConnectionStatus = getCachedConnectionStatus();

        // Prefer connection status over email if available
        if (cachedConnectionStatus != null && !cachedConnectionStatus.isEmpty()) {
            currentEmail = cachedConnectionStatus;
            if (tvUsername != null) {
                tvUsername.setText(cachedConnectionStatus);
            }
        } else if (isValidEmail(cachedEmail)) {
            currentEmail = cachedEmail;
            if (tvUsername != null) {
                tvUsername.setText(cachedEmail);
            }
        }

        // Branch display hidden for customer profile.
    }

    private void fetchUserData() {
        fallbackToCachedData();
    }

    private void processUserData(UserResponse.Data userData) {
        currentName = buildNameFromApi(userData);
        currentEmail = getEmailToDisplay(userData);
        currentBranch = userData.getBranch();

        connectionStatus = null;

        // Load profile photo from API if available
        if (userData.getProfilePhoto() != null && !userData.getProfilePhoto().isEmpty()) {
            loadProfileImageFromUrl(userData.getProfilePhoto());
        } else {
            // No profile photo - set default avatar
            if (imgProfile != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (imgProfile != null) {
                        imgProfile.setImageResource(R.mipmap.ic_launchericons_round);
                    }
                });
            }
            // Also clear local cache
            try {
                File imageFile = tokenManager.getProfileImageFile(requireContext());
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }

        updateUI();
        updateCache(connectionStatus);
    }

    private String buildNameFromApi(UserResponse.Data userData) {
        String apiName = userData.getName();
        if (isValidName(apiName)) {
            return apiName.trim();
        }

        String firstName = userData.getFirstName();
        String lastName = userData.getLastName();

        if (isValidName(firstName) || isValidName(lastName)) {
            StringBuilder builder = new StringBuilder();
            if (isValidName(firstName)) {
                builder.append(firstName.trim());
            }
            if (isValidName(lastName)) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(lastName.trim());
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }

        return null;
    }

    private String getEmailToDisplay(UserResponse.Data userData) {
        String cachedEmail = getCachedEmail();
        if (isValidEmail(cachedEmail)) {
            return cachedEmail;
        }

        String apiEmail = userData.getEmail();
        String apiUsername = userData.getUsername();

        if (isValidApiEmail(apiEmail, apiUsername)) {
            return apiEmail.trim();
        }

        return cachedEmail;
    }

    private boolean isValidApiEmail(String email, String username) {
        return email != null
                && !email.trim().isEmpty()
                && email.contains("@")
                && !email.equals(username);
    }

    private void updateCache(String connectionStatus) {
        // Save name to cache
        if (isValidName(currentName)) {
            tokenManager.saveName(currentName);
        }

        // Save email to cache if valid
        if (isValidEmail(currentEmail) && !currentEmail.equals(getCachedEmail())) {
            tokenManager.saveEmail(currentEmail);
        }

        // Save connection status to cache
        if (connectionStatus != null && !connectionStatus.isEmpty()) {
            saveConnectionStatus(connectionStatus);
        } else {
            // Clear connection status if user has email now
            clearConnectionStatus();
        }
    }

    private void updateUI() {
        if (getActivity() == null || getView() == null)
            return;

        getActivity().runOnUiThread(() -> {
            updateNameDisplay();
            updateEmailDisplay();
            updateBranchDisplay();
        });
    }

    private void updateNameDisplay() {
        String displayName = isValidName(currentName) ? currentName : getCachedName();
        if (isValidName(displayName) && tvName != null) {
            tvName.setText(displayName.trim());
        }
    }

    private void updateEmailDisplay() {
        String displayEmail = currentEmail;

        // If we have connection status text, use it directly
        if (connectionStatus != null && !connectionStatus.isEmpty()) {
            if (tvUsername != null) {
                tvUsername.setText(connectionStatus);
            }
            return;
        }

        // Otherwise, use email validation logic
        if (!isValidEmail(displayEmail)) {
            displayEmail = getCachedEmail();
        }

        if (isValidEmail(displayEmail) && tvUsername != null) {
            tvUsername.setText(displayEmail);
        }
    }

    private void updateBranchDisplay() {
        if (tvBranch != null) {
            tvBranch.setVisibility(View.GONE);
        }
    }

    private void fallbackToCachedData() {
        if (getActivity() == null)
            return;
        getActivity().runOnUiThread(() -> {
            String cachedName = getCachedName();
            if (isValidName(cachedName) && tvName != null) {
                tvName.setText(cachedName);
                currentName = cachedName;
            }

            String cachedEmail = getCachedEmail();
            if (isValidEmail(cachedEmail) && tvUsername != null) {
                tvUsername.setText(cachedEmail);
                currentEmail = cachedEmail;
            }
        });
    }

    private boolean isValidName(String name) {
        return name != null
                && !name.trim().isEmpty()
                && !name.trim().equals("null")
                && !name.trim().contains("null");
    }

    private boolean isValidEmail(String email) {
        return email != null
                && email.contains("@")
                && email.trim().length() > 3;
    }

    private String getCachedName() {
        try {
            String name = tokenManager.getName();
            return isValidName(name) ? name.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCachedEmail() {
        try {
            String email = tokenManager.getEmail();
            return isValidEmail(email) ? email.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCachedConnectionStatus() {
        try {
            return tokenManager.getConnectionStatus();
        } catch (Exception e) {
            return null;
        }
    }

    private void saveConnectionStatus(String status) {
        try {
            tokenManager.saveConnectionStatus(status);
        } catch (Exception e) {
            // Ignore save errors
        }
    }

    private void clearConnectionStatus() {
        try {
            tokenManager.clearConnectionStatus();
        } catch (Exception e) {
            // Ignore clear errors
        }
    }

    private void setupClickListeners(View view) {
        setClickListener(view, R.id.logoutButton, this::showLogoutConfirmation);
        setClickListener(view, R.id.btn_sign_out, this::showLogoutConfirmation);
        setClickListener(view, R.id.btn_personal_info, this::openPersonalInfo);
        setClickListener(view, R.id.btn_password_privacy, () -> navigateToChangePassword());
        setClickListener(view, R.id.btn_help, this::openAddressBook);
        setClickListener(view, R.id.btn_edit_photo, () -> showImagePickerDialog());
        setClickListener(view, R.id.btn_appearance, () -> showThemeToggler());
        setClickListener(view, R.id.btn_notifications, () -> showNotificationSettings());
        setClickListener(view, R.id.btn_language, () -> showLanguageToggler());
        setClickListener(view, R.id.btn_about_us, () -> navigateToFragment(new ProfileAboutUsFragment()));
    }

    private void setClickListener(View view, int id, Runnable action) {
        View button = view.findViewById(id);
        if (button != null) {
            button.setOnClickListener(v -> action.run());
        }
    }

    private void openPersonalInfo() {
        Intent intent = new Intent(getActivity(), app.hub.common.PersonalInfoActivity.class);
        startActivity(intent);
    }

    private void navigateToFragment(Fragment fragment) {
        // For About Us, launch as activity
        if (fragment instanceof ProfileAboutUsFragment) {
            Intent intent = new Intent(getActivity(), app.hub.common.AboutUsActivity.class);
            startActivity(intent);
            return;
        }
        
        // Fallback for other fragments (if any)
        if (!isAdded() || getActivity() == null) {
            return;
        }

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
    }

    private void showNotificationSettings() {
        if (getContext() == null)
            return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.user_notificationstoggler, null);

        SwitchMaterial switchPush = view.findViewById(R.id.switch_push);
        SwitchMaterial switchEmail = view.findViewById(R.id.switch_email);
        SwitchMaterial switchSms = view.findViewById(R.id.switch_sms);

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

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void openAddressBook() {
        if (getActivity() == null)
            return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, UserAddressFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    private void openPayments() {
        if (getActivity() == null)
            return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, new UserNotificationFragment())
                .addToBackStack(null)
                .commit();

        com.google.android.material.bottomnavigation.BottomNavigationView navView =
                getActivity().findViewById(R.id.bottomNavigationView);
        if (navView != null) {
            navView.setSelectedItemId(R.id.activitybtn);
        }
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
            Log.e(TAG, "Error signing out of Firebase: " + e.getMessage());
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
                Log.e("UserProfileFragment", "Error during logout cleanup: " + e.getMessage(), e);
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
                                        Log.d("UserProfileFragment", "Google sign out successful");
                                    } else {
                                        Log.w("UserProfileFragment", "Google sign out failed: " + task.getException());
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.w("UserProfileFragment", "Google sign out error: " + e.getMessage());
                                });
                            } catch (Exception e) {
                                Log.w("UserProfileFragment", "Google sign out exception: " + e.getMessage());
                            }
                        });

                // Wait for sign out with timeout (don't block forever)
                try {
                    signOutFuture.get(3, java.util.concurrent.TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    Log.w("UserProfileFragment", "Google sign out timed out");
                } catch (Exception e) {
                    Log.w("UserProfileFragment", "Google sign out interrupted: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("UserProfileFragment", "Error during Google sign out: " + e.getMessage(), e);
        }
    }

    private void clearUserData() {
        try {
            File imageFile = tokenManager != null
                    ? tokenManager.getProfileImageFile(requireContext())
                    : new File(requireContext().getFilesDir(), "profile_image.jpg");

            // Clear token manager data
            if (tokenManager != null) {
                tokenManager.clear();
            }

            // Delete locally stored profile photo
            if (imageFile.exists()) {
                imageFile.delete();
            }
        } catch (Exception e) {
            Log.w("UserProfileFragment", "Error clearing user data: " + e.getMessage());
            // Continue with logout even if clearing data fails
        }
    }

    private void navigateToLogin() {
        if (getActivity() == null)
            return;

        try {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        } catch (Exception e) {
            Log.e("UserProfileFragment", "Error navigating to login: " + e.getMessage(), e);
            // If navigation fails, at least clear the activity stack
            getActivity().finish();
        }
    }

    private void showImagePickerDialog() {
        if (getContext() == null)
            return;

        String[] options = { "Camera", "Gallery", "Remove Photo", "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Profile Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else if (which == 1) {
                openGallery();
            } else if (which == 2) {
                showRemovePhotoConfirmation();
            }
        });
        builder.show();
    }

    private void showRemovePhotoConfirmation() {
        if (getContext() == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Remove Photo");
        builder.setMessage("Are you sure you want to remove your profile photo?");
        builder.setPositiveButton("Remove", (dialog, which) -> {
            deleteProfilePhoto();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            if (photoFile != null) {
                String authority = requireContext().getPackageName() + ".fileprovider";
                cameraImageUri = FileProvider.getUriForFile(requireContext(), authority, photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraLauncher.launch(cameraIntent);
            }
        } catch (Exception e) {
            showToast("Error opening camera: " + e.getMessage());
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        galleryLauncher.launch(galleryIntent);
    }

    private File createImageFile() throws IOException {
        String imageFileName = "profile_" + System.currentTimeMillis();
        File storageDir = requireContext().getFilesDir();
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        return imageFile;
    }

    private void setProfileImage(Uri imageUri) {
        try {
            Bitmap bitmap = getBitmapFromUri(imageUri);
            if (bitmap != null && imgProfile != null) {
                imgProfile.setImageBitmap(bitmap);
                saveProfileImage(bitmap);
                uploadProfilePhotoToServer(imageUri);
            }
        } catch (Exception e) {
            showToast("Error loading image: " + e.getMessage());
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            return resizeBitmap(bitmap, 500, 500);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null)
            return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void saveProfileImage(Bitmap bitmap) {
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            // Handle error silently or log it
        }
    }

    private void loadProfileImage() {
        // First try to load from API (will be set in processUserData)
        // If not available, fallback to local cache
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            if (imageFile.exists() && imgProfile != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }

    private void uploadProfilePhotoToServer(Uri imageUri) {
        String email = tokenManager.getEmail();
        if (email == null) {
            showToast("Not authenticated. Please login again.");
            return;
        }

        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            showToast("User not found.");
            return;
        }

        try {
            com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();
            com.google.firebase.storage.StorageReference ref = storage.getReference()
                    .child("profile_photos/" + user.getUid() + "/photo.jpg");

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            String photoUrl = uri.toString();
                            
                            // Update Firestore
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.getUid())
                                    .update("profilePhoto", photoUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        showToast("Profile photo saved");
                                    })
                                    .addOnFailureListener(e -> {
                                        showToast("Failed to update profile: " + e.getMessage());
                                    });
                        }).addOnFailureListener(e -> {
                            showToast("Failed to get image URL: " + e.getMessage());
                        });
                    })
                    .addOnFailureListener(e -> {
                        showToast("Failed to upload photo: " + e.getMessage());
                    });
        } catch (Exception e) {
            showToast("Error uploading photo: " + e.getMessage());
        }
    }

    private File createFileFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null)
                return null;

            File tempFile = new File(requireContext().getFilesDir(),
                    "temp_upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void loadProfileImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || imgProfile == null) {
            return;
        }

        // Load image from URL in background thread
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
                            // Also save locally for offline access
                            saveProfileImage(bitmap);
                        }
                    });
                }
            } catch (Exception e) {
                // Fallback to local image if URL loading fails
                loadProfileImage();
            }
        }).start();
    }

    private void deleteProfilePhoto() {
        String email = tokenManager.getEmail();
        if (email == null) {
            showToast("Not authenticated. Please login again.");
            return;
        }

        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            showToast("User not found.");
            return;
        }

        // Update Firestore to remove profilePhoto
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("profilePhoto", null)
                .addOnSuccessListener(aVoid -> {
                    showToast("Profile photo removed");

                    // Clear local cache
                    try {
                        File imageFile = tokenManager.getProfileImageFile(requireContext());
                        if (imageFile.exists()) {
                            imageFile.delete();
                        }
                    } catch (Exception e) {
                        // Ignore errors
                    }

                    // Set default avatar
                    if (imgProfile != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (imgProfile != null) {
                                imgProfile.setImageResource(R.mipmap.ic_launchericons_round);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to remove photo: " + e.getMessage());
                });
    }

    private void navigateToChangePassword() {
        if (getActivity() != null) {
            ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, changePasswordFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showThemeToggler() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
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
                showToast("Theme updated to " + selectedTheme);
                bottomSheetDialog.dismiss();
            });
        }
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showLanguageToggler() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
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
                showToast("Language updated");
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
        TextView btnConfirmLogout = overlayView.findViewById(R.id.btnConfirmLogout);
        TextView btnCancelLogout = overlayView.findViewById(R.id.btnCancelLogout);
        
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
