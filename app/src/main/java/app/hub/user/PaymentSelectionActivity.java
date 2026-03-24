package app.hub.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.hub.R;
import app.hub.api.PaymentConfirmationBody;
import app.hub.api.PaymentConfirmationResponse;
import app.hub.api.PaymentMethod;
import app.hub.util.TokenManager;

public class PaymentSelectionActivity extends AppCompatActivity {
    private static final String TAG = "PaymentSelection";

    private TextView tvTicketId, tvServiceType, tvAmount;
    private RadioGroup rgPaymentMethods;
    private RadioButton rbCash, rbCreditCard, rbGPay, rbBankTransfer;
    private Button btnConfirmPayment;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private String ticketId;
    private String serviceType;
    private double amount;
    private int customerId;
    private PaymentMethod selectedPaymentMethod;

    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_selection);

        Log.d(TAG, "onCreate() - PaymentSelectionActivity started");

        // Initialize TokenManager and ApiService
        tokenManager = new TokenManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        // Get data from intent
        Intent intent = getIntent();
        ticketId = intent.getStringExtra("ticket_id");
        serviceType = intent.getStringExtra("service_type");
        amount = intent.getDoubleExtra("amount", 0.0);
        customerId = intent.getIntExtra("customer_id", 0);
        
        Log.d(TAG, "Intent data - Ticket: " + ticketId + ", Service: " + serviceType + 
                ", Amount: " + amount + ", Customer: " + customerId);
        
        // Cancel notification when this activity opens
        if (ticketId != null) {
            app.hub.util.NotificationHelper.cancelNotification(this, ticketId);
            Log.d(TAG, "Notification cancelled for ticket: " + ticketId);
        }

        // Initialize views
        initializeViews();

        // Display ticket data
        displayTicketData();

        // Setup listeners
        setupListeners();
        
        Log.d(TAG, "onCreate() completed - Activity ready for user interaction");
    }

    private void initializeViews() {
        tvTicketId = findViewById(R.id.tvTicketId);
        tvServiceType = findViewById(R.id.tvServiceType);
        tvAmount = findViewById(R.id.tvAmount);
        rgPaymentMethods = findViewById(R.id.rgPaymentMethods);
        rbCash = findViewById(R.id.rbCash);
        rbCreditCard = findViewById(R.id.rbCreditCard);
        rbGPay = findViewById(R.id.rbGPay);
        rbBankTransfer = findViewById(R.id.rbBankTransfer);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
    }

    private void displayTicketData() {
        // Validate data
        if (ticketId == null || ticketId.isEmpty()) {
            Toast.makeText(this, "Invalid ticket ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        if (customerId == 0) {
            // Get customer ID from token if not provided
            customerId = tokenManager.getUserIdInt();
            Log.d(TAG, "Customer ID from token: " + customerId);
        }
        
        if (customerId == 0) {
            Toast.makeText(this, "Invalid customer ID. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        tvTicketId.setText(ticketId);
        tvServiceType.setText(serviceType != null ? serviceType : "N/A");
        tvAmount.setText(String.format("₱%.2f", amount));
        
        // Cash is pre-selected by default in layout
        selectedPaymentMethod = PaymentMethod.CASH;
        btnConfirmPayment.setEnabled(true);
        
        Log.d(TAG, "Payment form loaded - Ticket: " + ticketId + ", Amount: " + amount + ", Customer: " + customerId);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Payment method selection
        rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            validateSelection();
        });

        // Add explicit click listeners to ensure mutual exclusivity
        rbCash.setOnClickListener(v -> {
            rgPaymentMethods.check(R.id.rbCash);
            validateSelection();
        });

        rbCreditCard.setOnClickListener(v -> {
            rgPaymentMethods.check(R.id.rbCreditCard);
            validateSelection();
        });

        rbGPay.setOnClickListener(v -> {
            rgPaymentMethods.check(R.id.rbGPay);
            validateSelection();
        });

        rbBankTransfer.setOnClickListener(v -> {
            rgPaymentMethods.check(R.id.rbBankTransfer);
            validateSelection();
        });

        // Confirm payment button
        btnConfirmPayment.setOnClickListener(v -> confirmPayment());
    }

    private void validateSelection() {
        int selectedId = rgPaymentMethods.getCheckedRadioButtonId();
        
        if (selectedId == R.id.rbCash) {
            selectedPaymentMethod = PaymentMethod.CASH;
            btnConfirmPayment.setEnabled(true);
        } else if (selectedId == R.id.rbCreditCard) {
            selectedPaymentMethod = PaymentMethod.CREDIT_CARD;
            btnConfirmPayment.setEnabled(true);
        } else if (selectedId == R.id.rbGPay) {
            selectedPaymentMethod = PaymentMethod.GPAY;
            btnConfirmPayment.setEnabled(true);
        } else if (selectedId == R.id.rbBankTransfer) {
            selectedPaymentMethod = PaymentMethod.BANK_TRANSFER;
            btnConfirmPayment.setEnabled(true);
        } else {
            selectedPaymentMethod = null;
            btnConfirmPayment.setEnabled(false);
        }
    }

    private void confirmPayment() {
        Log.d(TAG, "confirmPayment() called");
        
        if (selectedPaymentMethod == null) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Confirming payment - Method: " + selectedPaymentMethod.getValue() + 
                ", Ticket: " + ticketId + ", Amount: " + amount + ", Customer: " + customerId);

        // Show loading
        showLoading(true);

        // Create request body
        PaymentConfirmationBody requestBody = new PaymentConfirmationBody(
                ticketId,
                customerId,
                selectedPaymentMethod.getValue(),
                amount
        );

        // Get token
        String token = tokenManager.getToken();
        if (token == null || token.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No token found");
            return;
        }

        Log.d(TAG, "Sending payment confirmation to API...");

        // Call API
        Call<PaymentConfirmationResponse> call = apiService.confirmPayment("Bearer " + token, requestBody);
        call.enqueue(new Callback<PaymentConfirmationResponse>() {
            @Override
            public void onResponse(Call<PaymentConfirmationResponse> call, Response<PaymentConfirmationResponse> response) {
                showLoading(false);
                
                Log.d(TAG, "API Response - Code: " + response.code() + ", Success: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    PaymentConfirmationResponse result = response.body();
                    
                    Log.d(TAG, "Response body - Success: " + result.isSuccess() + ", Message: " + result.getMessage());
                    
                    if (result.isSuccess()) {
                        handleConfirmationResponse(result);
                    } else {
                        Toast.makeText(PaymentSelectionActivity.this, 
                                result.getMessage() != null ? result.getMessage() : "Payment confirmation failed", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Payment confirmation failed: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body", e);
                    }
                    Toast.makeText(PaymentSelectionActivity.this, 
                            "Payment confirmation failed. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentConfirmationResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(PaymentSelectionActivity.this, 
                        "Network error. Please check your connection and try again.", 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleConfirmationResponse(PaymentConfirmationResponse response) {
        // Show success message
        Toast.makeText(this, "Payment confirmed successfully!", Toast.LENGTH_LONG).show();

        // Return to previous screen (Activity Tab)
        showSuccessAndFinish();
    }

    private void showSuccessAndFinish() {
        // Set result to indicate success
        Intent resultIntent = new Intent();
        resultIntent.putExtra("payment_confirmed", true);
        resultIntent.putExtra("ticket_id", ticketId);
        setResult(RESULT_OK, resultIntent);
        
        // Finish activity
        finish();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnConfirmPayment.setEnabled(false);
            rgPaymentMethods.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnConfirmPayment.setEnabled(selectedPaymentMethod != null);
            rgPaymentMethods.setEnabled(true);
        }
    }
}
