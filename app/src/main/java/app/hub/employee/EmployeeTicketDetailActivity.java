package app.hub.employee;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import app.hub.R;
import app.hub.api.TicketDetailResponse;
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
        if (ticketId == null) return;

        android.util.Log.d("EmployeeTicketDetail", "Loading ticket details from Firestore: " + ticketId);

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentTicket = documentSnapshot.toObject(TicketDetailResponse.TicketDetail.class);
                    if (currentTicket != null) {
                        displayTicketDetails(currentTicket);
                    }
                } else {
                    Toast.makeText(EmployeeTicketDetailActivity.this, "Ticket not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("EmployeeTicketDetail", "Error loading ticket details: " + e.getMessage());
                Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to load ticket details", Toast.LENGTH_SHORT).show();
                finish();
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
        if (ticketId == null) return;

        android.util.Log.d("EmployeeTicketDetail", "Loading payment details from Firestore: " + ticketId);

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (isFinishing() || isDestroyed()) return;
                
                if (documentSnapshot.exists()) {
                    Double amountVal = documentSnapshot.getDouble("total_amount");
                    String statusVal = documentSnapshot.getString("payment_status");
                    String methodVal = documentSnapshot.getString("payment_method");
                    
                    if (amountVal != null && amountVal > 0) {
                        // Use a dummy object for bindPayment or update bindPayment to take raw values
                        app.hub.api.PaymentDetailResponse.PaymentDetail payment = new app.hub.api.PaymentDetailResponse.PaymentDetail();
                        payment.setAmount(amountVal);
                        payment.setStatus(statusVal);
                        payment.setPaymentMethod(methodVal);
                        bindPayment(payment);
                    }
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
        if (ticketId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updated_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                String message = status.equals("ongoing") ? "Work started successfully"
                        : "Work completed successfully";
                Toast.makeText(EmployeeTicketDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                loadTicketDetails();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (ticketId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("total_amount", amount);
        updates.put("notes", notes);
        updates.put("status", "Pending Payment");
        updates.put("payment_status", "pending");
        updates.put("updated_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(EmployeeTicketDetailActivity.this, "Payment request sent to customer.", Toast.LENGTH_LONG).show();
                if (finishAfterPayment) {
                    Intent result = new Intent();
                    result.putExtra("ticket_id", ticketId);
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    loadTicketDetails();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to request payment: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void completeWorkWithPayment(String paymentMethod, double amount, String notes) {
        if (ticketId == null) return;

        btnCompleteWork.setEnabled(false);
        btnCompleteWork.setText("Processing...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("payment_method", paymentMethod);
        updates.put("total_amount", amount);
        updates.put("notes", notes);
        updates.put("payment_status", "paid");
        updates.put("status", "completed");
        updates.put("completed_at", com.google.firebase.Timestamp.now());
        updates.put("updated_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                btnCompleteWork.setEnabled(true);
                btnCompleteWork.setText("Complete Work");
                Toast.makeText(EmployeeTicketDetailActivity.this, "Work completed successfully!", Toast.LENGTH_SHORT).show();
                loadTicketDetails();
            })
            .addOnFailureListener(e -> {
                btnCompleteWork.setEnabled(true);
                btnCompleteWork.setText("Complete Work");
                Toast.makeText(EmployeeTicketDetailActivity.this, "Failed to complete work: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
