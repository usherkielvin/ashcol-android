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
import app.hub.manager.ManagerCompleteTicketsAdapter;
import app.hub.manager.ManagerTicketDetailActivity;
import app.hub.util.TokenManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("branch_name", branchName)
            .whereEqualTo("status", "completed")
            .orderBy("completed_at", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                showLoading(false);
                ticketList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    app.hub.api.TicketListResponse.TicketItem ticket = doc.toObject(app.hub.api.TicketListResponse.TicketItem.class);
                    if (ticket != null) {
                        ticket.setTicketId(doc.getId());
                        ticketList.add(ticket);
                    }
                }
                
                if (ticketList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    adapter.notifyDataSetChanged();
                }
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                showError("Error loading tickets: " + e.getMessage());
            });
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
