package app.hub.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.SharedPreferences;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.hub.R;
import app.hub.api.TicketListResponse;
import app.hub.map.EmployeeMapActivity;
import app.hub.util.TokenManager;

public class EmployeeWorkFragment extends Fragment implements OnMapReadyCallback {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TokenManager tokenManager;
    private List<TicketListResponse.TicketItem> assignedTickets;

    private View activeContentContainer;
    private View mapContainer;
    private FrameLayout workStatusContainer;
    private FrameLayout stateOverlayContainer;
    private MapView mapViewActiveJob;
    private GoogleMap googleMap;
    private LatLng customerLatLng;
    private LatLng branchLatLng;
    private String cachedCustomerAddress;
    private String cachedBranchName;

    private TicketListResponse.TicketItem activeTicket;
    private ActivityResultLauncher<Intent> paymentLauncher;
    private boolean isLoadingTickets = false;

    private android.os.Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    
    // Firebase real-time listener
    private EmployeeFirebaseListener firebaseListener;

    private static final String PREFS_NAME = "employee_work_steps";
    private static final String PREFS_TIMES = "employee_work_times";
    private static final String PREFS_DISMISSED = "employee_work_dismissed";
    private static final String PREFS_READY_PAYMENT = "employee_work_ready_payment";
    private static final int STEP_ASSIGNED = 0;
    private static final int STEP_ON_THE_WAY = 1;
    private static final int STEP_ARRIVED = 2;
    private static final int STEP_IN_PROGRESS = 3;
    private static final int STEP_COMPLETED = 4;

    private static final String EXTRA_OPEN_PAYMENT = "open_payment";
    private static final String EXTRA_FINISH_AFTER_PAYMENT = "finish_after_payment";

