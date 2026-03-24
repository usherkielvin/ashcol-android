package app.hub.manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.api.TicketListResponse;

import app.hub.util.TokenManager;
import app.hub.manager.ManagerDataManager.TicketDataChangeListener;

public class ManagerWorkFragment extends Fragment implements ManagerDataManager.TicketDataChangeListener {

    private RecyclerView rvWorkLoadList;
    private SearchView searchViewWork;
    private FloatingActionButton filterWork;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipIncoming, chipScheduled, chipOngoing, chipCompleted, chipCancelled;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ManagerTicketsAdapter adapter;
    private TokenManager tokenManager;
    private List<TicketListResponse.TicketItem> tickets;
    private List<TicketListResponse.TicketItem> filteredTickets;
    private String currentFilter = "all";

    public ManagerWorkFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manager_work, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupSearch();

        chipAll.setChecked(true);
        currentFilter = "all";

        displayTicketData();

        ManagerDataManager.loadAllData(getContext(), new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {}

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                if(getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayTicketData());
                }
            }

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats, List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {}

            @Override
            public void onLoadComplete() {}

            @Override
            public void onLoadError(String error) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;
        ManagerDataManager.loadAllData(getContext(), new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {}

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                if(getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayTicketData());
                }
            }

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats, List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {}

            @Override
            public void onLoadComplete() {}

            @Override
            public void onLoadError(String error) {}
        });
    }

    private void initViews(View view) {
        rvWorkLoadList = view.findViewById(R.id.rvWorkLoadList);
        searchViewWork = view.findViewById(R.id.searchViewWork);
        filterWork = view.findViewById(R.id.filterWork);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipAll = view.findViewById(R.id.chipAll);
        chipIncoming = view.findViewById(R.id.chipIncoming);
        chipScheduled = view.findViewById(R.id.chipScheduled);
        chipOngoing = view.findViewById(R.id.chipOngoing);
        chipCompleted = view.findViewById(R.id.chipCompleted);
        chipCancelled = view.findViewById(R.id.chipCancelled);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Debug view initialization
        android.util.Log.d("ManagerWork", "RecyclerView found: " + (rvWorkLoadList != null));
        android.util.Log.d("ManagerWork", "SwipeRefreshLayout found: " + (swipeRefreshLayout != null));

        if (rvWorkLoadList != null) {
            android.util.Log.d("ManagerWork", "RecyclerView visibility: " + rvWorkLoadList.getVisibility());
        }

        tokenManager = new TokenManager(getContext());
        tickets = new ArrayList<>();
        filteredTickets = new ArrayList<>();

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        android.util.Log.d("ManagerWork",
                "Lists initialized - tickets: " + tickets.size() + ", filtered: " + filteredTickets.size());
    }

    private void setupRecyclerView() {
        adapter = new ManagerTicketsAdapter(filteredTickets);
        rvWorkLoadList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWorkLoadList.setAdapter(adapter);

        android.util.Log.d("ManagerWork",
                "RecyclerView setup complete - Adapter: " + (adapter != null ? "OK" : "NULL"));
        android.util.Log.d("ManagerWork", "RecyclerView: " + (rvWorkLoadList != null ? "OK" : "NULL"));
        android.util.Log.d("ManagerWork", "Filtered tickets size: " + filteredTickets.size());

        // Set click listener for ticket items
        adapter.setOnTicketClickListener(ticket -> {
            if (ticket == null || ticket.getTicketId() == null || ticket.getTicketId().trim().isEmpty()) {
                Toast.makeText(getContext(), "Ticket ID missing", Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("ManagerWork", "Ticket clicked: " + ticket.getTicketId());

            try {
                Intent intent = new Intent(getContext(), ManagerTicketDetailActivity.class);
                intent.putExtra("ticket_id", ticket.getTicketId());
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("ManagerWork", "Error starting ticket detail activity", e);
                Toast.makeText(getContext(), "Error opening ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty())
                return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) {
                currentFilter = "all";
            } else if (checkedId == R.id.chipIncoming) {
                currentFilter = "pending";
            } else if (checkedId == R.id.chipScheduled) {
                currentFilter = "scheduled";
            } else if (checkedId == R.id.chipOngoing) {
                currentFilter = "ongoing";
            } else if (checkedId == R.id.chipCompleted) {
                currentFilter = "completed";
            } else if (checkedId == R.id.chipCancelled) {
                currentFilter = "cancelled";
            }

            filterTickets();
        });
    }

    private void setupSearch() {
        searchViewWork.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTickets();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTickets();
                return true;
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            // Set refresh colors
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.green,
                    R.color.blue,
                    R.color.orange);

            // Set refresh listener - refresh from centralized manager
            swipeRefreshLayout.setOnRefreshListener(() -> {
                android.util.Log.d("ManagerWork", "Pull-to-refresh triggered - refreshing tickets");
                displayTicketData();
                ManagerDataManager.refreshTickets(getContext(), new ManagerDataManager.DataLoadCallback() {
                    @Override
                    public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {
                        // Not needed for ticket refresh
                    }

                    @Override
                    public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                        displayTicketData();
                        stopSwipeRefresh();
                        Toast.makeText(getContext(), "Tickets refreshed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats,
                            List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {
                        // Not needed for work fragment
                    }

                    @Override
                    public void onLoadComplete() {
                    }

                    @Override
                    public void onLoadError(String error) {
                        stopSwipeRefresh();
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });

            android.util.Log.d("ManagerWork", "SwipeRefreshLayout configured");
        }
    }

    private void displayTicketData() {
        // Get data from centralized manager
        List<TicketListResponse.TicketItem> cachedTickets = ManagerDataManager.getCachedTickets();

        if (cachedTickets != null && !cachedTickets.isEmpty()) {
            // Display cached data immediately
            tickets.clear();
            tickets.addAll(cachedTickets);

            // Apply filtering
            filterTickets();

            android.util.Log.d("ManagerWork", "Displayed " + tickets.size() + " cached tickets");
        } else {
            // No data available yet
            android.util.Log.d("ManagerWork", "No cached tickets available");
            tickets.clear();
            filterTickets(); // This will show empty state
        }
    }

    private void stopSwipeRefresh() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void filterTickets() {
        filteredTickets.clear();
        String searchQuery = searchViewWork.getQuery().toString().toLowerCase().trim();

        android.util.Log.d("ManagerWork", "Filtering tickets - Total: " + tickets.size() + ", Filter: " + currentFilter
                + ", Search: '" + searchQuery + "'");

        for (TicketListResponse.TicketItem ticket : tickets) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;
            if (ticket == null) {
                continue;
            }

            String ticketStatus = ticket.getStatus() != null ? ticket.getStatus().toLowerCase() : "";

            if (!isPendingOrOngoingTicket(ticketStatus)) {
                continue;
            }

            // Apply status filter first
            if (!currentFilter.equals("all")) {
                switch (currentFilter) {
                    case "pending":
                        matchesFilter = ticketStatus.contains("pending")
                                || ticketStatus.contains("open");
                        break;
                    case "scheduled":
                        matchesFilter = ticketStatus.contains("scheduled");
                        break;
                    case "ongoing":
                        matchesFilter = ticketStatus.contains("ongoing")
                                || ticketStatus.contains("progress")
                                || ticketStatus.contains("accepted");
                        break;
                }
            }

            // Apply search filter
            if (!searchQuery.isEmpty()) {
                matchesSearch = ticket.getTitle().toLowerCase().contains(searchQuery) ||
                        ticket.getDescription().toLowerCase().contains(searchQuery) ||
                        ticket.getTicketId().toLowerCase().contains(searchQuery);
            }

            if (matchesFilter && matchesSearch) {
                filteredTickets.add(ticket);
            }
        }

        android.util.Log.d("ManagerWork", "Filtered tickets count: " + filteredTickets.size());

        // Force RecyclerView to be visible and refresh
        if (rvWorkLoadList != null) {
            rvWorkLoadList.setVisibility(View.VISIBLE);
            rvWorkLoadList.post(() -> {
                adapter.notifyDataSetChanged();
                android.util.Log.d("ManagerWork", "RecyclerView forced refresh completed");
            });
        }

        adapter.notifyDataSetChanged();
    }

    private boolean isPendingOrOngoingTicket(String status) {
        if (status == null) return false;
        String normalized = status.toLowerCase().trim();
        return normalized.contains("pending")
                || normalized.contains("open")
                || normalized.contains("scheduled")
                || normalized.contains("ongoing")
                || normalized.contains("progress")
                || normalized.contains("accepted");
    }

    private boolean isExcludedFromWork(String status) {
        if (status == null) {
            return false;
        }
        return status.contains("cancelled")
                || status.contains("rejected")
                || status.contains("failed")
                || status.contains("completed")
                || status.contains("resolved")
                || status.contains("closed");
    }

    @Override
    public void onTicketDataChanged(List<TicketListResponse.TicketItem> tickets) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::displayTicketData);
        } else {
            displayTicketData();
        }
    }

    /**
     * Public method to manually refresh tickets (can be called from parent activity
     * if needed)
     */
    public void refreshTickets() {
        android.util.Log.d("ManagerWork", "Manual refresh requested");
        ManagerDataManager.refreshTickets(getContext(), new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {
                // Not needed
            }

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                displayTicketData();
            }

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats,
                    List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {
                // Not needed
            }

            @Override
            public void onLoadComplete() {
                Toast.makeText(getContext(), "Tickets refreshed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadError(String error) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Clear ticket cache - call this when tickets are updated (e.g., status
     * changed)
     */
    public static void clearTicketCache() {
        ManagerDataManager.clearTicketCache();
        android.util.Log.d("ManagerWork", "Ticket cache cleared");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ManagerDataManager.unregisterTicketListener(this);
    }
}
