package app.hub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.BranchReportsResponse;
import app.hub.util.TokenManager;

public class AdminReportsFragment extends Fragment {

    private RecyclerView rvBranches;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    
    private BranchReportsAdapter adapter;
    private List<BranchReportsResponse.BranchReport> branchList;
    private TokenManager tokenManager;

    public AdminReportsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);
            
            tokenManager = new TokenManager(requireContext());
            
            initViews(view);
            setupRecyclerView();
            loadBranchReports();
            
            return view;
        } catch (Exception e) {
            Log.e("AdminReportsFragment", "Error in onCreateView: " + e.getMessage(), e);
            return null;
        }
    }

    private void initViews(View view) {
        rvBranches = view.findViewById(R.id.rvBranches);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        swipeRefreshLayout.setOnRefreshListener(this::loadBranchReports);
    }

    private void setupRecyclerView() {
        branchList = new ArrayList<>();
        adapter = new BranchReportsAdapter(branchList, this::onBranchClick);
        rvBranches.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBranches.setAdapter(adapter);
    }

    private void loadBranchReports() {
        showLoading(true);
        tvEmptyState.setVisibility(View.GONE);

        String token = tokenManager.getToken();
        if (token == null) {
            showError("Authentication required");
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<BranchReportsResponse> call = apiService.getBranchReports("Bearer " + token);

        call.enqueue(new Callback<BranchReportsResponse>() {
            @Override
            public void onResponse(Call<BranchReportsResponse> call, Response<BranchReportsResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    BranchReportsResponse branchResponse = response.body();
                    
                    if (branchResponse.isSuccess()) {
                        List<BranchReportsResponse.BranchReport> branches = branchResponse.getBranches();
                        
                        if (branches != null && !branches.isEmpty()) {
                            branchList.clear();
                            branchList.addAll(branches);
                            adapter.updateData(branchList);
                            tvEmptyState.setVisibility(View.GONE);
                            rvBranches.setVisibility(View.VISIBLE);
                        } else {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvBranches.setVisibility(View.GONE);
                        }
                    } else {
                        showError(branchResponse.getMessage());
                    }
                } else {
                    showError("Failed to load branch reports");
                }
            }

            @Override
            public void onFailure(Call<BranchReportsResponse> call, Throwable t) {
                showLoading(false);
                Log.e("AdminReportsFragment", "API call failed: " + t.getMessage(), t);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void onBranchClick(BranchReportsResponse.BranchReport branch) {
        Intent intent = new Intent(requireContext(), BranchReportDetailActivity.class);
        intent.putExtra("branch_id", branch.getId());
        intent.putExtra("branch_name", branch.getName());
        intent.putExtra("branch_location", branch.getLocation());
        startActivity(intent);
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
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
