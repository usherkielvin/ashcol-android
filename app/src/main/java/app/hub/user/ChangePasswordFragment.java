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
import app.hub.api.ChangePasswordRequest;
import app.hub.api.ChangePasswordResponse;
import app.hub.util.TokenManager;


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
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword, newPassword);
        ApiService apiService = ApiClient.getApiService();
        Call<ChangePasswordResponse> call = apiService.changePassword(token, request);

        call.enqueue(new Callback<ChangePasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChangePasswordResponse> call, @NonNull Response<ChangePasswordResponse> response) {
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    ChangePasswordResponse changePasswordResponse = response.body();
                    if (changePasswordResponse.isSuccess()) {
                        handlePasswordChangeSuccess(changePasswordResponse.getMessage());
                    } else {
                        handlePasswordChangeFailure(changePasswordResponse.getMessage());
                    }
                } else {
                    handlePasswordChangeError(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChangePasswordResponse> call, @NonNull Throwable t) {
                setLoadingState(false);
                Log.e("ChangePasswordFragment", "Change password failed: " + t.getMessage());
                Toast.makeText(requireContext(), "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
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

    private void handlePasswordChangeError(Response<ChangePasswordResponse> response) {
        if (response.code() == 400 || response.code() == 422) {
            ChangePasswordResponse errorResponse = response.body();
            if (errorResponse != null && errorResponse.getErrors() != null) {
                displayValidationErrors(errorResponse.getErrors(), errorResponse.getMessage());
            } else {
                Toast.makeText(requireContext(), "Invalid input. Please check your passwords.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(requireContext(), "Failed to change password. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayValidationErrors(ChangePasswordResponse.Errors errors, String message) {
        StringBuilder errorMsg = new StringBuilder();

        if (errors.getCurrent_password() != null && errors.getCurrent_password().length > 0) {
            currentPasswordLayout.setError(errors.getCurrent_password()[0]);
            errorMsg.append(errors.getCurrent_password()[0]).append("\n");
        }

        if (errors.getNew_password() != null && errors.getNew_password().length > 0) {
            newPasswordLayout.setError(errors.getNew_password()[0]);
            errorMsg.append(errors.getNew_password()[0]).append("\n");
        }

        if (errors.getNew_password_confirmation() != null && errors.getNew_password_confirmation().length > 0) {
            confirmPasswordLayout.setError(errors.getNew_password_confirmation()[0]);
            errorMsg.append(errors.getNew_password_confirmation()[0]);
        }

        if (errorMsg.length() > 0) {
            Toast.makeText(requireContext(), errorMsg.toString().trim(), Toast.LENGTH_LONG).show();
        } else {
            String displayMessage = message != null && !message.isEmpty() ? message : "Invalid input";
            Toast.makeText(requireContext(), displayMessage, Toast.LENGTH_LONG).show();
        }
    }
}
