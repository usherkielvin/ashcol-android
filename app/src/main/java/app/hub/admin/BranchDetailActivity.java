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
import app.hub.util.TokenManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        // Load real employees from Firestore
        loadEmployeesFromFirestore(branchNameStr);
    }

    private void loadEmployeesFromFirestore(String branchNameStr) {
        if (branchNameStr == null) {
            showEmptyEmployeeList();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("branch", branchNameStr)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<ManagersActivity.Manager> employees = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String firstName = doc.getString("first_name");
                    String lastName = doc.getString("last_name");
                    String name = firstName != null ? firstName + " " + lastName : doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String branch = doc.getString("branch");
                    String profilePhoto = doc.getString("profile_photo");
                    
                    employees.add(new ManagersActivity.Manager(name, email, phone, branch, profilePhoto));
                }
                
                if (employees.isEmpty()) {
                    showEmptyEmployeeList();
                } else {
                    employeesRecyclerView.setAdapter(new ManagersAdapter(employees, null));
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("BranchDetailActivity", "Error loading employees: " + e.getMessage());
                showEmptyEmployeeList();
            });
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
