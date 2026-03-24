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
        Log.d(TAG, "Mocking OTP send to email: " + email);
        Toast.makeText(getContext(), "Verification code sent to " + email, Toast.LENGTH_SHORT).show();
    }
    
    // Verify OTP code
    private void verifyOtpCode(String email, String code) {
        Log.d(TAG, "Mocking OTP verification for code: " + code);
        
        // Simulate a short delay
        new android.os.Handler().postDelayed(() -> {
            resetVerifyButton();
            // Always succeed for now as we are removing Laravel
            RegisterActivity activity = (RegisterActivity) getActivity();
            if (activity != null) {
                activity.onOtpVerified();
            }
        }, 1000);
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
    private void resendOtpCode(String email) {
        Log.d(TAG, "Mocking OTP resend to email: " + email);
        
        new android.os.Handler().postDelayed(() -> {
            if (resendCodeButton != null) {
                resendCodeButton.setEnabled(true);
                resendCodeButton.setText("Resend Code");
            }
            Toast.makeText(getContext(), "Verification code resent!", Toast.LENGTH_SHORT).show();
        }, 1500);
    }
}
