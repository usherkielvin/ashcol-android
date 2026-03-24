package app.hub;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import app.hub.api.ResetPasswordRequest;
import app.hub.api.ResetPasswordResponse;
import app.hub.api.VerificationRequest;
import app.hub.api.VerificationResponse;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    // Views
    private TextInputEditText etEmail;
    private TextInputEditText etOtp;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputLayout emailInputLayout;
    private TextInputLayout otpInputLayout;
    private TextInputLayout newPasswordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private MaterialButton btnSendCode;
    private MaterialButton btnResetPassword;
    private MaterialButton btnResendCode;
    private View otpSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        emailInputLayout = findViewById(R.id.emailInputLayout);
        otpInputLayout = findViewById(R.id.otpInputLayout);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        
        btnSendCode = findViewById(R.id.btnSendCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnResendCode = findViewById(R.id.btnResendCode);
        otpSection = findViewById(R.id.otpSection);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupClickListeners() {
        btnSendCode.setOnClickListener(v -> validateAndSendCode());
        btnResetPassword.setOnClickListener(v -> validateAndResetPassword());
        btnResendCode.setOnClickListener(v -> resendVerificationCode());
    }

    private void validateAndSendCode() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            return;
        }

        emailInputLayout.setError(null);
        sendVerificationCode(email);
    }

    private void sendVerificationCode(String email) {
        setLoadingState(true, true); // Enable loading for send code button

        ApiService apiService = ApiClient.getApiService();
        VerificationRequest request = new VerificationRequest(email);
        Call<VerificationResponse> call = apiService.forgotPassword(request);

        call.enqueue(new Callback<VerificationResponse>() {
            @Override
            public void onResponse(Call<VerificationResponse> call, Response<VerificationResponse> response) {
                setLoadingState(false, true);
                
                if (response.isSuccessful() && response.body() != null) {
                    VerificationResponse verificationResponse = response.body();
                    if (verificationResponse.isSuccess()) {
                        showOtpSection();
                        Toast.makeText(ForgotPasswordActivity.this, 
                            verificationResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        String message = verificationResponse.getMessage() != null ? 
                            verificationResponse.getMessage() : "Failed to send verification code";
                        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<VerificationResponse> call, Throwable t) {
                setLoadingState(false, true);
                Log.e(TAG, "Send verification code failed: " + t.getMessage());
                Toast.makeText(ForgotPasswordActivity.this, 
                    "Failed to send verification code. Please check your connection.", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOtpSection() {
        otpSection.setVisibility(View.VISIBLE);
        btnSendCode.setVisibility(View.GONE);
    }

    private void validateAndResetPassword() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        clearErrors();

        boolean isValid = true;

        if (TextUtils.isEmpty(otp)) {
            otpInputLayout.setError("Verification code is required");
            isValid = false;
        } else if (otp.length() != 6) {
            otpInputLayout.setError("Code must be 6 digits");
            isValid = false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            newPasswordInputLayout.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 8) {
            newPasswordInputLayout.setError("Password must be at least 8 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        resetPassword(email, otp, newPassword);
    }

    private void resetPassword(String email, String code, String newPassword) {
        setLoadingState(true, false); // Enable loading for reset password button

        ApiService apiService = ApiClient.getApiService();
        ResetPasswordRequest request = new ResetPasswordRequest(
            email, 
            code, 
            newPassword, 
            newPassword // Confirmation is same as password
        );
        
        Call<ResetPasswordResponse> call = apiService.resetPassword(request);

        call.enqueue(new Callback<ResetPasswordResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {
                setLoadingState(false, false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ResetPasswordResponse resetResponse = response.body();
                    if (resetResponse.isSuccess()) {
                        Toast.makeText(ForgotPasswordActivity.this, 
                            "Password reset successfully! You can now login with your new password.", 
                            Toast.LENGTH_LONG).show();
                        
                        // Finish the activity and return to login
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        String message = resetResponse.getMessage() != null ? 
                            resetResponse.getMessage() : "Failed to reset password";
                        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
                setLoadingState(false, false);
                Log.e(TAG, "Reset password failed: " + t.getMessage());
                Toast.makeText(ForgotPasswordActivity.this, 
                    "Failed to reset password. Please check your connection.", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resendVerificationCode() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            return;
        }

        sendVerificationCode(email);
    }

    private void setLoadingState(boolean isLoading, boolean isSendCodeButton) {
        if (isSendCodeButton) {
            btnSendCode.setEnabled(!isLoading);
            btnSendCode.setText(isLoading ? "Sending..." : "Send Code");
        } else {
            btnResetPassword.setEnabled(!isLoading);
            btnResetPassword.setText(isLoading ? "Resetting..." : "Reset Password");
        }
    }

    private void clearErrors() {
        emailInputLayout.setError(null);
        otpInputLayout.setError(null);
        newPasswordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
    }

    private void handleErrorResponse(Response<?> response) {
        if (response.code() == 400 || response.code() == 422) {
            Toast.makeText(ForgotPasswordActivity.this, 
                "Invalid input. Please check your information.", 
                Toast.LENGTH_LONG).show();
        } else if (response.code() == 404) {
            Toast.makeText(ForgotPasswordActivity.this, 
                "User not found. Please check your email address.", 
                Toast.LENGTH_LONG).show();
        } else if (response.code() == 500) {
            Toast.makeText(ForgotPasswordActivity.this, 
                "Server error. Please try again later.", 
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(ForgotPasswordActivity.this, 
                "Request failed. Please try again.", 
                Toast.LENGTH_LONG).show();
        }
    }
}
