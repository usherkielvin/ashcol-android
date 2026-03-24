package app.hub.employee;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import app.hub.R;
import app.hub.BuildConfig;
import com.google.firebase.Timestamp;
import app.hub.util.TokenManager;

public class EmployeePersonalInfoFragment extends Fragment {

    private TokenManager tokenManager;
    private TextView tvEmail;
    private TextView tvRole;
    private TextView tvBranch;
    private MaterialAutoCompleteTextView tvGender;
    private TextView tvBirthdate;
    private TextView etUsername;
    private TextInputEditText inputFirstName;
    private TextInputEditText inputLastName;
    private TextInputEditText inputPhone;
    private TextInputEditText inputLocation;
    private TextInputLayout layoutFirstName;
    private TextInputLayout layoutLastName;
    private MaterialButton btnSave;
    private ShapeableImageView imgProfile;
    private MaterialButton btnEditPhoto;
    private View personalInfoContent;
    private View profileLoading;
    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private String selectedGender;
    private String selectedBirthdate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_employee_personal_info, container, false);
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

        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvRole = view.findViewById(R.id.tvProfileRole);
        tvBranch = view.findViewById(R.id.tvProfileBranch);
        tvGender = view.findViewById(R.id.tvGender);
        tvBirthdate = view.findViewById(R.id.etDob);
        etUsername = view.findViewById(R.id.etUsername);
        inputFirstName = view.findViewById(R.id.inputFirstName);
        inputLastName = view.findViewById(R.id.inputLastName);
        inputPhone = view.findViewById(R.id.inputPhone);
        inputLocation = view.findViewById(R.id.inputLocation);
        layoutFirstName = view.findViewById(R.id.layoutFirstName);
        layoutLastName = view.findViewById(R.id.layoutLastName);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        imgProfile = view.findViewById(R.id.imgProfile);
        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);
        personalInfoContent = view.findViewById(R.id.personalInfoContent);
        profileLoading = view.findViewById(R.id.profileLoading);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }

        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> showPhotoOptions());
        }

        View btnDelete = view.findViewById(R.id.btnDeleteAccount);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteAccountDialog());
        }

        if (tvBirthdate != null) {
            tvBirthdate.setOnClickListener(v -> showBirthdatePicker());
        }

        setupGenderDropdown();

        loadCachedProfileImage();
        loadProfile();
    }

    private void initializeLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            uploadProfilePhoto(selectedImage);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && cameraImageUri != null) {
                        uploadProfilePhoto(cameraImageUri);
                    }
                });
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }

        setLoading(true);

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                setLoading(false);
                if (documentSnapshot.exists()) {
                    bindProfile(documentSnapshot);
                }
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setLoading(boolean isLoading) {
        if (profileLoading != null) {
            profileLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (personalInfoContent != null) {
            personalInfoContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void bindProfile(DocumentSnapshot doc) {
        if (!isAdded() || doc == null) {
            return;
        }

        String firstName = doc.getString("first_name");
        String lastName = doc.getString("last_name");
        String email = doc.getString("email");
        String role = doc.getString("role");
        String branch = doc.getString("branch");
        String gender = doc.getString("gender");
        String birthdate = doc.getString("birthdate");
        String username = doc.getString("username");
        String phone = doc.getString("phone");
        String location = doc.getString("location");
        String profilePhoto = doc.getString("profile_photo");

        if (tvEmail != null) tvEmail.setText(email != null ? email : "--");
        if (tvRole != null) tvRole.setText(role != null ? capitalizeFirst(role) : "Employee");
        if (tvBranch != null) tvBranch.setText(branch != null ? branch : "No branch assigned");
        
        if (inputFirstName != null) inputFirstName.setText(firstName != null ? firstName : "");
        if (inputLastName != null) inputLastName.setText(lastName != null ? lastName : "");
        if (etUsername != null) etUsername.setText(username != null ? username : "");
        if (inputPhone != null) inputPhone.setText(phone != null ? phone : "");
        if (inputLocation != null) inputLocation.setText(location != null ? location : "");
        
        if (tvGender != null && gender != null) {
            tvGender.setText(gender, false);
            selectedGender = gender;
        }
        
        if (tvBirthdate != null && birthdate != null) {
            selectedBirthdate = birthdate;
            tvBirthdate.setText(formatBirthdateForDisplay(birthdate));
        }

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            loadProfileImageFromUrl(profilePhoto);
        } else {
            if (imgProfile != null) {
                imgProfile.setImageResource(R.mipmap.ic_launchericons_round);
            }
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void setupGenderDropdown() {
        if (getContext() == null || tvGender == null) {
            return;
        }

        String[] options = new String[] { "Male", "Female", "Other", "Prefer not to say" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options);
        tvGender.setAdapter(adapter);
        tvGender.setOnItemClickListener((parent, view, position, id) -> selectedGender = options[position]);
    }

    private void showBirthdatePicker() {
        if (getContext() == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        if (selectedBirthdate != null && !selectedBirthdate.trim().isEmpty()) {
            String[] parts = selectedBirthdate.split("-");
            if (parts.length == 3) {
                try {
                    calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedBirthdate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    if (tvBirthdate != null) {
                        tvBirthdate.setText(formatBirthdateForDisplay(selectedBirthdate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String formatBirthdateForDisplay(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return output.format(input.parse(value));
        } catch (Exception e) {
            return value;
        }
    }

    private void showPhotoOptions() {
        if (getContext() == null) return;

        String[] options = new String[] {"Take photo", "Choose from gallery", "Remove photo"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Profile photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        openGallery();
                    } else {
                        showRemovePhotoConfirmation();
                    }
                })
                .show();
    }

    private void showRemovePhotoConfirmation() {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Photo")
                .setMessage("Are you sure you want to remove your profile photo?")
                .setPositiveButton("Remove", (dialog, which) -> deleteProfilePhoto())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        if (getContext() == null) return;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireContext().getPackageManager()) == null) {
            Toast.makeText(requireContext(), "No camera app available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File imageFile = File.createTempFile("profile_", ".jpg", requireContext().getCacheDir());
            cameraImageUri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider", imageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Unable to open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfilePhoto(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setLoading(true);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
            .child("profile_photos")
            .child(user.getUid() + ".jpg");

        storageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                        .update("profile_photo", downloadUri.toString())
                        .addOnSuccessListener(aVoid -> {
                            setLoading(false);
                            if (isAdded()) {
                                if (imgProfile != null) {
                                    imgProfile.setImageURI(imageUri);
                                }
                                saveProfileImage(imageUri);
                                
                                // Clear manager cache so updated photo shows immediately
                                app.hub.manager.ManagerDataManager.clearEmployeeCache();
                                
                                Toast.makeText(getContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                                loadProfile();
                            }
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to update profile info", Toast.LENGTH_SHORT).show();
                            }
                        });
                });
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void deleteProfilePhoto() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setLoading(true);

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .update("profile_photo", null)
            .addOnSuccessListener(aVoid -> {
                setLoading(false);
                if (isAdded()) {
                    if (imgProfile != null) {
                        imgProfile.setImageResource(R.mipmap.ic_launchericons_round);
                    }
                    clearCachedProfileImage();
                    
                    // Clear manager cache so removed photo reflects immediately
                    app.hub.manager.ManagerDataManager.clearEmployeeCache();
                    
                    Toast.makeText(getContext(), "Profile photo removed", Toast.LENGTH_SHORT).show();
                    loadProfile();
                }
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to remove photo", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadProfileImageFromUrl(String url) {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (imgProfile != null) {
                                imgProfile.setImageBitmap(bitmap);
                            }
                        });
                        saveProfileBitmap(bitmap);
                    }
                    input.close();
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void saveProfileBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ignored) {
        }
    }

    private void saveProfileImage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return;

            File imageFile = tokenManager.getProfileImageFile(requireContext());
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException ignored) {
        }
    }

    private void loadCachedProfileImage() {
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

    private void clearCachedProfileImage() {
        try {
            File imageFile = tokenManager.getProfileImageFile(requireContext());
            if (imageFile.exists()) {
                imageFile.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (layoutFirstName != null) layoutFirstName.setError(null);
        if (layoutLastName != null) layoutLastName.setError(null);

        String firstName = inputFirstName != null ? inputFirstName.getText().toString().trim() : "";
        String lastName = inputLastName != null ? inputLastName.getText().toString().trim() : "";
        String phone = inputPhone != null ? inputPhone.getText().toString().trim() : "";
        String location = inputLocation != null ? inputLocation.getText().toString().trim() : "";
        String username = etUsername != null ? etUsername.getText().toString().trim() : "";
        String gender = selectedGender != null ? selectedGender : "";
        String birthdate = selectedBirthdate != null ? selectedBirthdate : "";

        if (!validateRequiredFields(firstName, lastName, username, phone, gender, birthdate)) {
            return;
        }

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("first_name", firstName);
        updates.put("last_name", lastName);
        updates.put("phone", phone);
        updates.put("location", location);
        updates.put("gender", gender);
        updates.put("birthdate", birthdate);
        updates.put("updated_at", Timestamp.now());

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                if (!isAdded()) return;
                restoreSaveButton();
                String fullName = firstName + " " + lastName;
                tokenManager.saveName(fullName.trim());
                Toast.makeText(requireContext(), "Profile updated.", Toast.LENGTH_SHORT).show();
                loadProfile();
                navigateBack();
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                restoreSaveButton();
                Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void restoreSaveButton() {
        if (btnSave != null) {
            btnSave.setEnabled(true);
            btnSave.setText("Save changes");
        }
    }

    private boolean validateRequiredFields(String firstName, String lastName, String username,
            String phone, String gender, String birthdate) {
        boolean valid = true;

        if (layoutFirstName != null) layoutFirstName.setError(null);
        if (layoutLastName != null) layoutLastName.setError(null);

        if (firstName.isEmpty()) {
            if (layoutFirstName != null) layoutFirstName.setError("First name is required");
            valid = false;
        }

        if (lastName.isEmpty()) {
            if (layoutLastName != null) layoutLastName.setError("Last name is required");
            valid = false;
        }

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Username is required", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (gender.isEmpty()) {
            Toast.makeText(requireContext(), "Gender is required", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (birthdate.isEmpty()) {
            Toast.makeText(requireContext(), "Birthdate is required", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    private void showDeleteAccountDialog() {
        if (getContext() == null) {
            return;
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextInputLayout passwordLayout = new TextInputLayout(requireContext());
        passwordLayout.setHint("Password");
        TextInputEditText passwordInput = new TextInputEditText(requireContext());
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordLayout.addView(passwordInput);

        TextInputLayout confirmLayout = new TextInputLayout(requireContext());
        confirmLayout.setHint("Type DELETE to confirm");
        TextInputEditText confirmInput = new TextInputEditText(requireContext());
        confirmLayout.addView(confirmInput);

        layout.addView(passwordLayout);
        layout.addView(confirmLayout);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete account")
                .setMessage("This will permanently delete your account and data.")
                .setView(layout)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String password = passwordInput.getText() != null
                            ? passwordInput.getText().toString().trim()
                            : "";
                    String confirm = confirmInput.getText() != null
                            ? confirmInput.getText().toString().trim()
                            : "";
                    if (password.isEmpty()) {
                        Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!"DELETE".equals(confirm)) {
                        Toast.makeText(requireContext(), "Please type DELETE to confirm", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    deleteAccount(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate user before deletion
        com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                // Delete Firestore data first
                FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid1 -> {
                        // Then delete Auth account
                        user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                if (!isAdded()) return;
                                tokenManager.clear();
                                Toast.makeText(requireContext(), "Account deleted.", Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) {
                                    Intent intent = new Intent(getActivity(), app.hub.common.MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    getActivity().finish();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Failed to delete auth account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Re-authentication failed. Please check your password.", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}
