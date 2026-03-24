package app.hub.manager;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.DashboardStatsResponse;
import app.hub.api.EmployeeResponse;
import app.hub.api.TicketListResponse;

public class ManagerHomeFragment extends Fragment implements ManagerDataManager.EmployeeDataChangeListener,
    ManagerDataManager.DashboardDataChangeListener,
    ManagerDataManager.TicketDataChangeListener {

    private static final int RECENT_TICKETS_LIMIT = 2;

    private RecyclerView recentActivityRecyclerView;
    private RecentActivityAdapter recentActivityAdapter;

    private RecyclerView rvEmployeePreview;
    private EmployeePreviewAdapter employeePreviewAdapter;

    public ManagerHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_home, container, false);
        initializeViews(view);
        updateWelcomeMessage(view);
        setupRecyclerView();
        loadDashboardData();
        refreshDataInBackground();
        return view;
    }

    private void initializeViews(View view) {
        recentActivityRecyclerView = view.findViewById(R.id.recentActivityFlow);
        rvEmployeePreview = view.findViewById(R.id.rvEmployeePreview);
        // tvEmployeeCount and btnMenu are compatibility stub Views - skip initialization
        // tvEmployeeCount = view.findViewById(R.id.tvEmployeeCount);
        // btnMenu = view.findViewById(R.id.btnMenu);
    }

    private void showSideMenu() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext(), R.style.DialogSlideLeft);
        dialog.setContentView(R.layout.service_operations_side_sheet);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.START;
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(layoutParams);
        }

        // Initialize actions inside the sidebar
        View btnCleaning = dialog.findViewById(R.id.btnSidebarCleaning);
        if (btnCleaning != null) {
            btnCleaning.setOnClickListener(v -> {
                // Handle cleaning click
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void setupRecyclerView() {
        recentActivityAdapter = new RecentActivityAdapter(getContext());
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentActivityRecyclerView.setAdapter(recentActivityAdapter);
        recentActivityRecyclerView.setNestedScrollingEnabled(false);

        employeePreviewAdapter = new EmployeePreviewAdapter(getContext());
        rvEmployeePreview.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEmployeePreview.setAdapter(employeePreviewAdapter);
        rvEmployeePreview.setNestedScrollingEnabled(false);

        ManagerDataManager.registerEmployeeListener(this);
        ManagerDataManager.registerDashboardListener(this);
        ManagerDataManager.registerTicketListener(this);
        loadEmployeeData();
    }

    private void loadDashboardData() {
        DashboardStatsResponse.Stats stats = ManagerDataManager.getCachedDashboardStats();
        List<DashboardStatsResponse.RecentTicket> recentTickets = ManagerDataManager.getCachedRecentTickets();

        if (stats != null) {
            updateDashboardStats(stats);
        }

        if (!recentTickets.isEmpty()) {
            recentActivityAdapter.setRecentTickets(limitRecentTickets(recentTickets));
        } else {
            List<DashboardStatsResponse.RecentTicket> fallback = buildRecentTicketsFromTickets(
                    ManagerDataManager.getCachedTickets(), RECENT_TICKETS_LIMIT);
            recentActivityAdapter.setRecentTickets(fallback);
        }
    }

    private void updateDashboardStats(DashboardStatsResponse.Stats stats) {
        android.util.Log.d("ManagerHome", "Total Tickets: " + stats.getTotalTickets());
        View view = getView();
        if (view == null) return;
        // Ongoing counter (in progress)
        TextView tvOngoing = null;
        TextView tvPending = null;
        // Find the first and second large stat TextViews by traversing the layout
        // Ongoing (first card)
        try {
            ViewGroup statsRow = view.findViewById(R.id.statsRow);
            // First card: MaterialCardView -> LinearLayout -> TextView (first child)
            ViewGroup firstCard = (ViewGroup) statsRow.getChildAt(0);
            ViewGroup firstCardLinearLayout = (ViewGroup) firstCard.getChildAt(0);
            tvOngoing = (TextView) firstCardLinearLayout.getChildAt(0);
            
            // Second card: MaterialCardView -> LinearLayout -> TextView (first child)
            ViewGroup secondCard = (ViewGroup) statsRow.getChildAt(1);
            ViewGroup secondCardLinearLayout = (ViewGroup) secondCard.getChildAt(0);
            tvPending = (TextView) secondCardLinearLayout.getChildAt(0);
        } catch (Exception e) {
            // fallback: do nothing
        }
        if (tvOngoing != null) {
            if (stats.getInProgress() == 0) {
                tvOngoing.setText("No");
            } else {
                tvOngoing.setText(String.format("%02d", stats.getInProgress()));
            }
        }
        if (tvPending != null) {
            if (stats.getPending() == 0) {
                tvPending.setText("No");
            } else {
                tvPending.setText(String.format("%02d", stats.getPending()));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            updateWelcomeMessage(getView());
        }
        loadDashboardData();
        refreshDataInBackground();
    }

    private void updateWelcomeMessage(View view) {
        android.widget.TextView tvWelcome = view.findViewById(R.id.tvManagerLocation);
        if (tvWelcome != null) {
            String branchName = ManagerDataManager.getCachedBranchName();
            if (branchName != null && !branchName.isEmpty() && !branchName.equals("No Branch Assigned")) {
                tvWelcome.setText(branchName.toUpperCase());
            } else {
                tvWelcome.setText(R.string.default_branch_name);
            }
        }
    }

    private void refreshDataInBackground() {
        ManagerDataManager.loadAllData(getContext(), new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        employeePreviewAdapter.setEmployees(employees);
                        updateEmployeeCount(employees.size());
                    });
                }
            }

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                // This is now handled by onDashboardStatsLoaded
            }

            @Override
            public void onDashboardStatsLoaded(DashboardStatsResponse.Stats stats, List<DashboardStatsResponse.RecentTicket> recentTickets) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateDashboardStats(stats);
                        recentActivityAdapter.setRecentTickets(limitRecentTickets(recentTickets));
                    });
                }
            }

            @Override
            public void onLoadComplete() {
                // All data is loaded
            }

            @Override
            public void onLoadError(String error) {
                // Handle error
            }
        });
    }

    private void loadEmployeeData() {
        List<EmployeeResponse.Employee> employees = ManagerDataManager.getCachedEmployees();
        if (!employees.isEmpty()) {
            employeePreviewAdapter.setEmployees(employees);
            updateEmployeeCount(employees.size());
        } else {
            updateEmployeeCount(0);
        }
    }

    private void updateEmployeeCount(int count) {
        // tvEmployeeCount is a compatibility stub - no longer used in the UI
        // Employee count is now shown through the RecyclerView adapter
    }

    @Override
    public void onEmployeeDataChanged(String branchName, List<EmployeeResponse.Employee> employees) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                employeePreviewAdapter.setEmployees(employees);
                updateEmployeeCount(employees.size());
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ManagerDataManager.unregisterEmployeeListener(this);
        ManagerDataManager.unregisterDashboardListener(this);
        ManagerDataManager.unregisterTicketListener(this);
    }

    @Override
    public void onDashboardDataChanged(DashboardStatsResponse.Stats stats,
            List<DashboardStatsResponse.RecentTicket> recentTickets) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (stats != null) {
                    updateDashboardStats(stats);
                }
                if (recentTickets != null && !recentTickets.isEmpty()) {
                    recentActivityAdapter.setRecentTickets(limitRecentTickets(recentTickets));
                } else {
                    List<DashboardStatsResponse.RecentTicket> fallback = buildRecentTicketsFromTickets(
                            ManagerDataManager.getCachedTickets(), RECENT_TICKETS_LIMIT);
                    recentActivityAdapter.setRecentTickets(fallback);
                }
            });
        }
    }

    @Override
    public void onTicketDataChanged(List<TicketListResponse.TicketItem> tickets) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                List<DashboardStatsResponse.RecentTicket> fallback = buildRecentTicketsFromTickets(
                        tickets, RECENT_TICKETS_LIMIT);
                if (!fallback.isEmpty()) {
                    recentActivityAdapter.setRecentTickets(fallback);
                }
            });
        }
    }

    private List<DashboardStatsResponse.RecentTicket> buildRecentTicketsFromTickets(
            List<TicketListResponse.TicketItem> tickets, int limit) {
        List<DashboardStatsResponse.RecentTicket> recent = new ArrayList<>();
        if (tickets == null || tickets.isEmpty()) {
            return recent;
        }

        int count = 0;
        for (TicketListResponse.TicketItem item : tickets) {
            if (item == null) {
                continue;
            }
            if (!isAllowedRecentStatus(item.getStatus())) {
                continue;
            }
            if (containsTicketId(recent, item.getTicketId())) {
                continue;
            }

            DashboardStatsResponse.RecentTicket recentTicket = new DashboardStatsResponse.RecentTicket();
            recentTicket.setTicketId(item.getTicketId());
            recentTicket.setStatus(item.getStatus());
            recentTicket.setStatusColor(item.getStatusColor());
            recentTicket.setCustomerName(item.getCustomerName());
            recentTicket.setServiceType(item.getServiceType());
            recentTicket.setDescription(item.getDescription());
            recentTicket.setAddress(item.getAddress());
            recentTicket.setCreatedAt(item.getCreatedAt());

            recent.add(recentTicket);
            count++;
            if (limit > 0 && count >= limit) {
                break;
            }
        }

        return recent;
    }

    private List<DashboardStatsResponse.RecentTicket> limitRecentTickets(
            List<DashboardStatsResponse.RecentTicket> recentTickets) {
        if (recentTickets == null || recentTickets.isEmpty()) {
            return new ArrayList<>();
        }
        List<DashboardStatsResponse.RecentTicket> trimmed = new ArrayList<>();
        for (DashboardStatsResponse.RecentTicket ticket : recentTickets) {
            if (ticket == null) {
                continue;
            }
            if (!isAllowedRecentStatus(ticket.getStatus())) {
                continue;
            }
            if (containsTicketId(trimmed, ticket.getTicketId())) {
                continue;
            }
            trimmed.add(ticket);
            if (trimmed.size() >= RECENT_TICKETS_LIMIT) {
                break;
            }
        }
        return trimmed;
    }

    private boolean containsTicketId(List<DashboardStatsResponse.RecentTicket> tickets, String ticketId) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            return false;
        }
        for (DashboardStatsResponse.RecentTicket ticket : tickets) {
            if (ticket != null && ticketId.equals(ticket.getTicketId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedRecentStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return !"completed".equalsIgnoreCase(normalized)
                && !"cancelled".equalsIgnoreCase(normalized)
                && !"rejected".equalsIgnoreCase(normalized)
                && !"failed".equalsIgnoreCase(normalized);
    }
}