    private static final Map<String, String> BRANCH_ADDRESS_MAP = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        paymentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK) {
                        return;
                    }
                    String ticketId = null;
                    if (result.getData() != null) {
                        ticketId = result.getData().getStringExtra("ticket_id");
                    }
                    if (ticketId == null && activeTicket != null) {
                        ticketId = activeTicket.getTicketId();
                    }
                    if (ticketId == null) {
                        return;
                    }
                    saveStepTime(ticketId, STEP_COMPLETED);
                    saveStep(ticketId, STEP_COMPLETED);
                    loadAssignedTickets();
                });
    }

    static {
        BRANCH_ADDRESS_MAP.put(
                "ASHCOL - CALAUAN LAGUNA",
                "Purok 4 Kalye Pogi, Brgy. Bangyas, Calauan, Laguna");
        BRANCH_ADDRESS_MAP.put(
                "ASHCOL - STA ROSA - TAGAYTAY RD BATANGAS",
                "2nd Flr Rheayanell Bldg., 9015 Pandan St., Sta. Rosa - Tagaytay Road, Brgy. Puting Kahoy, Silang, Cavite");
        BRANCH_ADDRESS_MAP.put(
                "ASHCOL - PAMPANGA",
                "202 CityCorp Business Center, San Isidro, City of San Fernando, Pampanga");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_employee_work, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        showInitialState();
        loadAssignedTickets(true);

        // Start auto-refresh in background
        startAutoRefresh();
        
        // Start Firebase real-time listener
        startFirebaseListener();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        activeContentContainer = view.findViewById(R.id.activeContentContainer);
        mapContainer = view.findViewById(R.id.mapContainer);
        workStatusContainer = view.findViewById(R.id.workStatusContainerInner);
        stateOverlayContainer = view.findViewById(R.id.stateOverlayContainer);
        mapViewActiveJob = view.findViewById(R.id.mapViewActiveJob);

        tokenManager = new TokenManager(getContext());
        assignedTickets = new ArrayList<>();

        setupMapView();
        setupSwipeRefresh();
    }

    private void setupSwipeRefresh() {
        // Check if fragment is still attached and context is valid
        if (!isAdded() || getContext() == null) {
            android.util.Log.w("EmployeeWork", "Fragment detached or context null, skipping swipe refresh setup");
            return;
        }

        if (swipeRefreshLayout != null) {
            // Set refresh colors
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.green,
                    R.color.blue,
                    R.color.orange);

            // Set refresh listener
            swipeRefreshLayout.setOnRefreshListener(() -> {
                android.util.Log.d("EmployeeWork", "Pull-to-refresh triggered - forcing refresh");
                if (activeTicket != null && resolveStep(activeTicket) == STEP_COMPLETED) {
                    dismissCompletedTicket(activeTicket.getTicketId());
                }
                loadAssignedTickets(true);
            });

            android.util.Log.d("EmployeeWork", "SwipeRefreshLayout configured");
        }
    }

    private void loadAssignedTickets() {
        loadAssignedTickets(false);
    }

    private void loadAssignedTickets(boolean preserveUi) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !isAdded()) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        isLoadingTickets = true;
        if (!preserveUi) {
            setMapVisible(false);
            if (workStatusContainer != null) {
                workStatusContainer.removeAllViews();
            }
        }

        android.util.Log.d("EmployeeWork", "Loading tickets from Firestore...");

        FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("assigned_staff_id", user.getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                isLoadingTickets = false;
                if (!isAdded()) return;
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                assignedTickets.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    TicketListResponse.TicketItem ticket = doc.toObject(TicketListResponse.TicketItem.class);
                    if (ticket != null) {
                        ticket.setTicketId(doc.getId());
                        assignedTickets.add(ticket);
                    }
                }
                android.util.Log.d("EmployeeWork", "Loaded " + assignedTickets.size() + " tickets");
                updateActiveJobUi();
            })
            .addOnFailureListener(e -> {
                isLoadingTickets = false;
                if (!isAdded()) return;
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                android.util.Log.e("EmployeeWork", "Error loading tickets: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to load tickets", Toast.LENGTH_SHORT).show();
            });
    }

    private void showInitialState() {
        if (!isAdded()) {
            return;
        }
        setMapVisible(false);
        if (workStatusContainer != null) {
            workStatusContainer.removeAllViews();
        }
        showOverlay(R.layout.fragment_employee_work_nojob);
    }

    private void setupMapView() {
        if (mapViewActiveJob == null) {
            return;
        }
        mapViewActiveJob.onCreate(null);
        mapViewActiveJob.getMapAsync(this);
    }

    private void updateActiveJobUi() {
        activeTicket = findActiveTicket(assignedTickets);
        if (activeTicket == null) {
            setMapVisible(false);
            if (workStatusContainer != null) {
                workStatusContainer.removeAllViews();
            }
            showOverlay(R.layout.fragment_employee_work_nojob);
            return;
        }

        if (getStepTime(activeTicket.getTicketId(), STEP_ASSIGNED).isEmpty()) {
            saveStepTime(activeTicket.getTicketId(), STEP_ASSIGNED);
        }

        int step = resolveStep(activeTicket);
        if (step == STEP_COMPLETED) {
            View overlay = showOverlay(R.layout.item_employee_work_workdone);
            bindTicketDetails(overlay, activeTicket);
            bindWorkCompletedOverlay(overlay, activeTicket);
            setMapVisible(false);
            return;
        }

        hideOverlay();
        View statusView = showStatusLayout(getLayoutForStep(step));
        bindTicketDetails(statusView, activeTicket);
        bindStepTimes(statusView, activeTicket);
        bindStatusActions(statusView, step);
        applyStepStyling(statusView, step);
        applyCompletedUi(statusView, activeTicket);
        setMapVisible(false);
        updateMapMarker(activeTicket.getLatitude(), activeTicket.getLongitude());
    }

    private int getLayoutForStep(int step) {
        switch (step) {
            case STEP_ON_THE_WAY:
                return R.layout.item_employee_work_jobotw;
            case STEP_ARRIVED:
                return R.layout.item_employee_work_jobarrive;
            case STEP_IN_PROGRESS:
                return R.layout.item_employee_work_jobprogress;
            case STEP_ASSIGNED:
            default:
                return R.layout.fragment_employee_work_jobassign;
        }
    }

    private View showStatusLayout(int layoutResId) {
        if (workStatusContainer == null || getContext() == null) {
            return null;
        }
        workStatusContainer.removeAllViews();
        View statusView = LayoutInflater.from(getContext()).inflate(layoutResId, workStatusContainer, false);
        if (statusView instanceof NestedScrollView) {
            ((NestedScrollView) statusView).setNestedScrollingEnabled(false);
        }
        workStatusContainer.addView(statusView);
        return statusView;
    }

    private View showOverlay(int layoutResId) {
        if (stateOverlayContainer == null || getContext() == null) {
            return null;
        }
        stateOverlayContainer.removeAllViews();
        View overlay = LayoutInflater.from(getContext()).inflate(layoutResId, stateOverlayContainer, false);
        stateOverlayContainer.addView(overlay);
        stateOverlayContainer.setVisibility(View.VISIBLE);
        if (activeContentContainer != null) {
            activeContentContainer.setVisibility(View.GONE);
        }
        return overlay;
    }

    private void hideOverlay() {
        if (stateOverlayContainer != null) {
            stateOverlayContainer.setVisibility(View.GONE);
        }
        if (activeContentContainer != null) {
            activeContentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void bindStatusActions(View statusView, int step) {
        if (statusView == null) {
            return;
        }

        if (step == STEP_ASSIGNED) {
            View onTheWay = statusView.findViewById(R.id.tvOnWayStatus);
            setStepAction(onTheWay, () -> updateTicketStatus("ongoing", "otw",
                    () -> advanceStep(STEP_ON_THE_WAY)));
            View onTheWayCheck = statusView.findViewById(R.id.ivStep2);
            setStepAction(onTheWayCheck, () -> updateTicketStatus("ongoing", "otw",
                    () -> advanceStep(STEP_ON_THE_WAY)));
        } else if (step == STEP_ON_THE_WAY) {
            View arrived = statusView.findViewById(R.id.tvArrivedStatus);
            setStepAction(arrived, () -> updateTicketStatus("ongoing", "arrived",
                    () -> advanceStep(STEP_ARRIVED)));
            View arrivedCheck = statusView.findViewById(R.id.ivStep3);
            setStepAction(arrivedCheck, () -> updateTicketStatus("ongoing", "arrived",
                    () -> advanceStep(STEP_ARRIVED)));
        } else if (step == STEP_ARRIVED) {
            View startService = statusView.findViewById(R.id.btnStartService);
            if (startService != null) {
                startService.setOnClickListener(v -> {
                    if (activeTicket == null) {
                        return;
                    }
                    updateTicketStatus("ongoing", "working", () -> advanceStep(STEP_IN_PROGRESS));
                });
            }
        } else if (step == STEP_IN_PROGRESS) {
            View markComplete = statusView.findViewById(R.id.btnMarkCompleted);
            if (markComplete != null) {
                markComplete.setOnClickListener(v -> markCompletedLocal());
            }
            View requestPayment = statusView.findViewById(R.id.btnRequestPayment);
            if (requestPayment != null) {
                requestPayment.setOnClickListener(v -> requestOnlinePaymentFromWorkTab());
            }
        }
    }

    private void bindWorkCompletedOverlay(View overlay, TicketListResponse.TicketItem ticket) {
        if (overlay == null || ticket == null) {
            return;
        }

        View backHome = overlay.findViewById(R.id.btnViewHistory);
        if (backHome != null) {
            backHome.setOnClickListener(v -> {
                dismissCompletedTicket(ticket.getTicketId());
                loadAssignedTickets(true);
                navigateToHome();
            });
        }

        loadPaidAmount(overlay, ticket.getTicketId());
    }

    private void navigateToHome() {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        if (getActivity() instanceof EmployeeDashboardActivity) {
            ((EmployeeDashboardActivity) getActivity()).updateNavigationIndicator(R.id.nav_home);
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new EmployeeDashboardFragment())
                .commit();
    }

    private void loadPaidAmount(View overlay, String ticketId) {
        TextView amountView = overlay.findViewById(R.id.tvAmountPaid);
        if (amountView != null) {
            amountView.setText("Php --");
        }

        if (ticketId == null) return;

        android.util.Log.d("EmployeeWork", "Loading paid amount from Firestore: " + ticketId);

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded()) return;
                
                if (documentSnapshot.exists()) {
                    Double amount = documentSnapshot.getDouble("total_amount");
                    if (amount != null && amount > 0 && amountView != null) {
                        String amountText = "Php " + String.format(Locale.getDefault(), "%,.2f", amount);
                        amountView.setText(amountText);
                    }
                }
            });
    }

    private void setStepAction(View view, Runnable action) {
        if (view == null || action == null) {
            return;
        }
        view.setEnabled(true);
        view.setClickable(true);
        view.setOnClickListener(v -> action.run());
    }

    private void openPaymentFlow(boolean requestOnly) {
        if (activeTicket == null || getContext() == null) {
            return;
        }
        Intent intent = new Intent(getContext(), EmployeeTicketDetailActivity.class);
        intent.putExtra("ticket_id", activeTicket.getTicketId());
        intent.putExtra(EXTRA_OPEN_PAYMENT, true);
        intent.putExtra(EXTRA_FINISH_AFTER_PAYMENT, true);
        intent.putExtra(EmployeeTicketDetailActivity.EXTRA_REQUEST_PAYMENT, requestOnly);
        if (paymentLauncher != null) {
            paymentLauncher.launch(intent);
        } else {
            startActivity(intent);
        }
    }

    private void requestOnlinePaymentFromWorkTab() {
        if (activeTicket == null) return;

        double amount = activeTicket.getAmount();
        if (amount <= 0) {
            fetchTicketAmountAndRequestPayment(activeTicket.getTicketId());
            return;
        }
        sendPaymentRequest(activeTicket.getTicketId(), amount);
    }

    private void fetchTicketAmountAndRequestPayment(String ticketId) {
        if (ticketId == null) return;

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded()) return;
                
                if (documentSnapshot.exists()) {
                    Double amount = documentSnapshot.getDouble("total_amount");
                    if (amount != null && amount > 0) {
                        if (activeTicket != null && ticketId.equals(activeTicket.getTicketId())) {
                            activeTicket.setAmount(amount);
                        }
                        sendPaymentRequest(ticketId, amount);
                    } else {
                        Toast.makeText(getContext(), "Missing amount. Please set the ticket amount first.", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load ticket amount", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void sendPaymentRequest(String ticketId, double amount) {
        if (ticketId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("total_amount", amount);
        updates.put("status", "Pending Payment");
        updates.put("payment_status", "pending");
        updates.put("updated_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                if (!isAdded()) return;
                
                Toast.makeText(getContext(), "Payment request sent to customer.", Toast.LENGTH_SHORT).show();
                
                if (activeTicket != null && ticketId.equals(activeTicket.getTicketId())) {
                    activeTicket.setStatus("Pending Payment");
                    activeTicket.setStatusDetail("Pending Payment");
                    clearReadyForPayment(ticketId);
                }
                
                showPaymentConfirmationScreen();
                loadAssignedTickets(true);
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to send payment request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showPaymentConfirmationScreen() {
        if (activeTicket == null || !isAdded()) {
            return;
        }

        String customerName = activeTicket.getCustomerName() != null ? 
                activeTicket.getCustomerName() : "Customer";
        String serviceName = activeTicket.getServiceType() != null ? 
                activeTicket.getServiceType() : "Service";
        double amount = activeTicket.getAmount();

        EmployeeWorkConfirmPaymentFragment confirmFragment = 
                EmployeeWorkConfirmPaymentFragment.newInstance(
                        activeTicket.getTicketId(),
                        customerName,
                        serviceName,
                        amount
                );

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, confirmFragment)
                .addToBackStack(null)
                .commit();
    }

    private void bindTicketDetails(View root, TicketListResponse.TicketItem ticket) {
        if (root == null || ticket == null) {
            return;
        }

        setLabeledText(root, R.id.tvTicketId, ticket.getTicketId());
        setLabeledText(root, R.id.tvCustomerName, ticket.getCustomerName());
        setLabeledText(root, R.id.tvCustomerPhone, ticket.getContact());
        setLabeledText(root, R.id.tvPhone, ticket.getContact());
        setLabeledText(root, R.id.tvServiceType, getServiceText(ticket));
        setLabeledText(root, R.id.tvServiceName, getServiceText(ticket));
        setLabeledText(root, R.id.tvAddress, ticket.getAddress());

        String schedule = buildScheduleText(ticket.getScheduledDate(), ticket.getScheduledTime());
        setLabeledText(root, R.id.tvSchedule, schedule);
        setLabeledText(root, R.id.tvNote, ticket.getScheduleNotes());
    }

    private String getServiceText(TicketListResponse.TicketItem ticket) {
        String service = ticket.getServiceType();
        if (service == null || service.trim().isEmpty()) {
            service = ticket.getDescription();
        }
        return service;
    }

    private void setLabeledText(View root, int viewId, String value) {
        TextView view = root.findViewById(viewId);
        if (view == null) {
            return;
        }
        if (value == null || value.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        String existing = view.getText() != null ? view.getText().toString() : "";
        int colonIndex = existing.indexOf(":");
        if (colonIndex >= 0) {
            String prefix = existing.substring(0, colonIndex + 1);
            view.setText(prefix + " " + value);
        } else {
            view.setText(value);
        }
        view.setVisibility(View.VISIBLE);
    }

    private TicketListResponse.TicketItem findActiveTicket(List<TicketListResponse.TicketItem> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return null;
        }

        java.util.Set<String> dismissed = getDismissedCompletedTickets();

        TicketListResponse.TicketItem best = null;
        int bestPriority = Integer.MAX_VALUE;
        long bestTime = Long.MAX_VALUE;

        for (TicketListResponse.TicketItem ticket : tickets) {
            if (ticket == null || ticket.getStatus() == null) {
                continue;
            }

            String status = ticket.getStatus().trim().toLowerCase();
            long ticketTime = getTicketSortTime(ticket);

            // Clean up dismissed list if ticket is no longer completed
            if (!isTicketCompletedStatus(status) && !isTicketLocallyCompleted(ticket)) {
                if (ticket.getTicketId() != null && dismissed.contains(ticket.getTicketId())) {
                    dismissed.remove(ticket.getTicketId());
                    saveDismissedCompletedTickets(dismissed);
                }
            }

            // Skip completed tickets entirely - they should not show in work view
            if (isTicketCompletedStatus(status) || isTicketLocallyCompleted(ticket)) {
                continue;
            }

            // Skip dismissed tickets
            if (ticket.getTicketId() != null && dismissed.contains(ticket.getTicketId())) {
                continue;
            }

            int priority = getTicketPriority(status);
            if (priority < bestPriority || (priority == bestPriority && ticketTime < bestTime)) {
                best = ticket;
                bestPriority = priority;
                bestTime = ticketTime;
            }
        }

        return best;
    }

    private java.util.Set<String> getDismissedCompletedTickets() {
        if (!isAdded()) {
            return new java.util.HashSet<>();
        }
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_DISMISSED,
                android.content.Context.MODE_PRIVATE);
        java.util.Set<String> stored = prefs.getStringSet("dismissed", new java.util.HashSet<>());
        return stored == null ? new java.util.HashSet<>() : new java.util.HashSet<>(stored);
    }

    private void saveDismissedCompletedTickets(java.util.Set<String> dismissed) {
        if (!isAdded()) {
            return;
        }
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_DISMISSED,
                android.content.Context.MODE_PRIVATE);
        prefs.edit().putStringSet("dismissed", dismissed).apply();
    }

    private void dismissCompletedTicket(String ticketId) {
        if (ticketId == null || !isAdded()) {
            return;
        }
        java.util.Set<String> dismissed = getDismissedCompletedTickets();
        dismissed.add(ticketId);
        saveDismissedCompletedTickets(dismissed);
    }

    private boolean isTicketLocallyCompleted(TicketListResponse.TicketItem ticket) {
        if (ticket == null || ticket.getTicketId() == null) {
            return false;
        }
        String completedTime = getStepTime(ticket.getTicketId(), STEP_COMPLETED);
        return completedTime != null && !completedTime.trim().isEmpty();
    }

    private boolean isTicketCompletedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toLowerCase().trim();

        // CRITICAL: "Pending Payment" should NOT be considered completed
        // The ticket must remain visible until payment is received
        if (normalized.contains("pending")) {
            return false;
        }

        // Check for actual completed statuses
        return normalized.equals("completed")
                || normalized.equals("resolved")
                || normalized.equals("closed")
                || normalized.contains("completed")
                || normalized.contains("resolved")
                || normalized.contains("closed");
    }

    private int getTicketPriority(String status) {
        if (status == null) {
            return 3;
        }
        if (status.contains("ongoing") || status.contains("in progress") || status.contains("progress")
                || status.contains("accepted")) {
            return 0;
        }
        if (status.contains("scheduled") || status.contains("pending") || status.contains("open")) {
            return 1;
        }
        return 2;
    }

    private long getTicketSortTime(TicketListResponse.TicketItem ticket) {
        if (ticket == null) {
            return Long.MAX_VALUE;
        }
        String createdAt = ticket.getCreatedAt();
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return Long.MAX_VALUE;
        }
        String value = createdAt.trim();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = format.parse(value);
            return date != null ? date.getTime() : Long.MAX_VALUE;
        } catch (Exception e) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = format.parse(value);
                return date != null ? date.getTime() : Long.MAX_VALUE;
            } catch (Exception ignored) {
                return Long.MAX_VALUE;
            }
        }
    }

    private int resolveStep(TicketListResponse.TicketItem ticket) {
        if (ticket == null || ticket.getTicketId() == null) {
            return STEP_ASSIGNED;
        }

        String status = ticket.getStatus() != null ? ticket.getStatus().trim().toLowerCase() : "";
        int resolved = STEP_ASSIGNED;

        if (status.contains("completed")) {
            resolved = STEP_COMPLETED;
        }

        int saved = getSavedStep(ticket.getTicketId());
        int step = Math.max(saved, resolved);
        if (step != saved) {
            saveStep(ticket.getTicketId(), step);
        }
        return step;
    }

    private void advanceStep(int nextStep) {
        if (activeTicket == null || activeTicket.getTicketId() == null) {
            return;
        }
        saveStepTime(activeTicket.getTicketId(), nextStep);
        saveStep(activeTicket.getTicketId(), nextStep);
        updateActiveJobUi();
    }

    private int getSavedStep(String ticketId) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME,
                android.content.Context.MODE_PRIVATE);
        return prefs.getInt(ticketId, STEP_ASSIGNED);
    }

    private void saveStep(String ticketId, int step) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME,
                android.content.Context.MODE_PRIVATE);
        prefs.edit().putInt(ticketId, step).apply();
    }

    private void saveStepTime(String ticketId, int step) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_TIMES,
                android.content.Context.MODE_PRIVATE);
        String key = ticketId + "_step_time_" + step;
        String now = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        prefs.edit().putString(key, now).apply();
    }

    private String getStepTime(String ticketId, int step) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_TIMES,
                android.content.Context.MODE_PRIVATE);
        String key = ticketId + "_step_time_" + step;
        return prefs.getString(key, "");
    }

    private void bindStepTimes(View root, TicketListResponse.TicketItem ticket) {
        if (root == null || ticket == null || ticket.getTicketId() == null) {
            return;
        }
        String ticketId = ticket.getTicketId();
        setStepTime(root, R.id.tvAssignedTime, getStepTime(ticketId, STEP_ASSIGNED));
        setStepTime(root, R.id.tvOnWayTime, getStepTime(ticketId, STEP_ON_THE_WAY));
        setStepTime(root, R.id.tvArrivedTime, getStepTime(ticketId, STEP_ARRIVED));
        setStepTime(root, R.id.tvInProgressTime, getStepTime(ticketId, STEP_IN_PROGRESS));
        setStepTime(root, R.id.tvCompletedTime, getStepTime(ticketId, STEP_COMPLETED));
    }

    private void markCompletedLocal() {
        if (activeTicket == null || activeTicket.getTicketId() == null) {
            return;
        }
        String ticketId = activeTicket.getTicketId();
        
        // Update status_detail to "work_completed" (keep status as "ongoing", not "completed" yet)
        // Ticket will only be marked "completed" after payment is confirmed
        updateTicketStatus("ongoing", "work_completed", () -> {
            // After backend update succeeds, mark as ready for payment locally
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_READY_PAYMENT,
                    android.content.Context.MODE_PRIVATE);
            prefs.edit().putBoolean(ticketId, true).apply();
            android.util.Log.d("EmployeeWork", "Work completed, ready for payment: " + ticketId);
            
            // DO NOT save completed step time yet - only save when payment is actually done
            // saveStepTime(ticketId, STEP_COMPLETED);
            // saveStep(ticketId, STEP_COMPLETED);
            
            updateActiveJobUi();
        });
    }

    private boolean isReadyForPayment(String ticketId) {
        if (ticketId == null)
            return false;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_READY_PAYMENT,
                android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean(ticketId, false);
    }

    private void clearReadyForPayment(String ticketId) {
        if (ticketId == null)
            return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_READY_PAYMENT,
                android.content.Context.MODE_PRIVATE);
        prefs.edit().remove(ticketId).apply();
    }

    /**
     * Apply grey styling to tracking steps until service actually starts (STEP_IN_PROGRESS)
     * Steps should remain grey until technician clicks "Start Service"
     */
    private void applyStepStyling(View root, int currentStep) {
        if (root == null) {
            return;
        }

        // If service hasn't started yet (before STEP_IN_PROGRESS), keep all future steps grey
        boolean serviceStarted = currentStep >= STEP_IN_PROGRESS;

        // Step 2: On the Way - grey out if service not started
        if (!serviceStarted && currentStep < STEP_ON_THE_WAY) {
            ImageView step2Icon = root.findViewById(R.id.ivStep2);
            View step2Status = root.findViewById(R.id.tvOnWayStatus);
            if (step2Icon != null) {
                step2Icon.setBackgroundResource(R.drawable.shape_circle_gray);
                step2Icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
            if (step2Status != null && step2Status instanceof TextView) {
                ((TextView) step2Status).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            }
        }

        // Step 3: Arrived - grey out if service not started
        if (!serviceStarted && currentStep < STEP_ARRIVED) {
            ImageView step3Icon = root.findViewById(R.id.ivStep3);
            View step3Status = root.findViewById(R.id.tvArrivedStatus);
            View step3StatusLabel = root.findViewById(R.id.tvArrivedStatusLabel);
            if (step3Icon != null) {
                step3Icon.setBackgroundResource(R.drawable.shape_circle_gray);
                step3Icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
            if (step3Status != null && step3Status instanceof TextView) {
                ((TextView) step3Status).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            }
            if (step3StatusLabel != null && step3StatusLabel instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) step3StatusLabel;
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                card.setStrokeColor(0xFFBDBDBD);
                TextView arrivedLabel = root.findViewById(R.id.tvArrivedLabel);
                if (arrivedLabel != null) {
                    arrivedLabel.setTextColor(0xFFBDBDBD);
                }
            }
        }

        // Step 4: In Progress - grey out if service not started
        if (!serviceStarted && currentStep < STEP_IN_PROGRESS) {
            ImageView step4Icon = root.findViewById(R.id.ivStep4);
            View step4Status = root.findViewById(R.id.tvInProgressStatus);
            if (step4Icon != null) {
                step4Icon.setBackgroundResource(R.drawable.shape_circle_gray);
                step4Icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
            if (step4Status != null && step4Status instanceof TextView) {
                ((TextView) step4Status).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            }
        }

        // Step 5: Completed - always grey until actually completed
        if (currentStep < STEP_COMPLETED) {
            ImageView step5Icon = root.findViewById(R.id.ivStep5);
            View step5Status = root.findViewById(R.id.tvCompletedStatus);
            TextView step5Label = root.findViewById(R.id.tvCompletedLabel);
            if (step5Icon != null) {
                step5Icon.setBackgroundResource(R.drawable.shape_circle_gray);
                step5Icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
            if (step5Status != null && step5Status instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) step5Status;
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                card.setStrokeColor(0xFFBDBDBD);
            }
            if (step5Label != null) {
                step5Label.setTextColor(0xFFBDBDBD);
            }
        }
    }

    private void applyCompletedUi(View root, TicketListResponse.TicketItem ticket) {
        if (root == null || ticket == null || ticket.getTicketId() == null) {
            return;
        }

        View markButton = root.findViewById(R.id.btnMarkCompleted);
        View requestButton = root.findViewById(R.id.btnRequestPayment);
        ImageView stepView = root.findViewById(R.id.ivStep5);
        View completedStatusView = root.findViewById(R.id.tvCompletedStatus);
        TextView completedLabel = root.findViewById(R.id.tvCompletedLabel);

        // Check if work is marked as ready for payment (not actually completed yet)
        boolean isReadyForPayment = isReadyForPayment(ticket.getTicketId());

        if (markButton != null) {
            // Hide "Mark as Completed" button once clicked
            markButton.setVisibility(isReadyForPayment ? View.GONE : View.VISIBLE);
            markButton.setEnabled(!isReadyForPayment);
        }
        if (requestButton != null) {
            // Show "Request Payment" button after marking as ready
            requestButton.setVisibility(isReadyForPayment ? View.VISIBLE : View.GONE);
            requestButton.setEnabled(isReadyForPayment);
        }

        if (stepView != null) {
            if (isReadyForPayment) {
                stepView.setBackgroundResource(R.drawable.bg_step_solid_green);
                stepView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                stepView.setBackgroundResource(R.drawable.shape_circle_gray);
                stepView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }

        int labelColor = isReadyForPayment ? 0xFF1B5E20 : 0xFFBDBDBD;
        if (completedStatusView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView completedCard = (com.google.android.material.card.MaterialCardView) completedStatusView;
            if (isReadyForPayment) {
                completedCard.setCardBackgroundColor(0xFFD1E7D1);
                completedCard.setStrokeColor(0xFF1B5E20);
                completedCard.setStrokeWidth(dpToPx(1));
            } else {
                completedCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                completedCard.setStrokeColor(0xFFBDBDBD);
                completedCard.setStrokeWidth(dpToPx(1));
            }
            if (completedLabel != null) {
                completedLabel.setTextColor(labelColor);
            }
        } else if (completedStatusView instanceof TextView) {
            TextView completedText = (TextView) completedStatusView;
            completedText.setTextColor(labelColor);
            if (isReadyForPayment) {
                completedText.setBackgroundColor(0xFFD1E7D1);
            } else {
                completedText.setBackgroundResource(R.drawable.bg_input_white);
            }
        } else if (completedLabel != null) {
            completedLabel.setTextColor(labelColor);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setStepTime(View root, int viewId, String time) {
        TextView view = root.findViewById(viewId);
        if (view == null) {
            return;
        }
        if (time == null || time.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(time);
    }

    private String buildScheduleText(String date, String time) {
        if (date == null && time == null) {
            return "";
        }
        Date parsedDateTime = parseScheduleDateTime(date, time);
        if (parsedDateTime != null) {
            SimpleDateFormat output = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            return output.format(parsedDateTime);
        }

        Date parsedDate = parseScheduleDate(date);
        Date parsedTime = parseScheduleTime(time);

        if (parsedDate != null && parsedTime != null) {
            java.util.Calendar dateCal = java.util.Calendar.getInstance();
            dateCal.setTime(parsedDate);
            java.util.Calendar timeCal = java.util.Calendar.getInstance();
            timeCal.setTime(parsedTime);
            dateCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
            dateCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
            dateCal.set(java.util.Calendar.SECOND, 0);
            dateCal.set(java.util.Calendar.MILLISECOND, 0);
            SimpleDateFormat output = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            return output.format(dateCal.getTime());
        }

        if (parsedDate != null) {
            SimpleDateFormat outputDate = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return outputDate.format(parsedDate);
        }

        if (parsedTime != null) {
            SimpleDateFormat outputTime = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return outputTime.format(parsedTime);
        }

        if (date != null && time != null) {
            return date + " • " + time;
        }
        return date != null ? date : time;
    }

    private Date parseScheduleDateTime(String date, String time) {
        if (date == null) {
            return null;
        }
        String trimmed = date.trim();
        if (time != null && !time.trim().isEmpty()) {
            String combined = trimmed + " " + time.trim();
            Date combinedParsed = tryParseDate(combined, "yyyy-MM-dd HH:mm:ss");
            if (combinedParsed != null) {
                return combinedParsed;
            }
            combinedParsed = tryParseDate(combined, "yyyy-MM-dd HH:mm");
            if (combinedParsed != null) {
                return combinedParsed;
            }
        }

        Date parsed = tryParseDate(trimmed, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        if (parsed != null) {
            return parsed;
        }
        parsed = tryParseDate(trimmed, "yyyy-MM-dd'T'HH:mm:ss'Z'");
        if (parsed != null) {
            return parsed;
        }
        parsed = tryParseDate(trimmed, "yyyy-MM-dd'T'HH:mm:ss");
        if (parsed != null) {
            return parsed;
        }
        return tryParseDate(trimmed, "yyyy-MM-dd HH:mm:ss");
    }

    private Date parseScheduleDate(String date) {
        if (date == null) {
            return null;
        }
        String trimmed = date.trim();
        Date parsed = tryParseDate(trimmed, "yyyy-MM-dd");
        if (parsed != null) {
            return parsed;
        }
        parsed = tryParseDate(trimmed, "yyyy/MM/dd");
        if (parsed != null) {
            return parsed;
        }
        return tryParseDate(trimmed, "MM/dd/yyyy");
    }

    private Date parseScheduleTime(String time) {
        if (time == null) {
            return null;
        }
        String trimmed = time.trim();
        Date parsed = tryParseDate(trimmed, "HH:mm:ss");
        if (parsed != null) {
            return parsed;
        }
        parsed = tryParseDate(trimmed, "HH:mm");
        if (parsed != null) {
            return parsed;
        }
        parsed = tryParseDate(trimmed, "hh:mm a");
        if (parsed != null) {
            return parsed;
        }
        return tryParseDate(trimmed, "h:mm a");
    }

    private Date tryParseDate(String value, String pattern) {
        if (value == null) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
            format.setLenient(false);
            return format.parse(value);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    private void updateMapMarker(double latitude, double longitude) {
        if (googleMap == null || mapViewActiveJob == null) {
            return;
        }
        setMapVisible(false);

        if (latitude != 0 && longitude != 0) {
            customerLatLng = new LatLng(latitude, longitude);
        } else {
            String address = activeTicket != null ? activeTicket.getAddress() : null;
            if (address != null && !address.equals(cachedCustomerAddress)) {
                cachedCustomerAddress = address;
                geocodeAndCacheLocation(address, true);
            }
        }

        String branchName = activeTicket != null ? activeTicket.getBranch() : null;
        if (branchName != null && !branchName.trim().isEmpty()) {
            if (!branchName.equals(cachedBranchName)) {
                cachedBranchName = branchName;
                geocodeAndCacheLocation(getBranchQuery(branchName), false);
            }
        }

        renderMapMarkers();
    }

    private void geocodeAndCacheLocation(String query, boolean isCustomer) {
        if (query == null || query.trim().isEmpty() || getContext() == null) {
            return;
        }

        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(getContext(),
                        java.util.Locale.getDefault());
                java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(query, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address location = addresses.get(0);
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isCustomer) {
                                customerLatLng = new LatLng(lat, lng);
                            } else {
                                branchLatLng = new LatLng(lat, lng);
                            }
                            renderMapMarkers();
                        });
                    }
                }
            } catch (java.io.IOException ignored) {
                // Keep map as-is on failure.
            }
        }).start();
    }

    private void renderMapMarkers() {
        if (googleMap == null) {
            return;
        }

        googleMap.clear();
        int markerCount = 0;
        com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder = new com.google.android.gms.maps.model.LatLngBounds.Builder();

        if (branchLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(branchLatLng)
                    .title("Branch")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            boundsBuilder.include(branchLatLng);
            markerCount++;
        }

        if (customerLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(customerLatLng)
                    .title("Customer"));
            boundsBuilder.include(customerLatLng);
            markerCount++;
        }

        if (markerCount == 0) {
            setMapVisible(false);
            return;
        }

        setMapVisible(true);
        if (markerCount == 1) {
            LatLng target = customerLatLng != null ? customerLatLng : branchLatLng;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 15f));
        } else {
            int padding = 120;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding));
        }
    }

    private String getBranchQuery(String branchName) {
        if (branchName == null) {
            return null;
        }
        String trimmed = branchName.trim();
        String address = BRANCH_ADDRESS_MAP.get(trimmed);
        return address != null ? address : trimmed;
    }

    private void setMapVisible(boolean visible) {
        if (mapContainer == null) {
            return;
        }
        mapContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void openMapForActiveTicket() {
        if (activeTicket == null || getContext() == null) {
            return;
        }
        double lat = activeTicket.getLatitude();
        double lng = activeTicket.getLongitude();
        if (lat == 0 || lng == 0) {
            Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getContext(), EmployeeMapActivity.class);
        intent.putExtra("customer_latitude", lat);
        intent.putExtra("customer_longitude", lng);
        intent.putExtra("customer_address", activeTicket.getAddress());
        intent.putExtra("ticket_id", activeTicket.getTicketId());
        startActivity(intent);
    }

    private void updateTicketStatus(String status, String statusDetail, Runnable onSuccess) {
        if (activeTicket == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("status_detail", statusDetail);
        updates.put("updated_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("tickets").document(activeTicket.getTicketId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                activeTicket.setStatus(status);
                activeTicket.setStatusDetail(statusDetail);
                if (onSuccess != null) {
                    onSuccess.run();
                }

                // Immediate refresh to sync with local state
                if (isAdded()) {
                    loadAssignedTickets(true);
                }
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        setMapVisible(false);
        if (activeTicket != null) {
            updateMapMarker(activeTicket.getLatitude(), activeTicket.getLongitude());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapViewActiveJob != null) {
            mapViewActiveJob.onResume();
        }
        loadAssignedTickets(true);
    }

    @Override
    public void onPause() {
        if (mapViewActiveJob != null) {
            mapViewActiveJob.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapViewActiveJob != null) {
            mapViewActiveJob.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mapViewActiveJob != null) {
            mapViewActiveJob.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        // Stop auto-refresh
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }

        // Stop Firebase listener
        stopFirebaseListener();

        if (mapViewActiveJob != null) {
            mapViewActiveJob.onDestroy();
        }
        super.onDestroyView();
    }
    
    /**
     * Start Firebase real-time listener for instant ticket updates
     */
    private void startFirebaseListener() {
        if (firebaseListener == null) {
            firebaseListener = new EmployeeFirebaseListener(requireContext());
            firebaseListener.setChangeListener(new EmployeeFirebaseListener.TicketChangeListener() {
                @Override
                public void onTicketAssigned(String ticketId) {
                    android.util.Log.i("EmployeeWork", "Firebase: New ticket assigned - " + ticketId);
                    // Refresh tickets immediately
                    if (isAdded()) {
                        loadAssignedTickets(true);
                    }
                }

                @Override
                public void onTicketUpdated(String ticketId) {
                    android.util.Log.i("EmployeeWork", "Firebase: Ticket updated - " + ticketId);
                    // Refresh tickets immediately
                    if (isAdded()) {
                        loadAssignedTickets(true);
                    }
                }

                @Override
                public void onTicketRemoved(String ticketId) {
                    android.util.Log.i("EmployeeWork", "Firebase: Ticket removed - " + ticketId);
                    // Refresh tickets immediately
                    if (isAdded()) {
                        loadAssignedTickets(true);
                    }
                }
            });
        }
        firebaseListener.startListening();
    }
    
    /**
     * Stop Firebase listener
     */
    private void stopFirebaseListener() {
        if (firebaseListener != null) {
            firebaseListener.stopListening();
        }
    }

    private void startAutoRefresh() {
        autoRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Silently refresh in background (preserve UI)
                if (isAdded() && getContext() != null && !isLoadingTickets) {
                    android.util.Log.d("EmployeeWork", "Auto-refreshing tickets in background");
                    loadAssignedTickets(true);
                }

                // Schedule next refresh in 3 seconds (FAST for technician)
                if (autoRefreshHandler != null) {
                    autoRefreshHandler.postDelayed(this, 3000);
                }
            }
        };

        // Start auto-refresh after 3 seconds
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 3000);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewActiveJob != null) {
            mapViewActiveJob.onLowMemory();
        }
    }

    /**
     * Public method to manually refresh tickets (can be called from parent activity
     * if needed)
     */
    public void refreshTickets() {
        android.util.Log.d("EmployeeWork", "Manual refresh requested");
        loadAssignedTickets();
    }
}
