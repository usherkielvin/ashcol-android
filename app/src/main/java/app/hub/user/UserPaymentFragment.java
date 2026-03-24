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
import app.hub.api.CompleteWorkResponse;
import app.hub.api.PaymentDetailResponse;
import app.hub.common.FirestoreManager;
import app.hub.util.TokenManager;

public class UserPaymentFragment extends Fragment {

    private static final String ARG_TICKET_ID = "ticket_id";
    private static final String ARG_PAYMENT_ID = "payment_id";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_SERVICE_NAME = "service_name";
    private static final String ARG_TECH_NAME = "technician_name";

    private TokenManager tokenManager;
    private FirestoreManager firestoreManager;

    private String ticketId;
    private int paymentId;
    private double amount;
    private String serviceName;
    private String technicianName;
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
            paymentId = getArguments().getInt(ARG_PAYMENT_ID, 0);
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
        btnContinuePayment.setEnabled(paymentId > 0 && amount > 0);
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

            if (paymentId <= 0) {
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
                paymentId = payment.paymentId;
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
                        btnContinuePayment.setEnabled(paymentId > 0 && amount > 0);
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
        if (ticketId == null) {
            return;
        }

        String token = tokenManager.getToken();
        if (token == null) {
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<PaymentDetailResponse> call = apiService.getPaymentByTicketId("Bearer " + token, ticketId);

        call.enqueue(new Callback<PaymentDetailResponse>() {
            @Override
            public void onResponse(Call<PaymentDetailResponse> call, Response<PaymentDetailResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvAmountDue.setText("No pending payment");
                            btnContinuePayment.setText("Continue");
                            btnContinuePayment.setEnabled(false);
                        });
                    }
                    return;
                }

                PaymentDetailResponse.PaymentDetail payment = response.body().getPayment();
                if (payment == null) {
                    return;
                }

                paymentId = payment.getId();
                amount = payment.getAmount();
                serviceName = payment.getServiceName();
                technicianName = payment.getTechnicianName();
                paymentMethod = payment.getPaymentMethod();

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

                        boolean pending = payment.getStatus() != null
                                && payment.getStatus().equalsIgnoreCase("pending");

                        if (isCashPayment(paymentMethod)) {
                            btnContinuePayment.setText("Awaiting Cash Collection");
                            btnContinuePayment.setEnabled(false);
                        } else {
                            btnContinuePayment.setText("Continue");
                            btnContinuePayment.setEnabled(pending && paymentId > 0 && amount > 0);
                        }

                        if (!pending && amount > 0) {
                            tvAmountDue.setText("Paid");
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<PaymentDetailResponse> call, Throwable t) {
                android.util.Log.e("UserPayment", "Failed to load payment", t);
            }
        });
    }

    private void completePayment(String method) {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<CompleteWorkResponse> call = apiService.payCustomerPayment("Bearer " + token, paymentId);

        call.enqueue(new Callback<CompleteWorkResponse>() {
            @Override
            public void onResponse(Call<CompleteWorkResponse> call, Response<CompleteWorkResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Payment failed. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserPaymentSuccessFragment successFragment = UserPaymentSuccessFragment.newInstance();
                Bundle args = new Bundle();
                args.putString("payment_method", method);
                args.putString("ticket_id", ticketId);
                args.putString("service_name", serviceName);
                args.putString("technician_name", technicianName);
                args.putDouble("amount", amount);
                successFragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainerView, successFragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFailure(Call<CompleteWorkResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error. Try again.", Toast.LENGTH_SHORT).show();
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
