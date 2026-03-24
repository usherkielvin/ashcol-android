package app.hub.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import app.hub.R;
import app.hub.common.FirestoreManager;
import app.hub.util.TokenManager;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class UserPaymentFragment extends Fragment {

    private static final String ARG_TICKET_ID = "ticket_id";
    private static final String ARG_PAYMENT_ID = "payment_id";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_SERVICE_NAME = "service_name";
    private static final String ARG_TECH_NAME = "technician_name";

    private TokenManager tokenManager;
    private FirestoreManager firestoreManager;

    private String ticketId;
    private double amount = 0.0;
    private String serviceName = "Service";
    private String technicianName = "Technician";
    private String paymentMethod;

    public UserPaymentFragment() {
        // Required empty public constructor
    }

    public static UserPaymentFragment newInstance() {
        return new UserPaymentFragment();
    }

    public static UserPaymentFragment newInstance(String ticketId, int paymentId, double amount,
            String serviceName, String technicianName) {
        UserPaymentFragment fragment = new UserPaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TICKET_ID, ticketId);
        args.putInt(ARG_PAYMENT_ID, paymentId);
        args.putDouble(ARG_AMOUNT, amount);
        args.putString(ARG_SERVICE_NAME, serviceName);
        args.putString(ARG_TECH_NAME, technicianName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioButton rbCash = view.findViewById(R.id.rbCash);
        RadioButton rbGpay = view.findViewById(R.id.rbGpay);
        RadioButton rbCreditCard = view.findViewById(R.id.rbCreditCard);
        com.google.android.material.card.MaterialCardView cardCash = view.findViewById(R.id.cardCash);
        com.google.android.material.card.MaterialCardView cardGpay = view.findViewById(R.id.cardGpay);
        com.google.android.material.card.MaterialCardView cardCreditCard = view.findViewById(R.id.cardCreditCard);
        com.google.android.material.button.MaterialButton btnContinuePayment = view.findViewById(R.id.btnContinuePayment);

        TextView tvTicketId = view.findViewById(R.id.tvTicketId);
        TextView tvServiceName = view.findViewById(R.id.tvServiceName);
        TextView tvTechnicianName = view.findViewById(R.id.tvTechnicianName);
        TextView tvAmountDue = view.findViewById(R.id.tvAmountDue);

        tokenManager = new TokenManager(requireContext());
        firestoreManager = new FirestoreManager(requireContext());

        if (getArguments() != null) {
            ticketId = getArguments().getString(ARG_TICKET_ID);
            amount = getArguments().getDouble(ARG_AMOUNT, 0.0);
            serviceName = getArguments().getString(ARG_SERVICE_NAME);
            technicianName = getArguments().getString(ARG_TECH_NAME);
        }

        if (ticketId != null) {
            tvTicketId.setText(ticketId);
        }
        if (serviceName != null) {
            tvServiceName.setText(serviceName);
        }
        if (technicianName != null) {
            tvTechnicianName.setText(technicianName);
        }
        if (amount > 0) {
            tvAmountDue.setText(formatAmount(amount));
        } else {
            tvAmountDue.setText("Loading payment...");
        }

        btnContinuePayment.setText("Loading...");
        btnContinuePayment.setEnabled(false);
        listenForPendingPayment(tvServiceName, tvTechnicianName, tvAmountDue, btnContinuePayment);
        fetchPaymentDetails(tvServiceName, tvTechnicianName, tvAmountDue, btnContinuePayment);

        setupPaymentSelection(cardCash, cardGpay, cardCreditCard, rbCash, rbGpay, rbCreditCard);

        btnContinuePayment.setOnClickListener(v -> {
            boolean cashChecked = rbCash != null && rbCash.isChecked();
            boolean gpayChecked = rbGpay != null && rbGpay.isChecked();
            boolean cardChecked = rbCreditCard != null && rbCreditCard.isChecked();

            if (!cashChecked && !gpayChecked && !cardChecked) {
                Toast.makeText(getContext(), "Please select a payment method.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                Toast.makeText(getContext(), "Payment details not ready yet.", Toast.LENGTH_SHORT).show();
                return;
            }

            String method;
            if (cashChecked) {
                method = "Cash";
                // For cash payment, show confirmation and close
                Toast.makeText(getContext(), "Cash payment selected. Payment will be collected by technician.", Toast.LENGTH_LONG).show();
                if (getActivity() != null) {
                    getActivity().finish();
                }
                return;
            } else if (gpayChecked) {
                method = "Google Pay";
            } else {
                method = "Credit Card";
            }

            // For online payments (Google Pay, Credit Card), proceed with payment
            completePayment(method);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreManager != null) {
            firestoreManager.stopPaymentListening();
        }
    }

    private void listenForPendingPayment(TextView tvServiceName, TextView tvTechnicianName,
            TextView tvAmountDue, com.google.android.material.button.MaterialButton btnContinuePayment) {
        firestoreManager.listenToPendingPayment(ticketId, new FirestoreManager.PendingPaymentListener() {
            @Override
            public void onPaymentUpdated(FirestoreManager.PendingPayment payment) {
                amount = payment.amount;
                if (payment.serviceName != null) {
                    serviceName = payment.serviceName;
                }
                if (payment.technicianName != null) {
                    technicianName = payment.technicianName;
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (serviceName != null) {
                            tvServiceName.setText(serviceName);
                        }
                        if (technicianName != null) {
                            tvTechnicianName.setText(technicianName);
                        }
                        if (amount > 0) {
                            tvAmountDue.setText(formatAmount(amount));
                        }
                        btnContinuePayment.setText("Continue");
                        btnContinuePayment.setEnabled(amount > 0);
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("UserPayment", "Failed to load payment", e);
            }
        });
    }

    private void fetchPaymentDetails(TextView tvServiceName, TextView tvTechnicianName,
            TextView tvAmountDue, com.google.android.material.button.MaterialButton btnContinuePayment) {
        if (ticketId == null) return;

        android.util.Log.d("UserPayment", "Loading payment details from Firestore for ticket: " + ticketId);

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded()) return;
                
                if (documentSnapshot.exists()) {
                    Double amountVal = documentSnapshot.getDouble("total_amount");
                    String service = documentSnapshot.getString("service_type");
                    String tech = documentSnapshot.getString("assigned_staff_name");
                    String status = documentSnapshot.getString("payment_status");
                    String method = documentSnapshot.getString("payment_method");

                    amount = amountVal != null ? amountVal : 0.0;
                    serviceName = service != null ? service : "Service";
                    technicianName = tech != null ? tech : "Technician";
                    paymentMethod = method;

                    if (getActivity() != null) {
                        tvServiceName.setText(serviceName);
                        tvTechnicianName.setText(technicianName);
                        tvAmountDue.setText(formatAmount(amount));

                        boolean pending = status == null || status.equalsIgnoreCase("pending");

                        if (isCashPayment(paymentMethod)) {
                            btnContinuePayment.setText("Awaiting Cash Collection");
                            btnContinuePayment.setEnabled(false);
                        } else {
                            btnContinuePayment.setText("Continue");
                            btnContinuePayment.setEnabled(pending && amount > 0);
                        }

                        if (!pending && amount > 0) {
                            tvAmountDue.setText("Paid");
                        }
                    }
                } else {
                    tvAmountDue.setText("No pending payment");
                    btnContinuePayment.setEnabled(false);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("UserPayment", "Failed to load payment from Firestore: " + e.getMessage());
            });
    }

    private void completePayment(String method) {
        if (ticketId == null) return;

        android.util.Log.d("UserPayment", "Completing payment in Firestore for ticket: " + ticketId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("payment_status", "paid");
        updates.put("payment_method", method);
        updates.put("payment_at", com.google.firebase.Timestamp.now());
        updates.put("status", "completed"); // Mark ticket as completed when paid

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                if (!isAdded()) return;
                
                UserPaymentSuccessFragment successFragment = UserPaymentSuccessFragment.newInstance();
                Bundle args = new Bundle();
                args.putString("payment_method", method);
                args.putString("ticket_id", ticketId);
                args.putString("service_name", serviceName);
                args.putString("technician_name", technicianName);
                args.putDouble("amount", amount);
                successFragment.setArguments(args);

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainerView, successFragment)
                            .addToBackStack(null)
                            .commit();
                }
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private boolean isOnlinePayment(String method) {
        return method != null && method.equalsIgnoreCase("online");
    }

    private boolean isCashPayment(String method) {
        return method != null && method.equalsIgnoreCase("cash");
    }

    private String formatAmount(double value) {
        return "Php " + String.format(java.util.Locale.getDefault(), "%,.2f", value);
    }

    private void setupPaymentSelection(
            com.google.android.material.card.MaterialCardView cardCash,
            com.google.android.material.card.MaterialCardView cardGpay,
            com.google.android.material.card.MaterialCardView cardCreditCard,
            RadioButton rbCash,
            RadioButton rbGpay,
            RadioButton rbCreditCard) {
        View.OnClickListener cashClick = v -> selectPayment(rbCash, rbGpay, rbCreditCard);
        View.OnClickListener gpayClick = v -> selectPayment(rbGpay, rbCash, rbCreditCard);
        View.OnClickListener cardClick = v -> selectPayment(rbCreditCard, rbCash, rbGpay);

        if (cardCash != null) cardCash.setOnClickListener(cashClick);
        if (cardGpay != null) cardGpay.setOnClickListener(gpayClick);
        if (cardCreditCard != null) cardCreditCard.setOnClickListener(cardClick);

        if (rbCash != null) rbCash.setOnClickListener(cashClick);
        if (rbGpay != null) rbGpay.setOnClickListener(gpayClick);
        if (rbCreditCard != null) rbCreditCard.setOnClickListener(cardClick);

        if (rbCash != null && rbGpay != null && rbCreditCard != null) {
            if (!rbCash.isChecked() && !rbGpay.isChecked() && !rbCreditCard.isChecked()) {
                rbCash.setChecked(true);
                rbGpay.setChecked(false);
                rbCreditCard.setChecked(false);
            }
        }
    }

    private void selectPayment(RadioButton selected, RadioButton other1, RadioButton other2) {
        if (selected != null) {
            selected.setChecked(true);
        }
        if (other1 != null) {
            other1.setChecked(false);
        }
        if (other2 != null) {
            other2.setChecked(false);
        }
    }
}
