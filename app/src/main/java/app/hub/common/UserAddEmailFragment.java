package app.hub.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import app.hub.R;
import app.hub.api.LoginRequest;
import app.hub.api.LoginResponse;


public class UserAddEmailFragment extends Fragment {

    private static final String TAG = "UserAddEmailFragment";
    private EditText emailInput;
    private Button continueButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_add_email, container, false);
        
        initializeViews(view);
        setupValidation();
        setupButtons(view);
        
        // Pre-fill email if available (for Google users)
        prefillEmailIfAvailable();
        
        return view;
    }
    
    private void prefillEmailIfAvailable() {
        RegisterActivity activity = (RegisterActivity) getActivity();
        if (activity != null && emailInput != null) {
            String prefillEmail = activity.getUserEmail();
            if (prefillEmail != null && !prefillEmail.isEmpty()) {
                emailInput.setText(prefillEmail);
                // Move cursor to end
                if (emailInput.getText() != null) {
                    emailInput.setSelection(emailInput.getText().length());
                }
            }
        }
    }

    private void initializeViews(View view) {
        emailInput = view.findViewById(R.id.Email_val);
        continueButton = view.findViewById(R.id.OpenOTP);
    }

    private void setupValidation() {
        if (emailInput != null) {
            emailInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateEmail(s.toString());
                }
            });
        }
    }

    private void setupButtons(View view) {
        ImageButton backButton = view.findViewById(R.id.closeButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (continueButton != null) {
            continueButton.setOnClickListener(v -> {
                if (validateEmailField()) {
                    saveEmailAndContinue();
                }
            });
        }
    }

    private void validateEmail(String email) {
        // Real-time validation as user types
        if (email.isEmpty()) {
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Invalid email format
        }
    }

    private boolean validateEmailField() {
        String email = getText(emailInput);

        if (email.isEmpty()) {
            showError("Email is required");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    private void saveEmailAndContinue() {
        String email = getText(emailInput);
        
        // Show loading state
        if (continueButton != null) {
            continueButton.setEnabled(false);
            continueButton.setText("Please wait...");
        }

        // Show loading state
        if (continueButton != null) {
            continueButton.setEnabled(false);
            continueButton.setText("Checking...");
        }

        // Check if email already exists in database
        checkEmailExists(email);
    }
    
    private void showEmailExistsError(String message) {
        Log.d(TAG, "Email already exists: " + message);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        
        // Don't navigate away automatically, let user choose what to do
        // The user can either type a different email or close the registration and go to login
    }
    
    private void checkEmailExists(String email) {
        ApiService apiService = ApiClient.getApiService();
        
        // Use a dummy login request to check if email exists
        // We'll use an invalid password to trigger the check
        app.hub.api.LoginRequest request = new app.hub.api.LoginRequest(email, "dummy_check_password_" + System.currentTimeMillis());
        
        Call<app.hub.api.LoginResponse> call = apiService.login(request);
        call.enqueue(new Callback<app.hub.api.LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<app.hub.api.LoginResponse> call, @NonNull Response<app.hub.api.LoginResponse> response) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    // If we get 401 (invalid credentials), it means the email exists
                    if (response.code() == 401) {
                        showEmailExistsError("Email already registered. Please use a different email or sign in instead.");
                        resetContinueButton();
                    } else if (response.code() == 404) {
                        // Email doesn't exist - proceed with registration
                        proceedWithEmailRegistration(email);
                    } else {
                        // Other error codes - proceed with registration (backend will catch duplicates)
                        proceedWithEmailRegistration(email);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<app.hub.api.LoginResponse> call, @NonNull Throwable t) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error checking email: " + t.getMessage());
                    // On network error, proceed anyway (backend will catch duplicates)
                    proceedWithEmailRegistration(email);
                });
            }
        });
    }
    
    private void proceedWithEmailRegistration(String email) {
        Log.d(TAG, "Email doesn't exist, proceeding with registration");
        
        RegisterActivity activity = (RegisterActivity) getActivity();
        if (activity != null) {
            activity.setUserEmail(email);
            activity.showTellUsFragment();
        }
    }
    
    private void resetContinueButton() {
        if (continueButton != null) {
            continueButton.setEnabled(true);
            continueButton.setText("Continue");
        }
    }

    private String getText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
