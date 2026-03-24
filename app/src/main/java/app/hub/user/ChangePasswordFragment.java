package app.hub.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import app.hub.R;
import app.hub.util.TokenManager;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ChangePasswordFragment extends Fragment {

    private TokenManager tokenManager;
    private TextInputEditText currentPasswordInput;
    private TextInputEditText newPasswordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputLayout currentPasswordLayout;
    private TextInputLayout newPasswordLayout;
    private TextInputLayout confirmPasswordLayout;
    private MaterialButton btnContinue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile__changepass, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tokenManager = new TokenManager(requireContext());
        initializeViews(view);
        setupClickListeners();
    }

    private void initializeViews(View view) {
        currentPasswordInput = view.findViewById(R.id.currentPasswordInput);
        newPasswordInput = view.findViewById(R.id.newPasswordInput);
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        currentPasswordLayout = view.findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = view.findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout);
        btnContinue = view.findViewById(R.id.verifyButton);

        View btnBack = view.findViewById(R.id.closeButton);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }
    }

    private void setupClickListeners() {
        btnContinue.setOnClickListener(v -> handleChangePassword());
    }

    private void navigateBack() {
        if (getActivity() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void handleChangePassword() {
        String currentPassword = currentPasswordInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        clearErrors();

        if (validateInputs(currentPassword, newPassword, confirmPassword)) {
            changePassword(currentPassword, newPassword);
        }
    }

    private void clearErrors() {
        currentPasswordLayout.setError(null);
        newPasswordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private boolean validateInputs(String currentPassword, String newPassword, String confirmPassword) {
        boolean isValid = true;

        if (currentPassword.isEmpty()) {
            currentPasswordLayout.setError("Current password is required");
            isValid = false;
        }

        if (newPassword.isEmpty()) {
            newPasswordLayout.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 8) {
            newPasswordLayout.setError("Password must be at least 8 characters");
            isValid = false;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        // Re-authenticate user before changing password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                // Change password
                user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid1 -> {
                        setLoadingState(false);
                        handlePasswordChangeSuccess("Password changed successfully");
                    })
                    .addOnFailureListener(e -> {
                        setLoadingState(false);
                        handlePasswordChangeFailure(e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                setLoadingState(false);
                Toast.makeText(requireContext(), "Re-authentication failed. Please check your current password.", Toast.LENGTH_LONG).show();
            });
    }

    private void setLoadingState(boolean isLoading) {
        btnContinue.setEnabled(!isLoading);
        btnContinue.setText(isLoading ? "Changing..." : "Continue");
    }

    private void handlePasswordChangeSuccess(String message) {
        String successMessage = message != null && !message.isEmpty() ? message : "Password changed successfully";
        Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
        clearInputFields();
        navigateBack();
    }

    private void handlePasswordChangeFailure(String message) {
        String errorMessage = message != null && !message.isEmpty() ? message : "Failed to change password";
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private void clearInputFields() {
        currentPasswordInput.setText("");
        newPasswordInput.setText("");
        confirmPasswordInput.setText("");
    }

    private void handlePasswordChangeError(Object response) {
        // Not used anymore in Firebase flow
    }
}
