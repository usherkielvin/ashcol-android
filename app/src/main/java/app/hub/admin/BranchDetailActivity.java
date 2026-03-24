package app.hub.admin;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.util.TokenManager;

public class BranchDetailActivity extends AppCompatActivity {

    private TextView branchName;
    private TextView branchDescription;
    private TextView managerName;
    private TextView managerEmail;
    private TextView employeeCount;
    private RecyclerView employeesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_detail);

        setupToolbar();
        initViews();
        loadBranchData();
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

    private void initViews() {
        branchName = findViewById(R.id.branchName);
        branchDescription = findViewById(R.id.branchDescription);
        managerName = findViewById(R.id.managerName);
        managerEmail = findViewById(R.id.managerEmail);
        employeeCount = findViewById(R.id.employeeCount);
        employeesRecyclerView = findViewById(R.id.employeesRecyclerView);
        
        employeesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadBranchData() {
        // Get data from intent
        String branchNameStr = getIntent().getStringExtra("branch_name");
        String branchManager = getIntent().getStringExtra("branch_manager");
        int empCount = getIntent().getIntExtra("employee_count", 0);
        String branchDesc = getIntent().getStringExtra("branch_description");

        // Set branch info
        branchName.setText(branchNameStr != null ? branchNameStr : "Unknown Branch");
        branchDescription.setText(branchDesc != null ? branchDesc : "No description available.");
        managerName.setText(branchManager != null ? branchManager : "No Manager Assigned");
        managerEmail.setText(branchManager != null ? branchManager.toLowerCase().replace(" ", ".") + "@ashcol.com" : "");
        employeeCount.setText(empCount + " Total");

        // Load real employees from API
        loadEmployeesFromAPI(branchNameStr);
    }

    private void loadEmployeesFromAPI(String branchNameStr) {
        if (branchNameStr == null) {
            showEmptyEmployeeList();
            return;
        }

        TokenManager tokenManager = new TokenManager(this);
        String token = tokenManager.getToken();
        
        if (token == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            showEmptyEmployeeList();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        Call<EmployeeResponse> call = apiService.getEmployeesByBranch("Bearer " + token, branchNameStr);
        
        call.enqueue(new Callback<EmployeeResponse>() {
            @Override
            public void onResponse(Call<EmployeeResponse> call, Response<EmployeeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EmployeeResponse employeeResponse = response.body();
                    
                    if (employeeResponse.isSuccess()) {
                        List<Employee> employees = convertToEmployeeList(employeeResponse.getEmployees());
                        displayEmployees(employees);
                    } else {
                        android.util.Log.e("BranchDetail", "API returned success=false: " + employeeResponse.getMessage());
                        showEmptyEmployeeList();
                    }
                } else {
                    android.util.Log.e("BranchDetail", "API response not successful: " + response.code());
                    showEmptyEmployeeList();
                }
            }
            
            @Override
            public void onFailure(Call<EmployeeResponse> call, Throwable t) {
                android.util.Log.e("BranchDetail", "API call failed: " + t.getMessage(), t);
                Toast.makeText(BranchDetailActivity.this, "Failed to load employees", Toast.LENGTH_SHORT).show();
                showEmptyEmployeeList();
            }
        });
    }

    private List<Employee> convertToEmployeeList(List<EmployeeResponse.Employee> apiEmployees) {
        List<Employee> employees = new ArrayList<>();
        
        for (EmployeeResponse.Employee apiEmployee : apiEmployees) {
            String fullName = (apiEmployee.getFirstName() != null ? apiEmployee.getFirstName() : "") + 
                             " " + (apiEmployee.getLastName() != null ? apiEmployee.getLastName() : "");
            fullName = fullName.trim();
            
            if (fullName.isEmpty()) {
                fullName = apiEmployee.getUsername() != null ? apiEmployee.getUsername() : "Employee #" + apiEmployee.getId();
            }
            
            String role = apiEmployee.getRole() != null ? 
                         capitalizeFirst(apiEmployee.getRole()) : "Employee";
            
            employees.add(new Employee(fullName, role));
        }
        
        return employees;
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void displayEmployees(List<Employee> employees) {
        EmployeesAdapter adapter = new EmployeesAdapter(employees);
        employeesRecyclerView.setAdapter(adapter);
    }

    private void showEmptyEmployeeList() {
        List<Employee> emptyList = new ArrayList<>();
        emptyList.add(new Employee("No employees found", ""));
        EmployeesAdapter adapter = new EmployeesAdapter(emptyList);
        employeesRecyclerView.setAdapter(adapter);
    }

    // Employee data class
    public static class Employee {
        private String name;
        private String role;

        public Employee(String name, String role) {
            this.name = name;
            this.role = role;
        }

        public String getName() { return name; }
        public String getRole() { return role; }
    }
}
