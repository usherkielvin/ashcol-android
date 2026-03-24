package app.hub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeFragment extends Fragment {
    
    private LinearLayout branchPreviewLayout;
    private RecyclerView managersRecyclerView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);
        
        setupViews(view);
        setupButtons(view);
        loadPreviewData();
        
        return view;
    }
    
    private void setupViews(View view) {
        branchPreviewLayout = view.findViewById(R.id.branchPreviewLayout);
        managersRecyclerView = view.findViewById(R.id.managersRecyclerView);
        
        // Setup managers RecyclerView
        if (managersRecyclerView != null) {
            managersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            managersRecyclerView.setAdapter(new ManagersAdapter(new ArrayList<>(), null));
        }
    }
    
    private void setupButtons(View view) {
        // View All Managers text click listener - Navigate to Operations tab (Manager)
        TextView btnViewAllManagers = view.findViewById(R.id.btnViewAllManagers);
        if (btnViewAllManagers != null) {
            btnViewAllManagers.setOnClickListener(v -> {
                try {
                    // Navigate to Operations tab and show Manager tab
                    if (getActivity() instanceof AdminDashboardActivity) {
                        AdminDashboardActivity activity = (AdminDashboardActivity) getActivity();
                        activity.navigateToOperationsTab(true); // true = show manager tab
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error navigating to managers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        }
        
        // View All Branches text click listener - Navigate to Operations tab (Branch)
        TextView viewAllBranches = view.findViewById(R.id.viewAllBranches);
        if (viewAllBranches != null) {
            viewAllBranches.setOnClickListener(v -> {
                try {
                    // Navigate to Operations tab and show Branch tab
                    if (getActivity() instanceof AdminDashboardActivity) {
                        AdminDashboardActivity activity = (AdminDashboardActivity) getActivity();
                        activity.navigateToOperationsTab(false); // false = show branch tab
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error navigating to branches: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        }
    }
    
    private void loadPreviewData() {
        android.util.Log.d("AdminHome", "Loading preview data from API...");
        
        TokenManager tokenManager = new TokenManager(getContext());
        String token = tokenManager.getToken();
        
        if (token == null) {
            android.util.Log.e("AdminHome", "No token available");
            showErrorInBranchPreview("Error: Not authenticated");
            return;
        }

        // Load branches data for preview
        ApiService apiService = ApiClient.getApiService();
        Call<app.hub.api.BranchResponse> call = apiService.getBranches("Bearer " + token);
        
        call.enqueue(new Callback<app.hub.api.BranchResponse>() {
            @Override
            public void onResponse(Call<app.hub.api.BranchResponse> call, Response<app.hub.api.BranchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    app.hub.api.BranchResponse branchResponse = response.body();
                    
                    if (branchResponse.isSuccess()) {
                        processBranchPreviewData(branchResponse.getBranches());
                        loadEmployeesForManagerPreview(tokenManager.getToken());
                    } else {
                        android.util.Log.e("AdminHome", "Branches API returned success=false: " + branchResponse.getMessage());
                        showErrorInBranchPreview("Error loading branches");
                    }
                } else {
                    android.util.Log.e("AdminHome", "Branches API response not successful");
                    showErrorInBranchPreview("Error loading branches");
                }
            }
            
            @Override
            public void onFailure(Call<app.hub.api.BranchResponse> call, Throwable t) {
                android.util.Log.e("AdminHome", "Branches API call failed: " + t.getMessage(), t);
                showErrorInBranchPreview("Error loading branches");
            }
        });
    }

    private void processBranchPreviewData(List<app.hub.api.BranchResponse.Branch> branches) {
        // Sort branches by employee count (descending) to show most active branches first
        branches.sort((a, b) -> Integer.compare(b.getEmployeeCount(), a.getEmployeeCount()));
        
        // Update UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateBranchPreview(branches);
            });
        }
    }

    private void updateBranchPreview(List<app.hub.api.BranchResponse.Branch> branches) {
        if (branchPreviewLayout != null) {
            branchPreviewLayout.removeAllViews();
            
            // Show top 3 branches or all if less than 3
            int maxBranches = Math.min(3, branches.size());
            int activeBranches = 0;
            
            for (int i = 0; i < branches.size() && activeBranches < maxBranches; i++) {
                app.hub.api.BranchResponse.Branch branch = branches.get(i);
                
                // Only show branches with employees
                if (branch.getEmployeeCount() > 0) {
                    createBranchPreviewCard(branch);
                    activeBranches++;
                }
            }
            
            if (activeBranches == 0) {
                // No active branches - use preset card style
                View noBranchCard = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_branch_card, branchPreviewLayout, false);
                TextView branchName = noBranchCard.findViewById(R.id.tvBranchName);
                TextView branchAddress = noBranchCard.findViewById(R.id.tvBranchAddress);
                TextView managerCount = noBranchCard.findViewById(R.id.tvManagerCount);
                TextView employeeCount = noBranchCard.findViewById(R.id.tvEmployeeCount);
                
                branchName.setText("NO ACTIVE BRANCHES");
                branchAddress.setText("No branches with employees yet");
                managerCount.setText("0 Managers");
                employeeCount.setText("0 Employees");
                
                branchPreviewLayout.addView(noBranchCard);
            }
        }
    }

    private void createBranchPreviewCard(app.hub.api.BranchResponse.Branch branch) {
        // Inflate the preset card layout
        View branchCard = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_branch_card, branchPreviewLayout, false);
        
        // Get references to the card's views
        TextView branchName = branchCard.findViewById(R.id.tvBranchName);
        TextView branchAddress = branchCard.findViewById(R.id.tvBranchAddress);
        TextView managerCount = branchCard.findViewById(R.id.tvManagerCount);
        TextView employeeCount = branchCard.findViewById(R.id.tvEmployeeCount);
        
        // Populate with branch data
        String displayName = branch.getName().replace("ASHCOL - ", "").replace("ASHCOL ", "");
        branchName.setText(displayName.toUpperCase());
        
        String address = branch.getAddress() != null ? branch.getAddress() : branch.getLocation();
        branchAddress.setText(address);
        
        // Manager count
        int managers = !"No manager assigned".equals(branch.getManager()) ? 1 : 0;
        managerCount.setText(managers + " Manager" + (managers != 1 ? "s" : ""));
        
        // Employee count
        int employees = branch.getEmployeeCount();
        employeeCount.setText(employees + " Employee" + (employees != 1 ? "s" : ""));
        
        // Add click listener to navigate to branch details
        branchCard.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(getContext(), BranchDetailActivity.class);
                intent.putExtra("branch_id", branch.getId());
                intent.putExtra("branch_name", branch.getName());
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error opening branch details", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
        
        branchPreviewLayout.addView(branchCard);
    }

    private void loadEmployeesForManagerPreview(String token) {
        // Load employees for manager preview
        ApiService apiService = ApiClient.getApiService();
        Call<EmployeeResponse> call = apiService.getEmployees("Bearer " + token);
        
        call.enqueue(new Callback<EmployeeResponse>() {
            @Override
            public void onResponse(Call<EmployeeResponse> call, Response<EmployeeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EmployeeResponse employeeResponse = response.body();
                    
                    if (employeeResponse.isSuccess()) {
                        updateManagersPreview(employeeResponse.getEmployees());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<EmployeeResponse> call, Throwable t) {
                android.util.Log.e("AdminHome", "Employees API call failed: " + t.getMessage(), t);
            }
        });
    }

    private void showErrorInBranchPreview(String errorMessage) {
        if (branchPreviewLayout != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                branchPreviewLayout.removeAllViews();
                
                // Use preset card style for error message
                View errorCard = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_branch_card, branchPreviewLayout, false);
                TextView branchName = errorCard.findViewById(R.id.tvBranchName);
                TextView branchAddress = errorCard.findViewById(R.id.tvBranchAddress);
                TextView managerCount = errorCard.findViewById(R.id.tvManagerCount);
                TextView employeeCount = errorCard.findViewById(R.id.tvEmployeeCount);
                
                branchName.setText("ERROR");
                branchName.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                branchAddress.setText(errorMessage);
                managerCount.setText("Unable to load");
                employeeCount.setText("data");
                
                branchPreviewLayout.addView(errorCard);
            });
        }
    }

    private void updateManagersPreview(List<EmployeeResponse.Employee> employees) {
        if (managersRecyclerView != null) {
            List<ManagersActivity.Manager> managers = new ArrayList<>();
            
            // Get first 3 managers for preview
            int count = 0;
            for (EmployeeResponse.Employee employee : employees) {
                if ("manager".equalsIgnoreCase(employee.getRole()) && count < 3) {
                    int userId = employee.getId();
                    String fullName = employee.getFirstName() + " " + employee.getLastName();
                    String branch = employee.getBranch() != null ? employee.getBranch() : "No branch assigned";
                    String email = employee.getEmail() != null ? employee.getEmail() : "No email";
                    String status = "Active";
                    String phone = "+63 9XX XXX XXXX";
                    String joinDate = "N/A";
                    
                    managers.add(new ManagersActivity.Manager(userId, fullName, branch, email, status, phone, joinDate));
                    count++;
                }
            }
            
            ManagersAdapter adapter = new ManagersAdapter(managers, null);
            managersRecyclerView.setAdapter(adapter);
        }
    }
}
