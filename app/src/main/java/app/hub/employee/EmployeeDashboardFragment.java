package app.hub.employee;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.hub.R;
import app.hub.api.TicketListResponse;
import app.hub.util.TokenManager;

public class EmployeeDashboardFragment extends Fragment {

    private LinearLayout todayWorkContainer;
    private FirebaseEmployeeListener firebaseEmployeeListener;
    private static List<TicketListResponse.TicketItem> cachedTodayWork = null;
    private static List<TicketListResponse.TicketItem> cachedScheduleTickets = null;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private android.os.Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;

    public EmployeeDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_employee_home, container, false);
    }

    @Override
    public void onViewCreated(@androidx.annotation.NonNull View view,
            @androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        app.hub.util.TokenManager tokenManager = new app.hub.util.TokenManager(requireContext());

        android.widget.TextView tvHeaderName = view.findViewById(R.id.tvHeaderName);
        android.widget.TextView tvHeaderBranch = view.findViewById(R.id.tvHeaderBranch);
        android.widget.TextView tvTodayDate = view.findViewById(R.id.tvTodayDate);

        if (tvHeaderName != null) {
            String name = tokenManager.getName();
            if (name != null) {
                // Extract first name for a friendlier greeting
                String[] parts = name.split(" ");
                if (parts.length > 0) {
                    name = parts[0];
                }
                tvHeaderName.setText("Hello, " + name);
            } else {
                tvHeaderName.setText("Hello, Employee");
            }
        }

        if (tvHeaderBranch != null) {
            String branch = tokenManager.getUserBranch();
            if (branch != null && !branch.isEmpty()) {
                tvHeaderBranch.setText(branch);
                tvHeaderBranch.setVisibility(View.VISIBLE);
            } else {
                // Try to see if there is a general branch saved
                String cachedBranch = tokenManager.getCachedBranch();
                if (cachedBranch != null) {
                    tvHeaderBranch.setText(cachedBranch);
                    tvHeaderBranch.setVisibility(View.VISIBLE);
                } else {
                    tvHeaderBranch.setVisibility(View.GONE);
                }
            }
        }

        // Set date header and legacy date view (if used elsewhere)
        String headerDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(new Date());
        TextView tvTodayWorkTitle = view.findViewById(R.id.tvTodayWorkTitle);
        if (tvTodayWorkTitle != null) {
            tvTodayWorkTitle.setText(headerDate);
        }
        if (tvTodayDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH);
            String currentDate = dateFormat.format(new Date()).toUpperCase();
            tvTodayDate.setText(currentDate);
        }

        // Setup View All Schedules button click
        com.google.android.material.button.MaterialButton btnViewAllSchedules = view
                .findViewById(R.id.btnViewAllSchedules);
        if (btnViewAllSchedules != null) {
            btnViewAllSchedules.setOnClickListener(v -> {
                // Navigate to EmployeeScheduleFragment
                if (getActivity() != null) {
                    // Update navigation indicator
                    if (getActivity() instanceof EmployeeDashboardActivity) {
                        ((EmployeeDashboardActivity) getActivity()).updateNavigationIndicator(R.id.nav_sched);
                    }

                    androidx.fragment.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragment_container, new EmployeeScheduleFragment());
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });
        }

        todayWorkContainer = view.findViewById(R.id.todayWorkContainer);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // Refresh with single API call
                loadAllTickets();
            });
        }

        // Preload cached data to keep home tab smooth when switching
        if (cachedTodayWork != null) {
            displayTodayWork(new ArrayList<>(cachedTodayWork));
        }
        if (cachedScheduleTickets != null) {
            displayAssignedSchedules(new ArrayList<>(cachedScheduleTickets));
        }

        // Load all tickets with single API call
        loadAllTickets();

        // Start real-time listener for updates
        setupRealtimeListener();

        // Start auto-refresh in background
        startAutoRefresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (firebaseEmployeeListener != null && !firebaseEmployeeListener.isListening()) {
            firebaseEmployeeListener.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firebaseEmployeeListener != null) {
            firebaseEmployeeListener.stopListening();
        }
    }

    private void setupRealtimeListener() {
        if (getContext() == null)
            return;

        firebaseEmployeeListener = new FirebaseEmployeeListener(getContext());
        firebaseEmployeeListener.setOnScheduleChangeListener(new FirebaseEmployeeListener.OnScheduleChangeListener() {
            @Override
            public void onScheduleChanged() {
                if (!isAdded())
                    return;
                loadAllTickets();
            }

            @Override
            public void onError(String error) {
                // No UI noise needed; keep home quiet
            }
        });
        firebaseEmployeeListener.startListening();
    }

    /**
     * Optimized: Single API call to load all tickets, then filter for today's work
     * and schedules
     */
    private void loadAllTickets() {
        if (!isAdded() || getContext() == null)
            return;

        TokenManager tokenManager = new TokenManager(requireContext());
        String token = tokenManager.getToken();
        if (token == null)
            return;

        ApiService apiService = ApiClient.getApiService();
        Call<TicketListResponse> call = apiService.getEmployeeTickets("Bearer " + token);

        call.enqueue(new Callback<TicketListResponse>() {
            @Override
            public void onResponse(Call<TicketListResponse> call, Response<TicketListResponse> response) {
                if (!isAdded() || getContext() == null)
                    return;

                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TicketListResponse.TicketItem> allTickets = response.body().getTickets();
                    List<TicketListResponse.TicketItem> safeTickets = allTickets != null ? allTickets
                            : new ArrayList<>();

                    // Cache all tickets
                    cachedTodayWork = new ArrayList<>(safeTickets);
                    cachedScheduleTickets = new ArrayList<>(safeTickets);

                    // Display both sections
                    displayTodayWork(safeTickets);
                    displayAssignedSchedules(safeTickets);
                } else {
                    displayTodayWork(new ArrayList<>());
                    displayAssignedSchedules(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<TicketListResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null)
                    return;

                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                displayTodayWork(new ArrayList<>());
                displayAssignedSchedules(new ArrayList<>());
            }
        });
    }

    private void loadAssignedSchedules() {
        if (!isAdded() || getContext() == null)
            return;

        TokenManager tokenManager = new TokenManager(requireContext());
        String token = tokenManager.getToken();
        if (token == null)
            return;

        ApiService apiService = ApiClient.getApiService();
        Call<TicketListResponse> call = apiService.getEmployeeTickets("Bearer " + token);

        call.enqueue(new Callback<TicketListResponse>() {
            @Override
            public void onResponse(Call<TicketListResponse> call, Response<TicketListResponse> response) {
                if (!isAdded() || getContext() == null)
                    return;

                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TicketListResponse.TicketItem> tickets = response.body().getTickets();
                    List<TicketListResponse.TicketItem> safeTickets = tickets != null ? tickets : new ArrayList<>();
                    cachedScheduleTickets = new ArrayList<>(safeTickets);
                    displayAssignedSchedules(safeTickets);
                } else {
                    displayAssignedSchedules(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<TicketListResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null)
                    return;

                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                displayAssignedSchedules(new ArrayList<>());
            }
        });
    }

    private void displayAssignedSchedules(List<TicketListResponse.TicketItem> tickets) {
        if (getView() == null)
            return;

        LinearLayout scheduleContainer = getView().findViewById(R.id.scheduleItemsContainer);
        TextView tvNoEventsToday = getView().findViewById(R.id.tvNoEventsToday);

        if (scheduleContainer == null || tvNoEventsToday == null)
            return;

        scheduleContainer.removeAllViews();

        List<TicketListResponse.TicketItem> activeTickets = new ArrayList<>();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
        for (TicketListResponse.TicketItem ticket : tickets) {
            String status = ticket.getStatus();
            if (status == null)
                continue;

            String normalized = status.trim().toLowerCase(Locale.ENGLISH);
            boolean isInProgress = "in_progress".equals(normalized)
                    || "in progress".equals(normalized)
                    || "ongoing".equals(normalized);
            boolean isScheduled = "scheduled".equals(normalized);

            if (isInProgress || isScheduled) {
                String scheduledDate = ticket.getScheduledDate();
                boolean isToday = scheduledDate != null && scheduledDate.equals(todayDate);
                if (!isToday) {
                    activeTickets.add(ticket);
                }
            }
        }

        if (activeTickets.isEmpty()) {
            tvNoEventsToday.setText("No schedules available.");
            tvNoEventsToday.setVisibility(View.VISIBLE);
            return;
        }

        tvNoEventsToday.setVisibility(View.GONE);
        for (TicketListResponse.TicketItem ticket : activeTickets) {
            View scheduleItem = createScheduleItemView(ticket);
            if (scheduleItem != null) {
                scheduleContainer.addView(scheduleItem);
            }
        }
    }

    private void loadTodayWork() {
        if (!isAdded() || getContext() == null)
            return;

        TokenManager tokenManager = new TokenManager(requireContext());
        String token = tokenManager.getToken();
        if (token == null)
            return;

        ApiService apiService = ApiClient.getApiService();
        Call<TicketListResponse> call = apiService.getEmployeeTickets("Bearer " + token);

        call.enqueue(new Callback<TicketListResponse>() {
            @Override
            public void onResponse(Call<TicketListResponse> call, Response<TicketListResponse> response) {
                if (!isAdded() || getContext() == null)
                    return;

                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TicketListResponse.TicketItem> tickets = response.body().getTickets();
                    List<TicketListResponse.TicketItem> safeTickets = tickets != null ? tickets : new ArrayList<>();
                    cachedTodayWork = new ArrayList<>(safeTickets);
                    displayTodayWork(safeTickets);
                } else {
                    displayTodayWork(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<TicketListResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null)
                    return;

                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                displayTodayWork(new ArrayList<>());
            }
        });
    }

    private void displayTodayWork(List<TicketListResponse.TicketItem> tickets) {
        if (todayWorkContainer == null || getContext() == null)
            return;

        todayWorkContainer.removeAllViews();

        List<TicketListResponse.TicketItem> todayWork = new ArrayList<>();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());

        for (TicketListResponse.TicketItem ticket : tickets) {
            String status = ticket.getStatus();
            if (status == null)
                continue;

            String normalized = status.trim().toLowerCase(Locale.ENGLISH);
            boolean isInProgress = "in_progress".equals(normalized) || "in progress".equals(normalized)
                    || "ongoing".equals(normalized);
            boolean isScheduled = "scheduled".equals(normalized);

            String scheduledDate = ticket.getScheduledDate();
            boolean dateMatch = scheduledDate == null
                    || scheduledDate.isEmpty()
                    || scheduledDate.equals(todayDate);

            if ((isInProgress && dateMatch) || (isScheduled && dateMatch)) {
                todayWork.add(ticket);
            }
        }

        if (todayWork.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("No work scheduled for today.");
            emptyView.setTextColor(getResources().getColor(R.color.dark_gray));
            emptyView.setTextSize(14f);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            emptyView.setPadding(padding, padding, padding, padding);
            todayWorkContainer.addView(emptyView);
            return;
        }

        for (TicketListResponse.TicketItem ticket : todayWork) {
            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_employee_home_daywork,
                    todayWorkContainer, false);

            TextView tvWorkTitle = itemView.findViewById(R.id.tvWorkTitle);
            TextView tvWorkDetail = itemView.findViewById(R.id.tvWorkDetail);
            TextView tvScheduleDate = itemView.findViewById(R.id.tvScheduleDate);
            TextView tvScheduleTime = itemView.findViewById(R.id.tvScheduleTime);
            TextView tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            TextView tvContactNumber = itemView.findViewById(R.id.tvContactNumber);
            com.google.android.material.button.MaterialButton btnWorkStatus = itemView.findViewById(R.id.btnWorkStatus);

            String title = ticket.getServiceType() != null ? ticket.getServiceType()
                    : (ticket.getTitle() != null ? ticket.getTitle() : "Service Request");
            if (tvWorkTitle != null)
                tvWorkTitle.setText(title);

            // Show ticket ID in detail field
            String ticketId = ticket.getTicketId();
            if (tvWorkDetail != null) {
                tvWorkDetail.setText("• " + (ticketId != null ? ticketId : "TCK-000"));
            }

            // Format schedule date as "Feb 11, 2026"
            if (tvScheduleDate != null) {
                String dateDisplay = this.formatScheduleDate(ticket.getScheduledDate());
                tvScheduleDate.setText(dateDisplay);
            }

            // Format schedule time as "10:11 AM"
            if (tvScheduleTime != null) {
                String timeDisplay = this.formatScheduleTime(ticket.getScheduledTime());
                tvScheduleTime.setText(timeDisplay);
            }


            if (tvCustomerName != null) {
                tvCustomerName.setText(ticket.getCustomerName() != null ? ticket.getCustomerName() : "--");
            }

            if (tvContactNumber != null) {
                tvContactNumber.setText(ticket.getContact() != null ? ticket.getContact() : "--");
            }

            if (btnWorkStatus != null) {
                String status = ticket.getStatus() != null ? ticket.getStatus() : "In progress";
                btnWorkStatus.setText("Status: " + status.replace('_', ' '));

                String statusColor = ticket.getStatusColor();
                if (statusColor != null && statusColor.startsWith("#")) {
                    try {
                        int color = android.graphics.Color.parseColor(statusColor);
                        btnWorkStatus.setTextColor(color);
                        btnWorkStatus.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            itemView.setOnClickListener(v -> {
                if (ticket.getTicketId() != null && getContext() != null) {
                    android.content.Intent intent = new android.content.Intent(getContext(),
                            EmployeeTicketDetailActivity.class);
                    intent.putExtra("ticket_id", ticket.getTicketId());
                    startActivity(intent);
                }
            });
            todayWorkContainer.addView(itemView);
        }
    }

    /**
     * Format schedule date as "Feb 11, 2026"
     */
    private String formatScheduleDate(String date) {
        if (date == null || date.isEmpty())
            return "--";

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
            Date parsedDate = inputFormat.parse(date);
            if (parsedDate != null) {
                return outputFormat.format(parsedDate);
            }
        } catch (Exception e) {
            // If parsing fails, return the original date
        }
        return date;
    }

    /**
     * Format schedule time as "10:11 AM"
     */
    private String formatScheduleTime(String time) {
        if (time == null || time.isEmpty())
            return "--";

        try {
            // Try parsing HH:mm:ss format
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date parsedTime = inputFormat.parse(time);
            if (parsedTime != null) {
                return outputFormat.format(parsedTime);
            }
        } catch (Exception e) {
            try {
                // Try parsing HH:mm format
                SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                Date parsedTime = inputFormat.parse(time);
                if (parsedTime != null) {
                    return outputFormat.format(parsedTime);
                }
            } catch (Exception ex) {
                // If parsing fails, return the original time
            }
        }
        return time;
    }

    private String formatAssignedTime(String updatedAt, String createdAt) {
        String source = (updatedAt != null && !updatedAt.isEmpty()) ? updatedAt : createdAt;
        if (source == null || source.isEmpty()) {
            return "--";
        }

        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
                SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                Date parsedTime = inputFormat.parse(source);
                if (parsedTime != null) {
                    return outputFormat.format(parsedTime);
                }
            } catch (Exception ignored) {
                // Try the next format.
            }
        }

        return source;
    }

    private View createScheduleItemView(TicketListResponse.TicketItem ticket) {
        if (getContext() == null)
            return null;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View itemView = inflater.inflate(R.layout.item_employee_home_schedule, null);

        // Hide the date header since schedules are list-based
        TextView tvScheduleDate = itemView.findViewById(R.id.tvScheduleDate);
        if (tvScheduleDate != null) {
            tvScheduleDate.setVisibility(View.GONE);
        }

        TextView tvScheduleTitle = itemView.findViewById(R.id.tvScheduleTitle);
        TextView tvScheduleDetail = itemView.findViewById(R.id.tvScheduleDetail);
        TextView tvRequestedById = itemView.findViewById(R.id.tvRequestedById);
        TextView tvScheduleTime = itemView.findViewById(R.id.tvScheduleTime);
        com.google.android.material.button.MaterialButton btnScheduleStatus = itemView
                .findViewById(R.id.btnScheduleStatus);

        if (tvScheduleTitle != null) {
            String title = ticket.getServiceType() != null ? ticket.getServiceType()
                    : (ticket.getTitle() != null ? ticket.getTitle() : "Service Request");
            tvScheduleTitle.setText(title);
        }

        if (tvScheduleDetail != null) {
            String detail = ticket.getDescription() != null && !ticket.getDescription().isEmpty()
                    ? ticket.getDescription()
                    : "Assigned ticket";
            tvScheduleDetail.setText("• " + detail);
        }

        if (tvRequestedById != null) {
            String ticketId = ticket.getTicketId();
            tvRequestedById.setText(ticketId != null ? ticketId : "--");
        }

        if (tvScheduleTime != null) {
            String dateDisplay = formatScheduleDate(ticket.getScheduledDate());
            String timeDisplay = formatScheduleTime(ticket.getScheduledTime());
            tvScheduleTime.setText(dateDisplay + " - " + timeDisplay);
        }

        if (btnScheduleStatus != null) {
            String status = ticket.getStatus() != null ? ticket.getStatus() : "Pending";
            btnScheduleStatus.setText("Status: " + status.replace('_', ' '));

            String statusColor = ticket.getStatusColor();
            if (statusColor != null && statusColor.startsWith("#")) {
                try {
                    int color = android.graphics.Color.parseColor(statusColor);
                    btnScheduleStatus.setTextColor(color);
                    btnScheduleStatus.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Add click listener to open ticket detail
        View scheduleItemLayout = itemView.findViewById(R.id.scheduleItemLayout);
        if (scheduleItemLayout != null) {
            scheduleItemLayout.setOnClickListener(v -> {
                if (ticket.getTicketId() != null) {
                    android.content.Intent intent = new android.content.Intent(getContext(),
                            app.hub.employee.EmployeeTicketDetailActivity.class);
                    intent.putExtra("ticket_id", ticket.getTicketId());
                    startActivity(intent);
                }
            });
        }

        return itemView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible - single API call
        loadAllTickets();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop auto-refresh
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void startAutoRefresh() {
        autoRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Silently refresh in background
                if (isAdded() && getContext() != null) {
                    android.util.Log.d("EmployeeDashboard", "Auto-refreshing tickets in background");
                    loadAllTickets();
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
}
