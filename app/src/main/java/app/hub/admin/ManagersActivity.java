package app.hub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.util.TokenManager;

public class ManagersActivity extends AppCompatActivity {

    private static final int ADD_MANAGER_REQUEST_CODE = 1001;
    
    private RecyclerView managersRecyclerView;
    private ManagersAdapter managersAdapter;
    private List<Manager> managerList;
    private FloatingActionButton fabAddManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_managers);
            setupToolbar();
            setupRecyclerView();
            setupFAB();
            loadManagers();
        } catch (Exception e) {
            android.util.Log.e("ManagersActivity", "Error in onCreate: " + e.getMessage(), e);
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
        managersRecyclerView = findViewById(R.id.managersRecyclerView);
        managersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        managerList = new ArrayList<>();
        managersAdapter = new ManagersAdapter(managerList, this::onManagerClick);
        managersRecyclerView.setAdapter(managersAdapter);
    }

    private void setupFAB() {
        fabAddManager = findViewById(R.id.fabAddManager);
        fabAddManager.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminAddManager.class);
            startActivityForResult(intent, ADD_MANAGER_REQUEST_CODE);
        });
    }

    private void loadManagers() {
        android.util.Log.d("ManagersActivity", "Loading managers from API...");
        
        TokenManager tokenManager = new TokenManager(this);
        String token = tokenManager.getToken();
        
        if (token == null) {
            android.util.Log.e("ManagersActivity", "No token available");
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
                        android.util.Log.e("ManagersActivity", "API returned success=false: " + employeeResponse.getMessage());
                    }
                } else {
                    android.util.Log.e("ManagersActivity", "API response not successful");
                }
            }
            
            @Override
            public void onFailure(Call<EmployeeResponse> call, Throwable t) {
                android.util.Log.e("ManagersActivity", "API call failed: " + t.getMessage(), t);
            }
        });
    }

    private void processManagerData(List<EmployeeResponse.Employee> employees) {
        managerList.clear();
        
        // Filter employees to get only managers
        for (EmployeeResponse.Employee employee : employees) {
            if ("manager".equalsIgnoreCase(employee.getRole())) {
                int userId = employee.getId();
                String fullName = employee.getFirstName() + " " + employee.getLastName();
                String branch = employee.getBranch() != null ? employee.getBranch() : "No branch assigned";
                String email = employee.getEmail() != null ? employee.getEmail() : "No email";
                String status = "Active"; // Default status
                String phone = "+63 9XX XXX XXXX"; // Placeholder phone
                String joinDate = "N/A"; // Placeholder join date
                
                managerList.add(new Manager(userId, fullName, branch, email, status, phone, joinDate));
            }
        }
        
        // Update UI on main thread
        runOnUiThread(() -> {
            managersAdapter.notifyDataSetChanged();
            android.util.Log.d("ManagersActivity", "Loaded " + managerList.size() + " managers");
        });
    }

    private void onManagerClick(Manager manager) {
        android.content.Intent intent = new android.content.Intent(this, ManagerDetailActivity.class);
        intent.putExtra("manager_name", manager.getName());
        intent.putExtra("manager_branch", manager.getBranch());
        intent.putExtra("manager_email", manager.getEmail());
        intent.putExtra("manager_status", manager.getStatus());
        intent.putExtra("manager_phone", manager.getPhone());
        intent.putExtra("manager_join_date", manager.getJoinDate());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ADD_MANAGER_REQUEST_CODE && resultCode == RESULT_OK) {
            // Refresh the managers list when a new manager is added
            loadManagers();
            Toast.makeText(this, "Manager list refreshed", Toast.LENGTH_SHORT).show();
        }
    }

    // Manager data class
    public static class Manager {
        private int id;
        private String name;
        private String branch;
        private String email;
        private String status;
        private String phone;
        private String joinDate;

        public Manager(int id, String name, String branch, String email, String status, String phone, String joinDate) {
            this.id = id;
            this.name = name;
            this.branch = branch;
            this.email = email;
            this.status = status;
            this.phone = phone;
            this.joinDate = joinDate;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getBranch() { return branch; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getPhone() { return phone; }
        public String getJoinDate() { return joinDate; }
    }
}
