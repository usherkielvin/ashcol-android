package app.hub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.BranchResponse;
import app.hub.api.DeleteAccountResponse;
import app.hub.api.EmployeeResponse;
import app.hub.util.TokenManager;

public class AdminOperationsFragment extends Fragment {
    private static final String TAG = "AdminOperationsFragment";
    private static final int REQUEST_ADD_MANAGER = 1001;
    private static final String ARG_SHOW_MANAGER_TAB = "show_manager_tab";
    
    private LinearLayout tabBranch, tabManager;
    private TextView tvTabBranch, tvTabManager, tvHeaderTitle;
    private View viewTabBranchUnderline, viewTabManagerUnderline;
    private RecyclerView rvOperations;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialButton btnAddManager;
    private View actionButtonContainer;
    
    private TokenManager tokenManager;
    private BranchesAdapter branchesAdapter;
    private ManagersAdapter managersAdapter;
    private List<BranchesActivity.Branch> branches = new ArrayList<>();
    private List<ManagersActivity.Manager> managers = new ArrayList<>();
    
    private boolean isManagerTabActive = false;

    /**
     * Create a new instance of AdminOperationsFragment
     * @param showManagerTab true to show Manager tab, false to show Branch tab
     */
    public static AdminOperationsFragment newInstance(boolean showManagerTab) {
        AdminOperationsFragment fragment = new AdminOperationsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_MANAGER_TAB, showManagerTab);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_operations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tokenManager = new TokenManager(getContext());
        initializeViews(view);
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        setupAddManagerButton();
        
