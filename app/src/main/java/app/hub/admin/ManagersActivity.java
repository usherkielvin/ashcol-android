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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        android.util.Log.d("ManagersActivity", "Loading managers from Firestore...");
        
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("role", "manager")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                managerList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String firstName = doc.getString("first_name");
                    String lastName = doc.getString("last_name");
                    String name = firstName != null ? firstName + " " + lastName : doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String branch = doc.getString("branch");
                    String profilePhoto = doc.getString("profile_photo");
                    
                    managerList.add(new Manager(name, email, phone, branch, profilePhoto));
                }
                
                runOnUiThread(() -> {
                    managersAdapter.notifyDataSetChanged();
                    android.util.Log.d("ManagersActivity", "Loaded " + managerList.size() + " managers from Firestore");
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("ManagersActivity", "Error loading managers: " + e.getMessage());
            });
    }

    private void processManagerData(List<EmployeeResponse.Employee> employees) {
        // No longer used
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
        private String id;
        private String name;
        private String branch;
        private String email;
        private String status;
        private String phone;
        private String joinDate;
        private String profilePhoto;

        public Manager(String id, String name, String branch, String email, String status, String phone, String joinDate, String profilePhoto) {
            this.id = id;
            this.name = name;
            this.branch = branch;
            this.email = email;
            this.status = status;
            this.phone = phone;
            this.joinDate = joinDate;
            this.profilePhoto = profilePhoto;
        }

        public Manager(String name, String email, String phone, String branch, String profilePhoto) {
            this.id = "";
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.branch = branch;
            this.profilePhoto = profilePhoto;
            this.status = "Active";
            this.joinDate = "";
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getBranch() { return branch; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getPhone() { return phone; }
        public String getJoinDate() { return joinDate; }
        public String getProfilePhoto() { return profilePhoto; }
    }
}
