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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        android.util.Log.d("BranchesActivity", "Loading branches from Firestore...");
        
        FirebaseFirestore.getInstance().collection("branches").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                branchList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String name = doc.getString("name");
                    String manager = doc.getString("manager");
                    Long countLong = doc.getLong("employeeCount");
                    int employeeCount = countLong != null ? countLong.intValue() : 0;
                    String description = doc.getString("description");
                    
                    branchList.add(new Branch(name, manager, employeeCount, description));
                }
                
                runOnUiThread(() -> {
                    branchesAdapter.notifyDataSetChanged();
                    android.util.Log.d("BranchesActivity", "Loaded " + branchList.size() + " branches from Firestore");
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("BranchesActivity", "Error loading branches: " + e.getMessage());
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
