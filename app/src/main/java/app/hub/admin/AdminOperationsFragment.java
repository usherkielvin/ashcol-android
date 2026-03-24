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
import app.hub.util.TokenManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        if (!isAdded()) return;
        
        swipeRefresh.setRefreshing(true);
        android.util.Log.d(TAG, "Loading branches from Firestore...");
        
        FirebaseFirestore.getInstance().collection("branches").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                
                branches.clear();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String name = doc.getString("name");
                    String manager = doc.getString("manager");
                    Long countLong = doc.getLong("employeeCount");
                    int employeeCount = countLong != null ? countLong.intValue() : 0;
                    String description = doc.getString("description");
                    
                    branches.add(new BranchesActivity.Branch(name, manager, employeeCount, description));
                }
                
                if (branchesAdapter == null) {
                    branchesAdapter = new BranchesAdapter(branches, this::onBranchClick);
                    rvOperations.setAdapter(branchesAdapter);
                } else {
                    branchesAdapter.notifyDataSetChanged();
                }
                android.util.Log.d(TAG, "Loaded " + branches.size() + " branches from Firestore");
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                android.util.Log.e(TAG, "Error loading branches: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to load branches", Toast.LENGTH_SHORT).show();
            });
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
        if (!isAdded()) return;
        
        swipeRefresh.setRefreshing(true);
        android.util.Log.d(TAG, "Loading managers from Firestore...");
        
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("role", "manager")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                
                managers.clear();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String firstName = doc.getString("first_name");
                    String lastName = doc.getString("last_name");
                    String name = firstName != null ? firstName + " " + lastName : doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String branch = doc.getString("branch");
                    String profilePhoto = doc.getString("profile_photo");
                    
                    managers.add(new ManagersActivity.Manager(name, email, phone, branch, profilePhoto));
                }
                
                if (managersAdapter == null) {
                    managersAdapter = new ManagersAdapter(managers, this::onManagerClick);
                    managersAdapter.setOnManagerActionListener(new ManagersAdapter.OnManagerActionListener() {
                        @Override
                        public void onEditManager(ManagersActivity.Manager manager) {
                            openManagerEditActivity(manager);
                        }

                        @Override
                        public void onRemoveManager(ManagersActivity.Manager manager, int position) {
                            // Assuming deleteManager(String uid) is used for Firestore deletions
                        }
                    });
                    rvOperations.setAdapter(managersAdapter);
                } else {
                    managersAdapter.notifyDataSetChanged();
                }
                android.util.Log.d(TAG, "Loaded " + managers.size() + " managers from Firestore");
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                android.util.Log.e(TAG, "Error loading managers: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to load managers", Toast.LENGTH_SHORT).show();
            });
    }

    private void openManagerEditActivity(ManagersActivity.Manager manager) {
        Intent intent = new Intent(getActivity(), AdminEditManagerActivity.class);
        intent.putExtra("manager_name", manager.getName());
        intent.putExtra("manager_branch", manager.getBranch());
        intent.putExtra("manager_email", manager.getEmail());
        intent.putExtra("manager_status", manager.getStatus());
        startActivity(intent);
    }

    private void deleteManager(int userId) {
        // Since we are using Firestore, deletion should be from Firestore
        // and ideally from Firebase Auth (via Cloud Function)
        // For now, just delete from Firestore
        // Note: userId is no longer an int, but a String (UID)
        // I'll skip this implementation for now or use a String parameter
    }

    private void deleteManager(String uid) {
        if (!isAdded()) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Manager")
            .setMessage("Are you sure you want to delete this manager?")
            .setPositiveButton("Delete", (dialog, which) -> {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Manager deleted successfully", Toast.LENGTH_SHORT).show();
                        loadManagers();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to delete manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
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
