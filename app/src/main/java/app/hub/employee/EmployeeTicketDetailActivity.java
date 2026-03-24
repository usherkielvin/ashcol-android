package app.hub.employee;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import app.hub.R;
import app.hub.api.TicketDetailResponse;
import app.hub.api.UpdateTicketStatusRequest;
import app.hub.api.UpdateTicketStatusResponse;
import app.hub.api.CompleteWorkRequest;
import app.hub.api.CompleteWorkResponse;
import app.hub.util.TokenManager;

public class EmployeeTicketDetailActivity extends AppCompatActivity
    implements EmployeePaymentFragment.OnPaymentConfirmedListener,
    EmployeePaymentFragment.OnPaymentRequestListener,
    OnMapReadyCallback {

        private TextView tvTicketId, tvTitle, tvDescription, tvServiceType, tvUnitType, tvAddress, tvContact, tvStatus, tvCustomerName,
            tvCreatedAt, tvScheduleDate, tvScheduleTime, tvScheduleNotes;
        private TextView tvPaymentStatus, tvPaymentMethod, tvPaymentAmount, tvPaymentCollectedBy;
        private View paymentCard;
        private View contentContainer;
    private Button btnViewMap, btnStartWork, btnCompleteWork;
    private ImageButton btnBack;
    private View mapCardContainer;
    private TokenManager tokenManager;
    private String ticketId;
    private double customerLatitude, customerLongitude;
    private TicketDetailResponse.TicketDetail currentTicket;
    private FusedLocationProviderClient fusedLocationClient;

    private MapView mapView;
    private GoogleMap googleMap;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String EXTRA_OPEN_PAYMENT = "open_payment";
    public static final String EXTRA_FINISH_AFTER_PAYMENT = "finish_after_payment";
    public static final String EXTRA_READ_ONLY = "read_only";
    public static final String EXTRA_REQUEST_PAYMENT = "request_payment";

    private boolean openPaymentOnLoad = false;
    private boolean finishAfterPayment = false;
    private boolean isReadOnly = false;
    private boolean isRequestPayment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_ticket_detail);

        initViews();

        // Initialize MapView
        mapView = findViewById(R.id.mapView);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        setupClickListeners();

        tokenManager = new TokenManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        ticketId = getIntent().getStringExtra("ticket_id");
        openPaymentOnLoad = getIntent().getBooleanExtra(EXTRA_OPEN_PAYMENT, false);
        finishAfterPayment = getIntent().getBooleanExtra(EXTRA_FINISH_AFTER_PAYMENT, false);
        isReadOnly = getIntent().getBooleanExtra(EXTRA_READ_ONLY, false);
        isRequestPayment = getIntent().getBooleanExtra(EXTRA_REQUEST_PAYMENT, false);
        if (isReadOnly) {
            openPaymentOnLoad = false;
            finishAfterPayment = false;
            isRequestPayment = false;
        }

        if (ticketId != null) {
            loadTicketDetails();
        } else {
            Toast.makeText(this, "Invalid ticket ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvTicketId = findViewById(R.id.tvTicketId);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvServiceType = findViewById(R.id.tvServiceType);
        tvUnitType = findViewById(R.id.tvUnitType);
        tvAddress = findViewById(R.id.tvAddress);
        tvContact = findViewById(R.id.tvContact);
        tvStatus = findViewById(R.id.tvStatus);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvScheduleDate = findViewById(R.id.tvScheduleDate);
        tvScheduleTime = findViewById(R.id.tvScheduleTime);
        tvScheduleNotes = findViewById(R.id.tvScheduleNotes);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
        tvPaymentCollectedBy = findViewById(R.id.tvPaymentCollectedBy);
        paymentCard = findViewById(R.id.paymentCard);
        contentContainer = findViewById(R.id.contentContainer);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnBack = findViewById(R.id.btnBack);
        btnStartWork = findViewById(R.id.btnStartWork);
        btnCompleteWork = findViewById(R.id.btnCompleteWork);
        mapCardContainer = findViewById(R.id.mapCardContainer);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnViewMap.setOnClickListener(v -> openInMapsApp());

        if (isReadOnly) {
            if (btnStartWork != null) {
                btnStartWork.setVisibility(View.GONE);
            }
            if (btnCompleteWork != null) {
                btnCompleteWork.setVisibility(View.GONE);
            }
            return;
        }

        btnStartWork.setOnClickListener(v -> updateTicketStatus("ongoing"));
        if (btnCompleteWork != null) {
            btnCompleteWork.setVisibility(View.GONE);
        }
    }

    private void loadTicketDetails() {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<TicketDetailResponse> call = apiService.getTicketDetail("Bearer " + token, ticketId);

        call.enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TicketDetailResponse ticketResponse = response.body();
                    if (ticketResponse.isSuccess()) {
                        currentTicket = ticketResponse.getTicket();
                        displayTicketDetails(currentTicket);
                    } else {
                        Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to load ticket details",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to load ticket details",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                Toast.makeText(EmployeeTicketDetailActivity.this, "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayTicketDetails(TicketDetailResponse.TicketDetail ticket) {
        if (contentContainer != null) {
            contentContainer.setVisibility(View.VISIBLE);
        }
        tvTicketId.setText(ticket.getTicketId());
        tvTitle.setText(ticket.getTicketId());
        
        // Display unit type from separate field
        String unitType = ticket.getUnitType();
        if (unitType != null && !unitType.trim().isEmpty()) {
            tvUnitType.setText(unitType);
        } else {
            tvUnitType.setText("N/A");
        }
        
        // Display other details (description) separately
        tvDescription.setText(cleanInfoText(ticket.getDescription()));
        tvServiceType.setText(ticket.getServiceType());
        tvAddress.setText(ticket.getAddress());
        tvContact.setText(ticket.getContact());
        updateStatusBadge(tvStatus, ticket.getStatus(), ticket.getStatusColor());
        tvCustomerName
                .setText("Customer: " + (ticket.getCustomerName() != null ? ticket.getCustomerName() : "Unknown"));
        tvCreatedAt.setText(formatDateOnly(ticket.getCreatedAt()));

        // Display schedule information
        tvScheduleDate.setVisibility(View.GONE);

        String scheduleTime = ticket.getScheduledTime();
        if (scheduleTime == null || scheduleTime.trim().isEmpty()) {
            scheduleTime = extractTimeFromDateTime(ticket.getCreatedAt());
        }
        if (scheduleTime != null && !scheduleTime.trim().isEmpty()) {
            tvScheduleTime.setText(formatTime(scheduleTime));
            tvScheduleTime.setVisibility(View.VISIBLE);
        } else {
            tvScheduleTime.setVisibility(View.GONE);
        }

        if (ticket.getScheduleNotes() != null && !ticket.getScheduleNotes().isEmpty()) {
            tvScheduleNotes.setText("Notes: " + ticket.getScheduleNotes());
            tvScheduleNotes.setVisibility(View.VISIBLE);
        } else {
            tvScheduleNotes.setVisibility(View.GONE);
        }

        // Status badge already applied above.

        // Store customer coordinates for map viewing
        customerLatitude = ticket.getLatitude();
        customerLongitude = ticket.getLongitude();

        // Update map if ready and coordinates are valid
        updateMapLocation();

        // Show/hide action buttons based on ticket status
        updateActionButtons(ticket.getStatus());

        updatePaymentSection(ticket.getStatus());

        if (openPaymentOnLoad) {
            openPaymentOnLoad = false;
            showPaymentFragment();
        }
    }

    private void updatePaymentSection(String status) {
        if (paymentCard == null) return;

        if (tvPaymentStatus != null) tvPaymentStatus.setVisibility(View.GONE);
        if (tvPaymentMethod != null) tvPaymentMethod.setVisibility(View.GONE);
        if (tvPaymentAmount != null) tvPaymentAmount.setVisibility(View.GONE);
        if (tvPaymentCollectedBy != null) tvPaymentCollectedBy.setVisibility(View.GONE);

        if (status == null) {
            paymentCard.setVisibility(View.GONE);
            return;
        }

        String normalized = status.trim().toLowerCase(java.util.Locale.ENGLISH);
        boolean showPayment = normalized.contains("completed")
                || normalized.contains("resolved")
                || normalized.contains("closed");

        if (!showPayment) {
            paymentCard.setVisibility(View.GONE);
            return;
        }

        paymentCard.setVisibility(View.VISIBLE);
        loadPaymentDetails();
    }

    private void loadPaymentDetails() {
        String token = tokenManager.getToken();
        if (token == null || ticketId == null) {
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<app.hub.api.PaymentDetailResponse> call = apiService.getPaymentByTicketId("Bearer " + token, ticketId);
        call.enqueue(new Callback<app.hub.api.PaymentDetailResponse>() {
            @Override
            public void onResponse(Call<app.hub.api.PaymentDetailResponse> call,
                    Response<app.hub.api.PaymentDetailResponse> response) {
                if (!isFinishing() && response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getPayment() != null) {
                    app.hub.api.PaymentDetailResponse.PaymentDetail payment = response.body().getPayment();
                    bindPayment(payment);
                }
            }

            @Override
            public void onFailure(Call<app.hub.api.PaymentDetailResponse> call, Throwable t) {
                // Keep existing payment UI state if request fails.
            }
        });
    }

    private void bindPayment(app.hub.api.PaymentDetailResponse.PaymentDetail payment) {
        if (payment == null) return;
        String statusValue = payment.getStatus() != null ? payment.getStatus() : "";
        boolean isPaid = isPaidStatus(statusValue);
        String methodValue = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "";
        boolean isCash = methodValue.trim().equalsIgnoreCase("cash");
        boolean hasAmount = payment.getAmount() > 0;
        if (tvPaymentStatus != null) {
            String status = statusValue.isEmpty() ? "Pending" : statusValue;
            tvPaymentStatus.setText("Status: " + status);
            tvPaymentStatus.setVisibility(View.VISIBLE);
        }
        if (tvPaymentMethod != null) {
            String method = methodValue.isEmpty() ? "--" : methodValue;
            tvPaymentMethod.setText("Method: " + method);
            tvPaymentMethod.setVisibility(View.VISIBLE);
        }
        if (tvPaymentAmount != null) {
            if (isPaid) {
                tvPaymentAmount.setText("Amount Paid: \u20b1" + String.format(java.util.Locale.getDefault(), "%.2f",
                        payment.getAmount()));
                tvPaymentAmount.setVisibility(View.VISIBLE);
            } else {
                tvPaymentAmount.setVisibility(View.GONE);
            }
        }
        if (tvPaymentCollectedBy != null) {
            if (isPaid && isCash && hasAmount) {
                String collectedBy = currentTicket != null ? currentTicket.getAssignedStaff() : null;
                String displayName = (collectedBy == null || collectedBy.trim().isEmpty()) ? "--" : collectedBy;
                tvPaymentCollectedBy.setText("Collected by: " + displayName);
                tvPaymentCollectedBy.setVisibility(View.VISIBLE);
            } else {
                tvPaymentCollectedBy.setVisibility(View.GONE);
            }
        }
    }

    private boolean isPaidStatus(String status) {
        if (status == null) return false;
        String normalized = status.trim().toLowerCase(java.util.Locale.ENGLISH);
        return normalized.contains("completed")
                || normalized.contains("resolved")
                || normalized.contains("closed");
    }

    private void updateActionButtons(String status) {
        if (isReadOnly) {
            if (btnStartWork != null) {
                btnStartWork.setVisibility(View.GONE);
            }
            if (btnCompleteWork != null) {
                btnCompleteWork.setVisibility(View.GONE);
            }
            return;
        }
        if (status == null)
            return;

        switch (status.toLowerCase()) {
            case "accepted":
            case "assigned":
            case "scheduled":
                btnStartWork.setVisibility(View.VISIBLE);
                if (btnCompleteWork != null) btnCompleteWork.setVisibility(View.GONE);
                break;
            case "in progress":
            case "ongoing":
                btnStartWork.setVisibility(View.GONE);
                if (btnCompleteWork != null) btnCompleteWork.setVisibility(View.GONE);
                break;
            case "completed":
            case "cancelled":
                btnStartWork.setVisibility(View.GONE);
                if (btnCompleteWork != null) btnCompleteWork.setVisibility(View.GONE);
                break;
            default:
                btnStartWork.setVisibility(View.GONE);
                if (btnCompleteWork != null) btnCompleteWork.setVisibility(View.GONE);
                break;
        }
    }

    private void openInMapsApp() {
        if (customerLatitude == 0 || customerLongitude == 0) {
            Toast.makeText(this, "Customer location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String address = tvAddress != null ? tvAddress.getText().toString() : "";
        String label = address != null ? address : "Service Location";
        String uri = "geo:" + customerLatitude + "," + customerLongitude
                + "?q=" + customerLatitude + "," + customerLongitude + "(" + Uri.encode(label) + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        }
    }

    private void updateTicketStatus(String status) {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest(status);
        ApiService apiService = ApiClient.getApiService();
        Call<UpdateTicketStatusResponse> call = apiService.updateTicketStatus("Bearer " + token, ticketId, request);

        call.enqueue(new Callback<UpdateTicketStatusResponse>() {
            @Override
            public void onResponse(Call<UpdateTicketStatusResponse> call,
                    Response<UpdateTicketStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UpdateTicketStatusResponse statusResponse = response.body();
                    if (statusResponse.isSuccess()) {
                        String message = status.equals("ongoing") ? "Work started successfully"
                                : "Work completed successfully";
                        Toast.makeText(EmployeeTicketDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                        loadTicketDetails(); // Refresh ticket details
                    } else {
                        Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to update ticket status",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to update ticket status",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateTicketStatusResponse> call, Throwable t) {
                Toast.makeText(EmployeeTicketDetailActivity.this, "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }

        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd",
                    java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy",
                    java.util.Locale.getDefault());

            java.util.Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (java.text.ParseException e) {
            return dateString;
        }

        return dateString;
    }

    private String cleanInfoText(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        String prefix = "Landmark/Additional Info:";
        if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            trimmed = trimmed.substring(prefix.length()).trim();
        }
        return trimmed;
    }

    private String formatDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return "";
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd"
        };
        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy h:mm a",
                java.util.Locale.getDefault());

        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat(pattern,
                        java.util.Locale.getDefault());
                inputFormat.setLenient(true);
                java.util.Date date = inputFormat.parse(dateTimeString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (java.text.ParseException ignored) {
            }
        }

        return dateTimeString;
    }

    private String formatDateOnly(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return "";
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd"
        };
        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy",
                java.util.Locale.getDefault());

        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat(pattern,
                        java.util.Locale.getDefault());
                inputFormat.setLenient(true);
                java.util.Date date = inputFormat.parse(dateTimeString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (java.text.ParseException ignored) {
            }
        }

        return dateTimeString;
    }

    private String formatTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return "";
        }

        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("h:mm a",
                java.util.Locale.getDefault());

            java.util.Date time = inputFormat.parse(timeString);
            if (time != null) {
            return outputFormat.format(time);
            }
        } catch (java.text.ParseException ignored) {
        }

        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("HH:mm",
                java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("h:mm a",
                    java.util.Locale.getDefault());

            java.util.Date time = inputFormat.parse(timeString);
            if (time != null) {
                return outputFormat.format(time);
            }
        } catch (java.text.ParseException e) {
            return timeString;
        }

        return timeString;
    }

    private String extractTimeFromDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };
        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.getDefault());

        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat(pattern,
                        java.util.Locale.getDefault());
                inputFormat.setLenient(true);
                java.util.Date date = inputFormat.parse(dateTimeString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (java.text.ParseException ignored) {
            }
        }

        return null;
    }

    private void updateStatusBadge(TextView textView, String status, String statusColor) {
        if (textView == null) return;

        String safeStatus = status != null ? status.trim() : "";
        textView.setText(safeStatus.isEmpty() ? "Unknown" : safeStatus);

        Integer color = null;
        if (statusColor != null && !statusColor.isEmpty()) {
            try {
                color = Color.parseColor(statusColor);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (color == null) {
            String normalized = safeStatus.toLowerCase();
            switch (normalized) {
                case "pending":
                    color = Color.parseColor("#FFA500");
                    break;
                case "scheduled":
                    color = Color.parseColor("#6366F1");
                    break;
                case "accepted":
                case "in progress":
                case "ongoing":
                    color = Color.parseColor("#2196F3");
                    break;
                case "completed":
                    color = Color.parseColor("#4CAF50");
                    break;
                case "cancelled":
                case "rejected":
                    color = Color.parseColor("#F44336");
                    break;
                default:
                    color = Color.parseColor("#757575");
                    break;
            }
        }

        textView.setTextColor(Color.WHITE);
        textView.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnViewMap.performClick();
            } else {
                Toast.makeText(this, "Location permission is required to view map", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPaymentFragment() {
        String customerName = currentTicket != null ? currentTicket.getCustomerName() : null;
        String serviceName = currentTicket != null ? currentTicket.getServiceType() : null;
        double amount = currentTicket != null ? currentTicket.getAmount() : 0.0;
        
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }
        
        EmployeePaymentFragment fragment = EmployeePaymentFragment.newInstance(
            ticketId,
            customerName,
            serviceName,
            amount,
            isRequestPayment);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onPaymentConfirmed(String paymentMethod, double amount, String notes) {
        if ("online".equals(paymentMethod) && isRequestPayment) {
            requestPayment(amount, notes);
            return;
        }
        completeWorkWithPayment(paymentMethod, amount, notes);
    }

    @Override
    public void onPaymentRequested(double amount, String notes) {
        requestPayment(amount, notes);
    }

    private void requestPayment(double amount, String notes) {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        int technicianId = tokenManager.getUserIdInt();
        if (technicianId <= 0) {
            Toast.makeText(this, "Unable to identify technician.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        app.hub.api.PaymentRequestBody request = new app.hub.api.PaymentRequestBody(ticketId, technicianId);
        Call<app.hub.api.PaymentRequestResponse> call = apiService.requestPayment("Bearer " + token, request);

        call.enqueue(new Callback<app.hub.api.PaymentRequestResponse>() {
            @Override
            public void onResponse(Call<app.hub.api.PaymentRequestResponse> call,
                                   Response<app.hub.api.PaymentRequestResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(EmployeeTicketDetailActivity.this,
                            "Payment request sent to customer.", Toast.LENGTH_LONG).show();
                    if (currentTicket != null) {
                        currentTicket.setStatus("Pending Payment");
                        currentTicket.setStatusDetail("Pending Payment");
                    }
                    if (finishAfterPayment) {
                        Intent result = new Intent();
                        result.putExtra("ticket_id", ticketId);
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        loadTicketDetails();
                    }
                    return;
                }

                String message = "Failed to request payment";
                if (response.body() != null && response.body().getMessage() != null) {
                    message = response.body().getMessage();
                }
                Toast.makeText(EmployeeTicketDetailActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<app.hub.api.PaymentRequestResponse> call, Throwable t) {
                Toast.makeText(EmployeeTicketDetailActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void completeWorkWithPayment(String paymentMethod, double amount, String notes) {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCompleteWork.setEnabled(false);
        btnCompleteWork.setText("Processing...");

        CompleteWorkRequest request = new CompleteWorkRequest(paymentMethod, amount, notes);
        ApiService apiService = ApiClient.getApiService();
        Call<CompleteWorkResponse> call = apiService.completeWorkWithPayment("Bearer " + token, ticketId, request);

        call.enqueue(new Callback<CompleteWorkResponse>() {
            @Override
            public void onResponse(Call<CompleteWorkResponse> call, Response<CompleteWorkResponse> response) {
                btnCompleteWork.setEnabled(true);
                btnCompleteWork.setText("Complete Work");

                if (response.isSuccessful() && response.body() != null) {
                    CompleteWorkResponse workResponse = response.body();
                    if (workResponse.isSuccess()) {
                        String message = "Work completed successfully!";
                        if ("cash".equals(paymentMethod)) {
                            message += "\nPayment collected: ₱" + String.format("%.2f", amount);
                        }
                        Toast.makeText(EmployeeTicketDetailActivity.this, message, Toast.LENGTH_LONG).show();
                        Runnable finishAction = () -> {
                            if (finishAfterPayment) {
                                Intent result = new Intent();
                                result.putExtra("ticket_id", ticketId);
                                setResult(RESULT_OK, result);
                                finish();
                            } else {
                                loadTicketDetails();
                            }
                        };

                        if ("cash".equals(paymentMethod)) {
                            int paymentId = workResponse.getPayment() != null
                                    ? workResponse.getPayment().getId()
                                    : 0;
                            if (paymentId > 0) {
                                submitPaymentToManager(paymentId, finishAction);
                            } else {
                                finishAction.run();
                            }
                        } else {
                            finishAction.run();
                        }
                    } else {
                        Toast.makeText(EmployeeTicketDetailActivity.this, "Failed: " + workResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to complete work", Toast.LENGTH_LONG)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<CompleteWorkResponse> call, Throwable t) {
                btnCompleteWork.setEnabled(true);
                btnCompleteWork.setText("Complete Work");
                Toast.makeText(EmployeeTicketDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void submitPaymentToManager(int paymentId, Runnable onComplete) {
        String token = tokenManager.getToken();
        if (token == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<CompleteWorkResponse> call = apiService.submitPaymentToManager("Bearer " + token, paymentId);
        call.enqueue(new Callback<CompleteWorkResponse>() {
            @Override
            public void onResponse(Call<CompleteWorkResponse> call, Response<CompleteWorkResponse> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(EmployeeTicketDetailActivity.this,
                            "Payment submitted, but manager sync failed.",
                            Toast.LENGTH_SHORT).show();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onFailure(Call<CompleteWorkResponse> call, Throwable t) {
                Toast.makeText(EmployeeTicketDetailActivity.this,
                        "Payment submitted, but manager sync failed.",
                        Toast.LENGTH_SHORT).show();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void updateMapLocation() {
        if (googleMap == null)
            return;

        if (customerLatitude != 0 && customerLongitude != 0) {
            // Use explicit coordinates
            showLocationOnMap(customerLatitude, customerLongitude);
        } else {
            // Fallback: Try to geocode the address
            String address = tvAddress.getText().toString();
            if (!address.isEmpty() && !address.equals("No Address")) {
                geocodeAndShowLocation(address);
            } else {
                hideMap();
            }
        }
    }

    private void showLocationOnMap(double lat, double lng) {
        if (googleMap != null) {
            LatLng location = new LatLng(lat, lng);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(location).title("Service Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            // Ensure map is visible
            btnViewMap.setVisibility(View.VISIBLE);
            if (mapCardContainer != null)
                mapCardContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideMap() {
        btnViewMap.setVisibility(View.GONE);
        if (mapCardContainer != null)
            mapCardContainer.setVisibility(View.GONE);
    }

    private void geocodeAndShowLocation(String addressStr) {
        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
                java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(addressStr, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address location = addresses.get(0);
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        // Update the stored coordinates
                        this.customerLatitude = lat;
                        this.customerLongitude = lng;
                        showLocationOnMap(lat, lng);
                    });
                } else {
                    runOnUiThread(this::hideMap);
                }
            } catch (java.io.IOException e) {
                runOnUiThread(this::hideMap);
            }
        }).start();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        updateMapLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null)
            mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null)
            mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null)
            mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null)
            mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null)
            mapView.onLowMemory();
    }
}
