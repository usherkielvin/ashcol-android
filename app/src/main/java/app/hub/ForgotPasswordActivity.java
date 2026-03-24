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

import com.google.firebase.auth.FirebaseAuth;

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

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                setLoadingState(false, true);
                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, 
                        "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                    // In Firebase, we don't need the OTP section for link-based reset
                    // We can just finish the activity or show a message
                    finish();
                } else {
                    String errorMessage = task.getException() != null ? 
                        task.getException().getMessage() : "Failed to send reset email";
                    Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void showOtpSection() {
        // No longer needed for standard Firebase link-based reset
        // but keeping it to avoid compilation errors if called elsewhere
        otpSection.setVisibility(View.VISIBLE);
        btnSendCode.setVisibility(View.GONE);
    }

    private void validateAndResetPassword() {
        // No longer needed for standard Firebase link-based reset
        Toast.makeText(this, "Please check your email for the reset link.", Toast.LENGTH_SHORT).show();
    }

    private void resetPassword(String email, String code, String newPassword) {
        // No longer needed for standard Firebase link-based reset
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
            btnSendCode.setText(isLoading ? "Sending..." : "Send Reset Link");
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

    private void handleErrorResponse(Object response) {
        // Not used anymore in Firebase flow
    }
}
