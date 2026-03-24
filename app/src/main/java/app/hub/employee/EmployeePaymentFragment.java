package app.hub.employee;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import app.hub.R;
import app.hub.api.PaymentDetailResponse;
import app.hub.util.TokenManager;

/**
 * Full-width fragment version of the payment confirmation UI.
 */
public class EmployeePaymentFragment extends Fragment {

    public interface OnPaymentConfirmedListener {
        void onPaymentConfirmed(String paymentMethod, double amount, String notes);
    }

    public interface OnPaymentRequestListener {
        void onPaymentRequested(double amount, String notes);
    }

    private static final String ARG_TICKET_ID = "ticket_id";
    private static final String ARG_CUSTOMER_NAME = "customer_name";
    private static final String ARG_SERVICE_NAME = "service_name";
    private static final String ARG_TOTAL_AMOUNT = "total_amount";
    private static final String ARG_REQUEST_ONLY = "request_only";

    private String ticketId;
    private String customerName;
    private String serviceName;
    private double totalAmount;
    private boolean requestOnly;

    private TextView tvTicketId;
    private TextView tvCustomerName;
    private TextView tvServiceName;
    private TextView tvTotalAmount;
    private MaterialButton btnPaymentConfirmed;
    private MaterialButton btnRequestOnlinePayment;
    private TokenManager tokenManager;
    private boolean isPaymentLoading = false;
    private String confirmButtonText = "Cash Received";

