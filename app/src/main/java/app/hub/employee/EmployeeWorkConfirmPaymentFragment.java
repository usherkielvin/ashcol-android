package app.hub.employee;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import app.hub.R;
import app.hub.api.CompleteWorkResponse;
import app.hub.api.PaymentDetailResponse;
import app.hub.common.FirestoreManager;
import app.hub.util.TokenManager;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EmployeeWorkConfirmPaymentFragment extends Fragment {

    private static final String ARG_TICKET_ID = "ticket_id";
    private static final String ARG_CUSTOMER_NAME = "customer_name";
    private static final String ARG_SERVICE_NAME = "service_name";
    private static final String ARG_AMOUNT = "amount";

    private String ticketId;
    private String customerName;
    private String serviceName;
    private double amount;
    private TokenManager tokenManager;
    private FirestoreManager firestoreManager;
    private TextView tvPaymentStatus;
    private com.google.android.material.button.MaterialButton btnPaymentConfirmed;

    public EmployeeWorkConfirmPaymentFragment() {
        // Required empty public constructor
    }

    public static EmployeeWorkConfirmPaymentFragment newInstance(String ticketId, String customerName,
                                                                   String serviceName, double amount) {
        EmployeeWorkConfirmPaymentFragment fragment = new EmployeeWorkConfirmPaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TICKET_ID, ticketId);
        args.putString(ARG_CUSTOMER_NAME, customerName);
        args.putString(ARG_SERVICE_NAME, serviceName);
        args.putDouble(ARG_AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ticketId = getArguments().getString(ARG_TICKET_ID);
            customerName = getArguments().getString(ARG_CUSTOMER_NAME);
            serviceName = getArguments().getString(ARG_SERVICE_NAME);
            amount = getArguments().getDouble(ARG_AMOUNT, 0.0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_employee_work_confirmpayment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());
        firestoreManager = new FirestoreManager(requireContext());

        TextView tvTicketId = view.findViewById(R.id.tvTicketId);
        TextView tvCustomerName = view.findViewById(R.id.tvCustomerName);
        TextView tvServiceName = view.findViewById(R.id.tvServiceName);
        TextView tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvPaymentStatus = view.findViewById(R.id.tvConfirmationStatus);
        btnPaymentConfirmed = view.findViewById(R.id.btnPaymentConfirmed);

        // Set ticket details
        if (ticketId != null) {
            tvTicketId.setText(ticketId);
        }
        if (customerName != null) {
            tvCustomerName.setText(customerName);
        }
        if (serviceName != null) {
            tvServiceName.setText(serviceName);
        }
        if (amount > 0) {
            tvTotalAmount.setText(formatAmount(amount));
        }

        // Setup cash received button
        if (btnPaymentConfirmed != null) {
            btnPaymentConfirmed.setText("Cash Received");
            btnPaymentConfirmed.setVisibility(View.GONE); // Initially hidden
            btnPaymentConfirmed.setEnabled(false);
            btnPaymentConfirmed.setOnClickListener(v -> confirmCashPayment());
        }

        // Load payment details and start listening for changes
        loadPaymentDetails();
        startPaymentListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreManager != null) {
            firestoreManager.stopPaymentListening();
        }
    }

    private void loadPaymentDetails() {
        if (ticketId == null) return;

        android.util.Log.d("EmployeePaymentConfirm", "Loading payment details from Firestore: " + ticketId);

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded()) return;
                
                if (documentSnapshot.exists()) {
                    String status = documentSnapshot.getString("payment_status");
                    String method = documentSnapshot.getString("payment_method");
                    updatePaymentUI(status, method);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("EmployeePaymentConfirm", "Failed to load payment from Firestore: " + e.getMessage());
            });
    }

    private void startPaymentListener() {
        if (ticketId == null || firestoreManager == null) {
            return;
        }

        // Listen to payment by ticket ID (any status) to catch status changes
        firestoreManager.listenToPaymentByTicket(ticketId, new FirestoreManager.PendingPaymentListener() {
            @Override
            public void onPaymentUpdated(FirestoreManager.PendingPayment payment) {
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                String status = payment.status;
                String method = payment.paymentMethod;

                getActivity().runOnUiThread(() -> updatePaymentUI(status, method));
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("EmployeePaymentConfirm", "Payment listener error", e);
            }
        });
    }

    private void updatePaymentUI(String status, String method) {
        if (!isAdded()) {
            return;
        }

        // Check if payment is completed or collected
        if ("completed".equalsIgnoreCase(status) || "collected".equalsIgnoreCase(status)) {
            // Payment completed - auto close and mark ticket as completed
            if (tvPaymentStatus != null) {
                tvPaymentStatus.setText("Payment received! Completing job...");
            }
            
            Toast.makeText(getContext(), "Payment received successfully!", Toast.LENGTH_SHORT).show();
            
            // Close fragment after short delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }, 1500);
            return;
        }

        // Check payment method
        if ("cash".equalsIgnoreCase(method)) {
            // Customer selected cash - enable cash received button and show it
            if (btnPaymentConfirmed != null) {
                btnPaymentConfirmed.setVisibility(View.VISIBLE);
                btnPaymentConfirmed.setEnabled(true);
                btnPaymentConfirmed.setText("Cash Received");
            }
            if (tvPaymentStatus != null) {
                tvPaymentStatus.setText("Customer selected cash payment. Click 'Cash Received' when payment is collected.");
            }
        } else if ("online".equalsIgnoreCase(method) || "gpay".equalsIgnoreCase(method) || 
                   "google pay".equalsIgnoreCase(method) || "credit card".equalsIgnoreCase(method) || "bank transfer".equalsIgnoreCase(method)) {
            // Customer selected online payment - hide button and wait for auto-completion
            if (btnPaymentConfirmed != null) {
                btnPaymentConfirmed.setVisibility(View.GONE);
            }
            if (tvPaymentStatus != null) {
                tvPaymentStatus.setText("Customer selected online payment. This screen will close automatically when payment is confirmed.");
            }
        } else {
            // Pending - waiting for customer to select method
            if (btnPaymentConfirmed != null) {
                btnPaymentConfirmed.setVisibility(View.GONE);
            }
            if (tvPaymentStatus != null) {
                tvPaymentStatus.setText("Waiting for customer to select payment method...");
            }
        }
    }

    private void confirmCashPayment() {
        if (ticketId == null) return;

        // Disable button while processing
        if (btnPaymentConfirmed != null) {
            btnPaymentConfirmed.setEnabled(false);
            btnPaymentConfirmed.setText("Processing...");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("payment_status", "paid");
        updates.put("status", "completed");
        updates.put("completed_at", com.google.firebase.Timestamp.now());
        updates.put("updated_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                if (!isAdded()) return;
                
                Toast.makeText(getContext(), "Cash payment confirmed!", Toast.LENGTH_SHORT).show();
                
                // Close fragment
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                
                Toast.makeText(getContext(), "Failed to confirm payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                if (btnPaymentConfirmed != null) {
                    btnPaymentConfirmed.setEnabled(true);
                    btnPaymentConfirmed.setText("Cash Received");
                }
            });
    }

    private String formatAmount(double value) {
        return "Php " + String.format(java.util.Locale.getDefault(), "%,.2f", value);
    }
}
