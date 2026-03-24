package app.hub.manager;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.util.TokenManager;

public class ManagerEmployeeFragment extends Fragment implements ManagerDataManager.EmployeeDataChangeListener {

    private static final String TAG = "ManagerEmployeeFragment";

    private TextView locationTitle, employeeCount;
    private RecyclerView rvEmployees;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TokenManager tokenManager;
    private EmployeeAdapter employeeAdapter;
    private List<EmployeeResponse.Employee> employeeList;

    public ManagerEmployeeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_employee, container, false);

        tokenManager = new TokenManager(getContext());
        employeeList = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupButtons(view);

        // Register as listener for employee data changes
        ManagerDataManager.registerEmployeeListener(this);

        // Display data immediately if available
        displayEmployeeData();

        return view;
    }

    private void initializeViews(View view) {
        locationTitle = view.findViewById(R.id.locationTitle);
        employeeCount = view.findViewById(R.id.employeeCount);
        rvEmployees = view.findViewById(R.id.rvEmployees);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshEmployees);
    }

    private void setupRecyclerView() {
        employeeAdapter = new EmployeeAdapter(employeeList);
        rvEmployees.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEmployees.setAdapter(employeeAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.green,
                    R.color.blue,
                    R.color.orange);

            swipeRefreshLayout.setOnRefreshListener(() -> {
                android.util.Log.d(TAG, "Pull-to-refresh triggered");
                refreshEmployeeData();
            });
        }
    }

    private void refreshEmployeeData() {
        if (getContext() == null) return;

        ManagerDataManager.loadAllData(getContext(), new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateEmployeeUI(branchName, employees);
                    });
                }
            }

            @Override
            public void onTicketsLoaded(List<app.hub.api.TicketListResponse.TicketItem> tickets) {}

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats, List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {}

            @Override
            public void onLoadComplete() {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onLoadError(String error) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (getContext() != null) Toast.makeText(getContext(), "Failed to refresh: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtons(View view) {
        MaterialButton btnAddEmployee = view.findViewById(R.id.btnAddEmployee);
        if (btnAddEmployee != null) {
            btnAddEmployee.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ManagerAddEmployee.class);
                startActivity(intent);
            });
        }
    }

    private void displayEmployeeData() {
        if (!isFragmentReady()) {
            return;
        }

        // Get data from centralized manager
        String branchName = ManagerDataManager.getCachedBranchName();
        List<EmployeeResponse.Employee> employees = ManagerDataManager.getCachedEmployees();

        if (branchName != null && employees != null) {
            // Display cached data immediately
            locationTitle.setText(branchName);
            employeeCount.setText(employees.size() + " Technician" + (employees.size() != 1 ? "s" : ""));

            employeeList.clear();
            employeeList.addAll(employees);
            employeeAdapter.notifyDataSetChanged();

            android.util.Log.d("ManagerEmployee",
                    "Displayed cached data: " + branchName + " with " + employees.size() + " employees");
        } else {
            // No data available yet
            locationTitle.setText("Loading...");
            employeeCount.setText("Loading...");
            android.util.Log.d("ManagerEmployee", "No cached data available");
        }
    }

    private boolean isFragmentReady() {
        return isAdded() && getContext() != null &&
                locationTitle != null && employeeCount != null &&
                employeeList != null && employeeAdapter != null;
    }

    @Override
    public void onEmployeeDataChanged(String branchName, List<EmployeeResponse.Employee> employees) {
        // This is called when ManagerDataManager notifies of data changes
        android.util.Log.d(TAG, "Employee data changed notification received");

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateEmployeeUI(branchName, employees);
            });
        }
    }

    private void updateEmployeeUI(String branchName, List<EmployeeResponse.Employee> employees) {
        if (!isFragmentReady())
            return;

        locationTitle.setText(branchName);
        employeeCount.setText(employees.size() + " Technician" + (employees.size() != 1 ? "s" : ""));

        employeeList.clear();
        employeeList.addAll(employees);
        employeeAdapter.notifyDataSetChanged();

        android.util.Log.d(TAG, "UI updated with " + employees.size() + " employees");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Always display latest data when fragment becomes visible
        displayEmployeeData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister listener to prevent memory leaks
        ManagerDataManager.unregisterEmployeeListener(this);
        android.util.Log.d(TAG, "Fragment destroyed, listener unregistered");
    }

    /**
     * Only call this when you actually add a new employee
     */
    public static void clearCache() {
        ManagerDataManager.clearEmployeeCache();
    }
}