    public static EmployeePaymentFragment newInstance(String ticketId, String customerName,
            String serviceName, double totalAmount, boolean requestOnly) {
        EmployeePaymentFragment fragment = new EmployeePaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TICKET_ID, ticketId);
        args.putString(ARG_CUSTOMER_NAME, customerName);
        args.putString(ARG_SERVICE_NAME, serviceName);
        args.putDouble(ARG_TOTAL_AMOUNT, totalAmount);
        args.putBoolean(ARG_REQUEST_ONLY, requestOnly);
        fragment.setArguments(args);
        return fragment;
    }

    public static EmployeePaymentFragment newInstance(String ticketId, String customerName,
            String serviceName, double totalAmount) {
        return newInstance(ticketId, customerName, serviceName, totalAmount, false);
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            ticketId = getArguments().getString(ARG_TICKET_ID);
            customerName = getArguments().getString(ARG_CUSTOMER_NAME);
            serviceName = getArguments().getString(ARG_SERVICE_NAME);
            totalAmount = getArguments().getDouble(ARG_TOTAL_AMOUNT, 0.0);
            requestOnly = getArguments().getBoolean(ARG_REQUEST_ONLY, false);
        }
        return inflater.inflate(R.layout.fragment_employee_work_confirmpayment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        TextView tvPaymentHeader = view.findViewById(R.id.tvPaymentHeader);
        TextView tvConfirmationStatus = view.findViewById(R.id.tvConfirmationStatus);
        tvTicketId = view.findViewById(R.id.tvTicketId);
        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        tvServiceName = view.findViewById(R.id.tvServiceName);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        btnPaymentConfirmed = view.findViewById(R.id.btnPaymentConfirmed);
        btnRequestOnlinePayment = view.findViewById(R.id.btnRequestOnlinePayment);
        tokenManager = new TokenManager(requireContext());

        if (btnPaymentConfirmed != null && btnPaymentConfirmed.getText() != null) {
            confirmButtonText = btnPaymentConfirmed.getText().toString();
        }

        if (tvTicketId != null) {
            tvTicketId.setText(ticketId != null ? ticketId : "");
        }
        if (tvCustomerName != null) {
            tvCustomerName.setText(customerName != null ? customerName : "");
        }
        if (tvServiceName != null) {
            tvServiceName.setText(serviceName != null ? serviceName : "");
        }
        if (tvTotalAmount != null) {
            String amountText = "Php " + String.format("%.2f", totalAmount);
            tvTotalAmount.setText(amountText);
        }

        if (requestOnly) {
            if (btnPaymentConfirmed != null) {
                btnPaymentConfirmed.setVisibility(View.GONE);
            }
            if (tvPaymentHeader != null) {
                tvPaymentHeader.setText("REQUEST PAYMENT");
            }
            if (tvConfirmationStatus != null) {
                tvConfirmationStatus.setText("Request online payment from the customer.");
            }
        }
    }

    private void setupClickListeners() {
        loadPaymentDetailsIfNeeded();
        if (btnRequestOnlinePayment != null) {
            btnRequestOnlinePayment.setOnClickListener(v -> {
                if (isPaymentLoading || totalAmount <= 0) {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(),
                                "Amount not ready yet. Please wait.",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                
                boolean listenerCalled = false;
                
                // Try activity first
                if (getActivity() instanceof OnPaymentRequestListener) {
                    ((OnPaymentRequestListener) getActivity())
                            .onPaymentRequested(totalAmount, "");
                    listenerCalled = true;
                }
                // Then try parent fragment
                else if (getParentFragment() instanceof OnPaymentRequestListener) {
                    ((OnPaymentRequestListener) getParentFragment())
                            .onPaymentRequested(totalAmount, "");
                    listenerCalled = true;
                }
                
                if (!listenerCalled && getContext() != null) {
                    android.widget.Toast.makeText(getContext(),
                            "Unable to request payment right now.",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Close the fragment after successful request
                if (getActivity() != null && getActivity().getSupportFragmentManager() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        if (btnPaymentConfirmed != null) {
            btnPaymentConfirmed.setOnClickListener(v -> {
                if (isPaymentLoading || totalAmount <= 0) {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(),
                                "Amount not ready yet. Please wait.",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                
                boolean listenerCalled = false;
                
                // Try activity first
                if (getActivity() instanceof OnPaymentConfirmedListener) {
                    ((OnPaymentConfirmedListener) getActivity())
                            .onPaymentConfirmed("cash", totalAmount, "");
                    listenerCalled = true;
                }
                // Then try parent fragment
                else if (getParentFragment() instanceof OnPaymentConfirmedListener) {
                    ((OnPaymentConfirmedListener) getParentFragment())
                            .onPaymentConfirmed("cash", totalAmount, "");
                    listenerCalled = true;
                }

                if (listenerCalled && getActivity() != null && getActivity().getSupportFragmentManager() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void loadPaymentDetailsIfNeeded() {
        if (ticketId == null || tokenManager == null) {
            return;
        }
        if (totalAmount > 0) {
            return;
        }

        setPaymentLoading(true);
        String token = tokenManager.getToken();
        if (token == null) {
            setPaymentLoading(false);
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<PaymentDetailResponse> call = apiService.getPaymentByTicketId("Bearer " + token, ticketId);
        call.enqueue(new Callback<PaymentDetailResponse>() {
            @Override
            public void onResponse(Call<PaymentDetailResponse> call, Response<PaymentDetailResponse> response) {
                if (!isAdded() || response.body() == null || !response.body().isSuccess()) {
                    setPaymentLoading(false);
                    return;
                }

                PaymentDetailResponse.PaymentDetail payment = response.body().getPayment();
                if (payment == null) {
                    setPaymentLoading(false);
                    return;
                }

                totalAmount = payment.getAmount();
                if (tvTotalAmount != null) {
                    String amountText = "Php " + String.format("%.2f", totalAmount);
                    tvTotalAmount.setText(amountText);
                }

                if (tvServiceName != null && (serviceName == null || serviceName.trim().isEmpty())) {
                    serviceName = payment.getServiceName();
                    if (serviceName != null) {
                        tvServiceName.setText(serviceName);
                    }
                }

                if (tvCustomerName != null && (customerName == null || customerName.trim().isEmpty())) {
                    customerName = payment.getCustomerName();
                    if (customerName != null) {
                        tvCustomerName.setText(customerName);
                    }
                }

                setPaymentLoading(false);
            }

            @Override
            public void onFailure(Call<PaymentDetailResponse> call, Throwable t) {
                setPaymentLoading(false);
                // Ignore to keep UI stable; amount will stay as-is.
            }
        });
    }

    private void setPaymentLoading(boolean loading) {
        isPaymentLoading = loading;
        if (btnPaymentConfirmed != null) {
            btnPaymentConfirmed.setEnabled(!loading);
            btnPaymentConfirmed.setText(loading ? "Loading..." : confirmButtonText);
        }
        if (btnRequestOnlinePayment != null) {
            btnRequestOnlinePayment.setEnabled(!loading);
        }
    }
}

