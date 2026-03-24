package app.hub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.BranchTicketsResponse;
import app.hub.manager.ManagerCompleteTicketsAdapter;
import app.hub.manager.ManagerTicketDetailActivity;
import app.hub.util.TokenManager;

public class BranchReportDetailActivity extends AppCompatActivity {

    private TextView tvBranchName;
    private RecyclerView rvTickets;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    
    private ManagerCompleteTicketsAdapter adapter;
    private List<app.hub.api.TicketListResponse.TicketItem> ticketList;
    private TokenManager tokenManager;
    
    private int branchId;
    private String branchName;
    private String branchLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_report_detail);

        tokenManager = new TokenManager(this);
        
        // Get branch data from intent
        branchId = getIntent().getIntExtra("branch_id", 0);
        branchName = getIntent().getStringExtra("branch_name");
        branchLocation = getIntent().getStringExtra("branch_location");

        setupToolbar();
        initViews();
        setupRecyclerView();
        loadCompletedTickets();
    }

    private void setupToolbar() {
        // Back button click listener
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    private void initViews() {
        tvBranchName = findViewById(R.id.tvBranchName);
        rvTickets = findViewById(R.id.rvTickets);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Format branch name - keep ASHCOL prefix and make uppercase
        String displayName = branchName;
        if (!displayName.toUpperCase().startsWith("ASHCOL")) {
            displayName = "ASHCOL " + displayName;
        }
        tvBranchName.setText(displayName.toUpperCase());
        swipeRefreshLayout.setOnRefreshListener(this::loadCompletedTickets);
    }

    private void setupRecyclerView() {
        ticketList = new ArrayList<>();
        adapter = new ManagerCompleteTicketsAdapter(ticketList);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(adapter);
        
        // Set click listener to view ticket details
        adapter.setOnTicketClickListener(ticket -> {
            if (ticket == null || ticket.getTicketId() == null || ticket.getTicketId().trim().isEmpty()) {
                Toast.makeText(this, "Ticket ID missing", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(this, ManagerTicketDetailActivity.class);
            intent.putExtra("ticket_id", ticket.getTicketId());
            startActivity(intent);
        });
    }

    private void loadCompletedTickets() {
        showLoading(true);
        tvEmptyState.setVisibility(View.GONE);

        String token = tokenManager.getToken();
        if (token == null) {
            showError("Authentication required");
            return;
        }

        // Load both completed and cancelled tickets
        ticketList.clear();
        loadTicketsByStatus(token, "completed", () -> {
            loadTicketsByStatus(token, "cancelled", () -> {
                showLoading(false);
                if (ticketList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvTickets.setVisibility(View.GONE);
                } else {
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(View.GONE);
                    rvTickets.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void loadTicketsByStatus(String token, String status, Runnable onComplete) {
        ApiService apiService = ApiClient.getApiService();
        Call<BranchTicketsResponse> call = apiService.getBranchTickets(
                "Bearer " + token, 
                branchId, 
                status
        );

        call.enqueue(new Callback<BranchTicketsResponse>() {
            @Override
            public void onResponse(Call<BranchTicketsResponse> call, Response<BranchTicketsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BranchTicketsResponse ticketsResponse = response.body();
                    
                    if (ticketsResponse.isSuccess()) {
                        List<BranchTicketsResponse.Ticket> tickets = ticketsResponse.getTickets();
                        
                        if (tickets != null && !tickets.isEmpty()) {
                            for (BranchTicketsResponse.Ticket ticket : tickets) {
                                ticketList.add(convertToTicketItem(ticket));
                            }
                        }
                    }
                }
                
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onFailure(Call<BranchTicketsResponse> call, Throwable t) {
                Log.e("BranchReportDetail", "API call failed for status " + status + ": " + t.getMessage(), t);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private app.hub.api.TicketListResponse.TicketItem convertToTicketItem(BranchTicketsResponse.Ticket ticket) {
        app.hub.api.TicketListResponse.TicketItem item = new app.hub.api.TicketListResponse.TicketItem();
        item.setId(ticket.getId());
        item.setTicketId(ticket.getTicketId());
        item.setTitle(ticket.getTitle());
        item.setDescription(ticket.getDescription());
        item.setServiceType(ticket.getServiceType());
        item.setStatus(ticket.getStatus());
        item.setStatusColor(ticket.getStatusColor());
        item.setCustomerName(ticket.getCustomerName());
        item.setAssignedStaff(ticket.getAssignedStaff());
        item.setCreatedAt(ticket.getCreatedAt());
        item.setUpdatedAt(ticket.getUpdatedAt());
        // Note: BranchTicketsResponse doesn't include scheduled date/time
        return item;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
        } else {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
