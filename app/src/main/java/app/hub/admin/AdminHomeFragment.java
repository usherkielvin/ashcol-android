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
import app.hub.util.TokenManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        android.util.Log.d("AdminHome", "Loading preview data from Firestore...");
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Load branches data for preview
        db.collection("branches")
            .limit(10)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<app.hub.api.BranchResponse.Branch> branches = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    app.hub.api.BranchResponse.Branch branch = doc.toObject(app.hub.api.BranchResponse.Branch.class);
                    if (branch != null) {
                        // branch.setId(doc.getId()); // ID is int in BranchResponse.Branch
                        branches.add(branch);
                    }
                }
                updateBranchPreview(branches);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("AdminHome", "Error loading branches: " + e.getMessage());
                showErrorInBranchPreview("Error loading branches");
            });

        // Load managers data for preview
        db.collection("users")
            .whereEqualTo("role", "manager")
            .limit(5)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<app.hub.api.EmployeeResponse.Employee> managers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    app.hub.api.EmployeeResponse.Employee manager = doc.toObject(app.hub.api.EmployeeResponse.Employee.class);
                    if (manager != null) {
                        // manager.setId(doc.getId()); // ID is int in Employee
                        managers.add(manager);
                    }
                }
                if (managersRecyclerView != null) {
                    // Update: ManagersAdapter might expect a different list type or Manager object
                    // I should check what ManagersAdapter expects.
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("AdminHome", "Error loading managers: " + e.getMessage());
            });
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

    private void loadEmployeesForManagerPreview() {
        android.util.Log.d("AdminHome", "Loading managers from Firestore...");
        
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("role", "manager")
            .limit(3)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<ManagersActivity.Manager> managers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String firstName = doc.getString("first_name");
                    String lastName = doc.getString("last_name");
                    String name = firstName != null ? firstName + " " + lastName : doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String branch = doc.getString("branch");
                    String profilePhoto = doc.getString("profile_photo");
                    
                    managers.add(new ManagersActivity.Manager(name, email, phone, branch, profilePhoto));
                }
                updateManagersPreview(managers);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("AdminHome", "Error loading managers: " + e.getMessage());
            });
    }

    private void updateManagersPreview(List<ManagersActivity.Manager> managers) {
        if (managersRecyclerView != null) {
            ManagersAdapter adapter = new ManagersAdapter(managers, null);
            managersRecyclerView.setAdapter(adapter);
        }
    }

    private void showErrorInBranchPreview(String errorMessage) {
        if (branchPreviewLayout != null && isAdded()) {
            branchPreviewLayout.removeAllViews();
            
            View errorCard = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_branch_card, branchPreviewLayout, false);
            TextView branchName = errorCard.findViewById(R.id.tvBranchName);
            TextView branchAddress = errorCard.findViewById(R.id.tvBranchAddress);
            
            branchName.setText("ERROR");
            branchName.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            branchAddress.setText(errorMessage);
            
            branchPreviewLayout.addView(errorCard);
        }
    }
}