        // Check if we should show manager tab from arguments
        if (getArguments() != null && getArguments().getBoolean(ARG_SHOW_MANAGER_TAB, false)) {
            isManagerTabActive = true;
            updateTabUI();
            loadManagers();
        } else {
            // Load branches by default
            loadBranches();
        }
    }

    private void initializeViews(View view) {
        tabBranch = view.findViewById(R.id.tabBranch);
        tabManager = view.findViewById(R.id.tabManager);
        tvTabBranch = view.findViewById(R.id.tvTabBranch);
        tvTabManager = view.findViewById(R.id.tvTabManager);
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        viewTabBranchUnderline = view.findViewById(R.id.viewTabBranchUnderline);
        viewTabManagerUnderline = view.findViewById(R.id.viewTabManagerUnderline);
        rvOperations = view.findViewById(R.id.rvOperations);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        btnAddManager = view.findViewById(R.id.btnAddManager);
        actionButtonContainer = view.findViewById(R.id.actionButtonContainer);
    }

    private void setupTabs() {
        tabBranch.setOnClickListener(v -> switchToBranchTab());
        tabManager.setOnClickListener(v -> switchToManagerTab());
    }

    private void setupRecyclerView() {
        rvOperations.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            if (isManagerTabActive) {
                loadManagers();
            } else {
                loadBranches();
            }
        });
    }

    private void setupAddManagerButton() {
        btnAddManager.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdminAddManager.class);
            startActivityForResult(intent, REQUEST_ADD_MANAGER);
        });
    }

    private void switchToBranchTab() {
        if (isManagerTabActive) {
            isManagerTabActive = false;
            updateTabUI();
            // Switch adapter immediately
            if (branchesAdapter != null) {
                rvOperations.setAdapter(branchesAdapter);
            }
            loadBranches();
        }
    }

    private void switchToManagerTab() {
        if (!isManagerTabActive) {
            isManagerTabActive = true;
            updateTabUI();
            // Switch adapter immediately
            if (managersAdapter != null) {
                rvOperations.setAdapter(managersAdapter);
            }
            loadManagers();
        }
    }

    private void updateTabUI() {
        if (getActivity() == null) return;
        
        if (isManagerTabActive) {
            // Manager tab active
            tvHeaderTitle.setText("Manager");
            tvTabManager.setTextColor(getResources().getColor(R.color.apps_green));
            tvTabManager.setTypeface(null, android.graphics.Typeface.BOLD);
            viewTabManagerUnderline.setBackgroundColor(getResources().getColor(R.color.apps_green));
            viewTabManagerUnderline.setVisibility(View.VISIBLE);
            
            tvTabBranch.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTabBranch.setTypeface(null, android.graphics.Typeface.NORMAL);
            viewTabBranchUnderline.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            viewTabBranchUnderline.setVisibility(View.INVISIBLE);
            
            btnAddManager.setVisibility(View.VISIBLE);
            actionButtonContainer.setVisibility(View.VISIBLE);
        } else {
            // Branch tab active
            tvHeaderTitle.setText("Branch");
            tvTabBranch.setTextColor(getResources().getColor(R.color.apps_green));
            tvTabBranch.setTypeface(null, android.graphics.Typeface.BOLD);
            viewTabBranchUnderline.setBackgroundColor(getResources().getColor(R.color.apps_green));
            viewTabBranchUnderline.setVisibility(View.VISIBLE);
            
            tvTabManager.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTabManager.setTypeface(null, android.graphics.Typeface.NORMAL);
            viewTabManagerUnderline.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            viewTabManagerUnderline.setVisibility(View.INVISIBLE);
            
            btnAddManager.setVisibility(View.GONE);
            actionButtonContainer.setVisibility(View.GONE);
        }
    }

    private void loadBranches() {
        swipeRefresh.setRefreshing(true);
        
        String token = tokenManager.getToken();
        if (token == null) {
            Log.e(TAG, "No token available");
            Toast.makeText(getContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<BranchResponse> call = apiService.getBranches("Bearer " + token);
        
        call.enqueue(new Callback<BranchResponse>() {
            @Override
            public void onResponse(Call<BranchResponse> call, Response<BranchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BranchResponse branchResponse = response.body();
                    
                    if (branchResponse.isSuccess()) {
                        processBranchData(branchResponse.getBranches());
                    } else {
                        Log.e(TAG, "API returned success=false: " + branchResponse.getMessage());
                        Toast.makeText(getContext(), "Failed to load branches", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API response not successful");
                    Toast.makeText(getContext(), "Failed to load branches", Toast.LENGTH_SHORT).show();
                }
                swipeRefresh.setRefreshing(false);
            }
            
            @Override
            public void onFailure(Call<BranchResponse> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void processBranchData(List<BranchResponse.Branch> apiBranches) {
        branches.clear();
        
        // Convert API branch data to local Branch objects
        for (BranchResponse.Branch apiBranch : apiBranches) {
            branches.add(new BranchesActivity.Branch(
                apiBranch.getName(),
                apiBranch.getManager(),
                apiBranch.getEmployeeCount(),
                apiBranch.getDescription()
            ));
        }
        
        // Update UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (branchesAdapter == null) {
                    branchesAdapter = new BranchesAdapter(branches, this::onBranchClick);
                    rvOperations.setAdapter(branchesAdapter);
                } else {
                    branchesAdapter.notifyDataSetChanged();
                }
                Log.d(TAG, "Loaded " + branches.size() + " branches");
            });
        }
    }

    private void onBranchClick(BranchesActivity.Branch branch) {
        Intent intent = new Intent(getActivity(), BranchDetailActivity.class);
        intent.putExtra("branch_name", branch.getName());
        intent.putExtra("branch_manager", branch.getManager());
        intent.putExtra("employee_count", branch.getEmployeeCount());
        intent.putExtra("branch_description", branch.getDescription());
        startActivity(intent);
    }

    private void loadManagers() {
        swipeRefresh.setRefreshing(true);
        
        String token = tokenManager.getToken();
        if (token == null) {
            Log.e(TAG, "No token available");
            Toast.makeText(getContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<EmployeeResponse> call = apiService.getEmployees("Bearer " + token);
        
        call.enqueue(new Callback<EmployeeResponse>() {
            @Override
            public void onResponse(Call<EmployeeResponse> call, Response<EmployeeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EmployeeResponse employeeResponse = response.body();
                    
                    if (employeeResponse.isSuccess()) {
                        processManagerData(employeeResponse.getEmployees());
                    } else {
                        Log.e(TAG, "API returned success=false");
                        Toast.makeText(getContext(), "Failed to load managers", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API response not successful");
                    Toast.makeText(getContext(), "Failed to load managers", Toast.LENGTH_SHORT).show();
                }
                swipeRefresh.setRefreshing(false);
            }
            
            @Override
            public void onFailure(Call<EmployeeResponse> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void processManagerData(List<EmployeeResponse.Employee> employees) {
        managers.clear();
        
        // Filter only managers
        for (EmployeeResponse.Employee employee : employees) {
            if ("manager".equalsIgnoreCase(employee.getRole())) {
                int userId = employee.getId();
                String fullName = employee.getFirstName() + " " + employee.getLastName();
                String branch = employee.getBranch() != null ? employee.getBranch() : "No branch assigned";
                String email = employee.getEmail() != null ? employee.getEmail() : "No email";
                String status = "Active";
                String phone = "+63 9XX XXX XXXX";
                String joinDate = "N/A";
                
                managers.add(new ManagersActivity.Manager(userId, fullName, branch, email, status, phone, joinDate));
            }
        }
        
        // Update UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (managersAdapter == null) {
                    managersAdapter = new ManagersAdapter(managers, this::onManagerClick);
                    managersAdapter.setOnManagerActionListener(new ManagersAdapter.OnManagerActionListener() {
                        @Override
                        public void onEditManager(ManagersActivity.Manager manager) {
                            openManagerEditActivity(manager);
                        }

                        @Override
                        public void onRemoveManager(ManagersActivity.Manager manager, int position) {
                            removeManager(manager, position);
                        }
                    });
                    rvOperations.setAdapter(managersAdapter);
                } else {
                    managersAdapter.notifyDataSetChanged();
                }
                Log.d(TAG, "Loaded " + managers.size() + " managers");
            });
        }
    }

    private void openManagerEditActivity(ManagersActivity.Manager manager) {
        Intent intent = new Intent(getActivity(), AdminEditManagerActivity.class);
        intent.putExtra("manager_name", manager.getName());
        intent.putExtra("manager_branch", manager.getBranch());
        intent.putExtra("manager_email", manager.getEmail());
        intent.putExtra("manager_status", manager.getStatus());
        startActivity(intent);
    }

    private void removeManager(ManagersActivity.Manager manager, int position) {
        // Show loading
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Removing " + manager.getName() + "...", Toast.LENGTH_SHORT).show();
            });
        }

        String token = tokenManager.getToken();
        if (token == null) {
            Log.e(TAG, "No token available");
            Toast.makeText(getContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = manager.getId();
        ApiService apiService = ApiClient.getApiService();
        Call<DeleteAccountResponse> call = apiService.adminDeleteUser("Bearer " + token, userId);
        
        call.enqueue(new Callback<DeleteAccountResponse>() {
            @Override
            public void onResponse(Call<DeleteAccountResponse> call, Response<DeleteAccountResponse> response) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        DeleteAccountResponse deleteResponse = response.body();
                        
                        if (deleteResponse.isSuccess()) {
                            // Remove from local list
                            managers.remove(position);
                            if (managersAdapter != null) {
                                managersAdapter.notifyItemRemoved(position);
                                managersAdapter.notifyItemRangeChanged(position, managers.size());
                            }
                            Toast.makeText(getContext(), manager.getName() + " removed successfully", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Manager deleted successfully: " + manager.getName());
                        } else {
                            Toast.makeText(getContext(), "Failed to remove manager: " + deleteResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Delete failed: " + deleteResponse.getMessage());
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to remove manager", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Delete response not successful: " + response.code());
                    }
                });
            }
            
            @Override
            public void onFailure(Call<DeleteAccountResponse> call, Throwable t) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete API call failed: " + t.getMessage(), t);
                });
            }
        });
    }

    private void onManagerClick(ManagersActivity.Manager manager) {
        Intent intent = new Intent(getActivity(), ManagerDetailActivity.class);
        intent.putExtra("manager_name", manager.getName());
        intent.putExtra("manager_branch", manager.getBranch());
        intent.putExtra("manager_email", manager.getEmail());
        intent.putExtra("manager_status", manager.getStatus());
        intent.putExtra("manager_phone", manager.getPhone());
        intent.putExtra("manager_join_date", manager.getJoinDate());
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_MANAGER && resultCode == getActivity().RESULT_OK) {
            // Refresh managers list
            loadManagers();
        }
    }
}
