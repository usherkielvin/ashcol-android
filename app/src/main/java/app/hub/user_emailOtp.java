package app.hub;

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import app.hub.api.VerificationRequest;
import app.hub.api.VerificationResponse;
import app.hub.api.VerifyEmailRequest;
import app.hub.api.VerifyEmailResponse;
import app.hub.common.RegisterActivity;

public class user_emailOtp extends Fragment {
    private static final String TAG = "user_emailOtp";
    
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button verifyButton;
    private MaterialButton resendCodeButton;
    private TextView messageTextView;
    private ImageButton closeButton;
    private String email;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_email_otp, container, false);
        
        // Get email from RegisterActivity
        RegisterActivity activity = (RegisterActivity) getActivity();
        if (activity != null) {
            email = activity.getUserEmail();
        }
        
        initializeViews(view);
        setupEmailMessage();
        setupCloseButton();
        setupOtpCodeInput();
        setupOtpVerifyButton();
        setupOtpResendButton();
        
        // Send OTP when fragment is created
        if (email != null && !email.isEmpty()) {
            sendOtpToEmail(email);
        }
        
        return view;
    }
    
    private void initializeViews(View view) {
        otp1 = view.findViewById(R.id.otp1);
        otp2 = view.findViewById(R.id.otp2);
        otp3 = view.findViewById(R.id.otp3);
        otp4 = view.findViewById(R.id.otp4);
        otp5 = view.findViewById(R.id.otp5);
        otp6 = view.findViewById(R.id.otp6);
        verifyButton = view.findViewById(R.id.verifyButton);
        resendCodeButton = view.findViewById(R.id.resendCodeButton);
        messageTextView = view.findViewById(R.id.verificationMessage);
        closeButton = view.findViewById(R.id.closeButton);
    }
    
    private void setupEmailMessage() {
        if (messageTextView != null && email != null) {
            String maskedEmail = maskEmail(email);
            String message = String.format("We've sent a 6-digit code to your\nemail %s.", maskedEmail);
            messageTextView.setText(message);
        }
    }
    
    private void setupCloseButton() {
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }
    
    // Mask email for display (e.g., user@example.com -> use***@example.com)
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "*******@example.com";
        }
        int atIndex = email.indexOf("@");
        String localPart = email.substring(0, Math.min(3, atIndex));
        String domain = email.substring(atIndex);
        return localPart + "***" + domain;
    }
    
    private void setupOtpCodeInput() {
        if (otp1 == null) return;

        // Auto-focus first OTP field and show keyboard
        otp1.post(() -> {
            otp1.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(otp1, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Setup text watchers for auto-advancing between OTP fields
        EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < otpFields.length; i++) {
            final int currentIndex = i;
            final EditText currentField = otpFields[i];
            
            if (currentField != null) {
                currentField.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Move to next field when a digit is entered
                        if (s.length() == 1 && currentIndex < otpFields.length - 1 && otpFields[currentIndex + 1] != null) {
                            otpFields[currentIndex + 1].requestFocus();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // Enable verify button when all 6 digits are entered
                        if (verifyButton != null) {
                            boolean allFilled = true;
                            for (EditText field : otpFields) {
                                if (field != null && (field.getText() == null || field.getText().length() == 0)) {
                                    allFilled = false;
                                    break;
                                }
                            }
                            verifyButton.setEnabled(allFilled);
                        }
                    }
                });

                // Handle backspace to move to previous field
                currentField.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == android.view.KeyEvent.KEYCODE_DEL && currentIndex > 0 && currentField.getText().length() == 0) {
                        if (otpFields[currentIndex - 1] != null) {
                            otpFields[currentIndex - 1].requestFocus();
                            return true;
                        }
                    }
                    return false;
                });
            }
        }
    }
    
    @SuppressLint("SetTextI18n")
    private void setupOtpVerifyButton() {
        if (verifyButton == null || email == null) return;

        verifyButton.setEnabled(false);
        verifyButton.setOnClickListener(v -> {
            // Combine all OTP fields into a single code string
            StringBuilder codeBuilder = new StringBuilder();
            EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};
            
            for (EditText field : otpFields) {
                if (field != null && field.getText() != null) {
                    String text = field.getText().toString().trim();
                    if (!text.isEmpty()) {
                        codeBuilder.append(text);
                    }
                }
            }
            
            final String code = codeBuilder.toString();

            // Validate code length
            if (code.length() != 6) {
                Toast.makeText(getContext(), "Enter 6-digit code", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyButton.setEnabled(false);
            verifyButton.setText("Verifying...");
            verifyOtpCode(email, code);
        });
    }
    
    @SuppressLint("SetTextI18n")
    private void setupOtpResendButton() {
        if (resendCodeButton == null || email == null) return;

        resendCodeButton.setOnClickListener(v -> {
            resendCodeButton.setEnabled(false);
            resendCodeButton.setText("Sending...");
            resendOtpCode(email);
        });
    }
    
    // Send OTP to email
    private void sendOtpToEmail(String email) {
        Log.d(TAG, "Attempting to send OTP to email: " + email);
        ApiService apiService = ApiClient.getApiService();
        VerificationRequest request = new VerificationRequest(email);

        Call<VerificationResponse> call = apiService.sendVerificationCode(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<VerificationResponse> call, @NonNull Response<VerificationResponse> response) {
                Log.d(TAG, "OTP send response - Status: " + response.code() + ", Successful: " + response.isSuccessful());
                if (response.isSuccessful() && response.body() != null) {
                    VerificationResponse body = response.body();
                    Log.d(TAG, "Response body - Success: " + body.isSuccess() + ", Message: " + body.getMessage());
                    if (!body.isSuccess()) {
                        // Response was successful but success flag is false
                        String errorMsg = body.getMessage() != null && !body.getMessage().isEmpty() ?
                            body.getMessage() : "Failed to send OTP. Please try again.";
                        Log.e(TAG, "OTP send failed: " + errorMsg);
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Response not successful - try to read error body
                    String errorMsg = "Failed to send OTP. Please try again.";
                    try {
                        if (response.errorBody() != null) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(response.errorBody().byteStream()));
                            String errorJson = reader.readLine();
                            if (errorJson != null) {
                                VerificationResponse errorResponse = gson.fromJson(errorJson, VerificationResponse.class);
                                if (errorResponse != null && errorResponse.getMessage() != null) {
                                    errorMsg = errorResponse.getMessage();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
                    }
                    Log.e(TAG, "OTP send failed with status: " + response.code() + ", message: " + errorMsg);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<VerificationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error sending OTP: " + t.getMessage(), t);
                String errorMsg = "Network error. Please check your connection.";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout") || t.getMessage().contains("Timeout")) {
                        errorMsg = "Request timeout. Please check your connection and try again.";
                    } else if (t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Cannot reach server. Please check your internet connection.";
                    }
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    // Verify OTP code
    private void verifyOtpCode(String email, String code) {
        ApiService apiService = ApiClient.getApiService();
        VerifyEmailRequest request = new VerifyEmailRequest(email, code);

        Call<VerifyEmailResponse> call = apiService.verifyEmail(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<VerifyEmailResponse> call, @NonNull Response<VerifyEmailResponse> response) {
                resetVerifyButton();

                if (response.isSuccessful() && response.body() != null) {
                    handleOtpVerificationSuccess(response.body());
                } else {
                    handleOtpVerificationError(response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<VerifyEmailResponse> call, @NonNull Throwable t) {
                resetVerifyButton();
                Log.e(TAG, "Error verifying OTP: " + t.getMessage(), t);
                Toast.makeText(getContext(),
                    "Failed to verify code. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // Handle successful OTP verification
    private void handleOtpVerificationSuccess(VerifyEmailResponse response) {
        if (!response.isSuccess() || response.getData() == null) {
            String errorMsg = response.getMessage() != null ?
                response.getMessage() :
                "Invalid verification code";
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            return;
        }

        // Call RegisterActivity to handle success
        RegisterActivity activity = (RegisterActivity) getActivity();
        if (activity != null) {
            activity.handleOtpVerificationSuccess(response);
        }
    }
    
    // Handle OTP verification error
    private void handleOtpVerificationError(int statusCode) {
        String errorMsg = (statusCode == 400) ?
            "Invalid or expired code" :
            "Invalid verification code";
        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
    }
    
    // Reset verify button to original state
    @SuppressLint("SetTextI18n")
    private void resetVerifyButton() {
        if (verifyButton != null) {
            verifyButton.setEnabled(true);
            verifyButton.setText("Continue");
        }
    }
    
    // Resend OTP code
    @SuppressLint("SetTextI18n")
    private void resendOtpCode(String email) {
        ApiService apiService = ApiClient.getApiService();
        VerificationRequest request = new VerificationRequest(email);

        Call<VerificationResponse> call = apiService.sendVerificationCode(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<VerificationResponse> call, @NonNull Response<VerificationResponse> response) {
                resetResendButton();

                if (response.isSuccessful() && response.body() != null) {
                    VerificationResponse body = response.body();
                    if (body.isSuccess()) {
                        String message = body.getMessage() != null && !body.getMessage().isEmpty() ?
                            body.getMessage() : "Verification code sent to your email";
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    } else {
                        String errorMsg = body.getMessage() != null && !body.getMessage().isEmpty() ?
                            body.getMessage() : "Failed to send code";
                        Log.e(TAG, "OTP resend failed: " + errorMsg);
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "Failed to send code";
                    try {
                        if (response.errorBody() != null) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(response.errorBody().byteStream()));
                            String errorJson = reader.readLine();
                            if (errorJson != null) {
                                VerificationResponse errorResponse = gson.fromJson(errorJson, VerificationResponse.class);
                                if (errorResponse != null && errorResponse.getMessage() != null) {
                                    errorMsg = errorResponse.getMessage();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
                    }
                    Log.e(TAG, "OTP resend failed with status: " + response.code() + ", message: " + errorMsg);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<VerificationResponse> call, @NonNull Throwable t) {
                resetResendButton();
                Log.e(TAG, "Error resending OTP: " + t.getMessage(), t);
                String errorMsg = "Network error. Please try again.";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout") || t.getMessage().contains("Timeout")) {
                        errorMsg = "Request timeout. Please check your connection and try again.";
                    } else if (t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Cannot reach server. Please check your internet connection.";
                    }
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @SuppressLint("SetTextI18n")
    private void resetResendButton() {
        if (resendCodeButton != null) {
            resendCodeButton.setEnabled(true);
            resendCodeButton.setText("resend");
        }
    }
}
