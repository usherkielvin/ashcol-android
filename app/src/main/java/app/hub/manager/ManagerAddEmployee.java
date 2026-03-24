package app.hub.manager;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.api.RegisterRequest;
import app.hub.api.RegisterResponse;
import app.hub.api.UserResponse;
import app.hub.util.LoadingDialog;
import app.hub.util.TokenManager;

public class ManagerAddEmployee extends AppCompatActivity {

    private TextInputEditText firstNameInput, lastNameInput, emailInput, passwordInput;
    private AutoCompleteTextView roleSpinner;
    private TextView branchDisplay;
    private MaterialButton btnBack, btnCreate;
    private TokenManager tokenManager;
    private String selectedRole = "technician";
    private String selectedBranch = null;
    private String[] roles = {"technician"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_manager_add_employee);

            android.util.Log.d("ManagerAddEmployee", "Activity created");

            tokenManager = new TokenManager(this);
            initializeViews();
            setupButtons();
            loadManagerInfo();

        } catch (Exception e) {
            android.util.Log.e("ManagerAddEmployee", "Exception in onCreate", e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            android.util.Log.d("ManagerAddEmployee", "Initializing views");

            firstNameInput = findViewById(R.id.etFirstName);
            lastNameInput = findViewById(R.id.etLastName);
            emailInput = findViewById(R.id.etEmail);
            passwordInput = findViewById(R.id.etPassword);
            roleSpinner = findViewById(R.id.spinnerRole);
            branchDisplay = findViewById(R.id.tvBranchDisplay);
            btnBack = findViewById(R.id.btnBack);
            btnCreate = findViewById(R.id.btnCreate);

            // Setup role spinner (only technician)
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, roles);
            roleSpinner.setAdapter(roleAdapter);
            roleSpinner.setText("technician", false);
            roleSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedRole = roles[position];
                }
            });

            // Check if all views were found
            if (firstNameInput == null)
                android.util.Log.e("ManagerAddEmployee", "firstNameInput is null");
            if (lastNameInput == null)
                android.util.Log.e("ManagerAddEmployee", "lastNameInput is null");
            if (emailInput == null)
                android.util.Log.e("ManagerAddEmployee", "emailInput is null");
            if (passwordInput == null)
                android.util.Log.e("ManagerAddEmployee", "passwordInput is null");
            if (roleSpinner == null)
                android.util.Log.e("ManagerAddEmployee", "roleSpinner is null");
            if (branchDisplay == null)
                android.util.Log.e("ManagerAddEmployee", "branchDisplay is null");
            if (btnBack == null)
                android.util.Log.e("ManagerAddEmployee", "btnBack is null");
            if (btnCreate == null)
                android.util.Log.e("ManagerAddEmployee", "btnCreate is null");

            android.util.Log.d("ManagerAddEmployee", "Views initialized successfully");

        } catch (Exception e) {
            android.util.Log.e("ManagerAddEmployee", "Exception in initializeViews", e);
            throw e; // Re-throw to be caught by onCreate
        }
    }

    private void setupButtons() {
        try {
            android.util.Log.d("ManagerAddEmployee", "Setting up buttons");

            if (btnCreate != null) {
                btnCreate.setOnClickListener(v -> {
                    try {
                        createEmployee();
                    } catch (Exception e) {
                        android.util.Log.e("ManagerAddEmployee", "Error in createEmployee click", e);
                        Toast.makeText(ManagerAddEmployee.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                android.util.Log.e("ManagerAddEmployee", "btnCreate is null");
            }

            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    try {
                        finish();
                    } catch (Exception e) {
                        android.util.Log.e("ManagerAddEmployee", "Error in back button click", e);
                    }
                });
            } else {
                android.util.Log.e("ManagerAddEmployee", "btnBack is null");
            }

            android.util.Log.d("ManagerAddEmployee", "Buttons setup completed");

        } catch (Exception e) {
            android.util.Log.e("ManagerAddEmployee", "Exception in setupButtons", e);
            throw e; // Re-throw to be caught by onCreate
        }
    }

    private void loadManagerInfo() {
        String branch = ManagerDataManager.getCachedBranchName();
        if (branch != null) {
            branchDisplay.setText(branch);
            selectedBranch = branch;
        } else {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(snapshot -> {
                        String b = snapshot.getString("branch");
                        if (b != null && !b.isEmpty()) {
                            branchDisplay.setText(b);
                            selectedBranch = b;
                        } else {
                            branchDisplay.setText("No branch assigned");
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w("ManagerAddEmployee", "Could not load manager branch: " + e.getMessage());
                        branchDisplay.setText("Error loading branch");
                    });
            } else {
                Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void createEmployee() {
        try {
            android.util.Log.d("ManagerAddEmployee", "Starting employee creation process");

            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (firstName.isEmpty()) {
                firstNameInput.setError("First name is required");
                firstNameInput.requestFocus();
                return;
            }

            if (lastName.isEmpty()) {
                lastNameInput.setError("Last name is required");
                lastNameInput.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                emailInput.setError("Email is required");
                emailInput.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordInput.setError("Password is required");
                passwordInput.requestFocus();
                return;
            }

            if (password.length() < 8) {
                passwordInput.setError("Password must be at least 8 characters");
                passwordInput.requestFocus();
                return;
            }

            if (selectedRole == null || selectedRole.isEmpty()) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedBranch == null || selectedBranch.isEmpty() || selectedBranch.equals("No branch assigned")) {
                Toast.makeText(this, "Valid branch is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading dialog
            LoadingDialog loadingDialog = new LoadingDialog(this);
            loadingDialog.show();

            // Create a secondary Firebase instance so we don't log out the current Manager
            com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
            com.google.firebase.FirebaseApp secondaryApp;
            try {
                secondaryApp = com.google.firebase.FirebaseApp.getInstance("SecondaryApp");
            } catch (IllegalStateException e) {
                secondaryApp = com.google.firebase.FirebaseApp.initializeApp(getApplicationContext(), options, "SecondaryApp");
            }

            com.google.firebase.auth.FirebaseAuth secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp);
            
            secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String newUid = authResult.getUser().getUid();
                    java.util.Map<String, Object> userData = new java.util.HashMap<>();
                    userData.put("uid", newUid);
                    userData.put("email", email);
                    userData.put("firstName", firstName);
                    userData.put("lastName", lastName);
                    userData.put("role", selectedRole);
                    userData.put("branch", selectedBranch);
                    userData.put("isApproved", true);
                    userData.put("created_at", System.currentTimeMillis());
                    
                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                        .document(newUid).set(userData)
                        .addOnSuccessListener(aVoid -> {
                            loadingDialog.dismiss();
                            ManagerDataManager.forceRefreshEmployees(ManagerAddEmployee.this, null);
                            Toast.makeText(ManagerAddEmployee.this, "Technician created successfully", Toast.LENGTH_SHORT).show();
                            
                            // Sign out the secondary instance to clean up
                            secondaryAuth.signOut();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            loadingDialog.dismiss();
                            android.util.Log.e("ManagerAddEmployee", "Failed to create user doc", e);
                            Toast.makeText(ManagerAddEmployee.this, "Failed to save user details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    android.util.Log.e("ManagerAddEmployee", "Failed to create employee", e);
                    Toast.makeText(ManagerAddEmployee.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        } catch (Exception e) {
            android.util.Log.e("ManagerAddEmployee", "Exception in createEmployee", e);
            Toast.makeText(this, "Error starting creation process", Toast.LENGTH_SHORT).show();
        }
    }
}
