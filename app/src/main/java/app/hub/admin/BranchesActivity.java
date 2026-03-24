package app.hub.admin;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.util.TokenManager;

public class BranchesActivity extends AppCompatActivity {

    private RecyclerView branchesRecyclerView;
    private BranchesAdapter branchesAdapter;
    private List<Branch> branchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_branches);
            setupToolbar();
            setupRecyclerView();
            loadBranches();
        } catch (Exception e) {
            android.util.Log.e("BranchesActivity", "Error in onCreate: " + e.getMessage(), e);
            finish(); // Close activity if there's an error
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        branchesRecyclerView = findViewById(R.id.branchesRecyclerView);
        branchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        branchList = new ArrayList<>();
        branchesAdapter = new BranchesAdapter(branchList, this::onBranchClick);
        branchesRecyclerView.setAdapter(branchesAdapter);
    }

    private void loadBranches() {
        android.util.Log.d("BranchesActivity", "Loading branches from API...");
        
        TokenManager tokenManager = new TokenManager(this);
        String token = tokenManager.getToken();
        
        if (token == null) {
            android.util.Log.e("BranchesActivity", "No token available");
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<app.hub.api.BranchResponse> call = apiService.getBranches("Bearer " + token);
        
        call.enqueue(new Callback<app.hub.api.BranchResponse>() {
            @Override
            public void onResponse(Call<app.hub.api.BranchResponse> call, Response<app.hub.api.BranchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    app.hub.api.BranchResponse branchResponse = response.body();
                    
                    if (branchResponse.isSuccess()) {
                        processBranchData(branchResponse.getBranches());
                    } else {
                        android.util.Log.e("BranchesActivity", "API returned success=false: " + branchResponse.getMessage());
                    }
                } else {
                    android.util.Log.e("BranchesActivity", "API response not successful");
                }
            }
            
            @Override
            public void onFailure(Call<app.hub.api.BranchResponse> call, Throwable t) {
                android.util.Log.e("BranchesActivity", "API call failed: " + t.getMessage(), t);
            }
        });
    }

    private void processBranchData(List<app.hub.api.BranchResponse.Branch> apiBranches) {
        branchList.clear();
        
        // Convert API branch data to local Branch objects
        for (app.hub.api.BranchResponse.Branch apiBranch : apiBranches) {
            branchList.add(new Branch(
                apiBranch.getName(),
                apiBranch.getManager(),
                apiBranch.getEmployeeCount(),
                apiBranch.getDescription()
            ));
        }
        
        // Update UI on main thread
        runOnUiThread(() -> {
            branchesAdapter.notifyDataSetChanged();
            android.util.Log.d("BranchesActivity", "Loaded " + branchList.size() + " branches from API");
        });
    }

    private void onBranchClick(Branch branch) {
        Intent intent = new Intent(this, BranchDetailActivity.class);
        intent.putExtra("branch_name", branch.getName());
        intent.putExtra("branch_manager", branch.getManager());
        intent.putExtra("employee_count", branch.getEmployeeCount());
        intent.putExtra("branch_description", branch.getDescription());
        startActivity(intent);
    }

    // Branch data class
    public static class Branch {
        private String name;
        private String manager;
        private int employeeCount;
        private String description;

        public Branch(String name, String manager, int employeeCount, String description) {
            this.name = name;
            this.manager = manager;
            this.employeeCount = employeeCount;
            this.description = description;
        }

        public String getName() { return name; }
        public String getManager() { return manager; }
        public int getEmployeeCount() { return employeeCount; }
        public String getDescription() { return description; }
    }
}
