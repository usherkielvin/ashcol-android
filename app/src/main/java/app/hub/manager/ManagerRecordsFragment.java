package app.hub.manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.api.TicketListResponse;
import app.hub.manager.ManagerDataManager.TicketDataChangeListener;

public class ManagerRecordsFragment extends Fragment implements ManagerDataManager.TicketDataChangeListener {

    private RecyclerView rvCompleteTickets;
    private TextView tvCompleteEmpty;
    private SwipeRefreshLayout swipeRefreshRecords;
    private ManagerCompleteTicketsAdapter completeAdapter;
    private List<TicketListResponse.TicketItem> completeTickets;

    public ManagerRecordsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manager_records_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        completeTickets = new ArrayList<>();

        initViews(view);
        setupCompleteRecyclerView();
        loadCompleteTickets();
        
        // Register for ticket updates
        ManagerDataManager.registerTicketListener(this);
    }

    private void initViews(View view) {
        rvCompleteTickets = view.findViewById(R.id.rvCompleteTickets);
        tvCompleteEmpty = view.findViewById(R.id.tvCompleteEmpty);
        swipeRefreshRecords = view.findViewById(R.id.swipeRefreshRecords);
        
        swipeRefreshRecords.setOnRefreshListener(this::refreshCompleteTickets);
    }

    private void setupCompleteRecyclerView() {
        completeAdapter = new ManagerCompleteTicketsAdapter(completeTickets);
        rvCompleteTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompleteTickets.setAdapter(completeAdapter);
        
        completeAdapter.setOnTicketClickListener(ticket -> {
            if (ticket == null || ticket.getTicketId() == null || ticket.getTicketId().trim().isEmpty()) {
                Toast.makeText(getContext(), "Ticket ID missing", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(getContext(), ManagerTicketDetailActivity.class);
            intent.putExtra("ticket_id", ticket.getTicketId());
            startActivity(intent);
        });
    }

    private void loadCompleteTickets() {
        List<TicketListResponse.TicketItem> cachedTickets = ManagerDataManager.getCachedTickets();
        
        completeTickets.clear();
        if (cachedTickets != null && !cachedTickets.isEmpty()) {
            for (TicketListResponse.TicketItem ticket : cachedTickets) {
                if (ticket != null && isCompletedTicket(ticket.getStatus())) {
                    completeTickets.add(ticket);
                }
            }
        }
        
        completeAdapter.notifyDataSetChanged();
        
        if (completeTickets.isEmpty()) {
            tvCompleteEmpty.setVisibility(View.VISIBLE);
            rvCompleteTickets.setVisibility(View.GONE);
        } else {
            tvCompleteEmpty.setVisibility(View.GONE);
            rvCompleteTickets.setVisibility(View.VISIBLE);
        }
    }

    private void refreshCompleteTickets() {
        ManagerDataManager.loadAllData(getContext(), new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {}

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                if(getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadCompleteTickets();
                        stopSwipeRefresh();
                        Toast.makeText(getContext(), "Records refreshed", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats, List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {}

            @Override
            public void onLoadComplete() {}

            @Override
            public void onLoadError(String error) {
                stopSwipeRefresh();
                if(getContext() != null) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isCompletedTicket(String status) {
        if (status == null) return false;
        String normalized = status.toLowerCase().trim();
        return normalized.contains("completed") 
            || normalized.contains("resolved")
            || normalized.contains("closed")
            || normalized.contains("cancelled")
            || normalized.contains("canceled")
            || normalized.contains("rejected");
    }

    private void stopSwipeRefresh() {
        if (swipeRefreshRecords != null && swipeRefreshRecords.isRefreshing()) {
            swipeRefreshRecords.setRefreshing(false);
        }
    }

    @Override
    public void onTicketDataChanged(List<TicketListResponse.TicketItem> tickets) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::loadCompleteTickets);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ManagerDataManager.unregisterTicketListener(this);
    }
}
