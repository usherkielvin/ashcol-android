package app.hub.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import app.hub.R;
import app.hub.util.LoadingDialog;

public class AdminAddEmployee extends AppCompatActivity {
    private static final String TAG = "AdminAddEmployee";

    private TextInputEditText firstNameInput, lastNameInput, usernameInput, emailInput, passwordInput, confirmPasswordInput, roleInput, branchInput;
    private String selectedRole = "";
    private String selectedBranch = "";

    // Available roles
    private final String[] roles = {"technician", "manager"};
    
    // Available branches
    private final String[] branches = {
        "ASHCOL TAGUIG",
        "ASHCOL VALENZUELA",
        "ASHCOL RODRIGUEZ RIZAL",
        "ASHCOL PAMPANGA",
        "ASHCOL BULACAN",
        "ASHCOL GENTRI CAVITE",
        "ASHCOL DASMARINAS CAVITE",
        "ASHCOL STA ROSA – TAGAYTAY RD",
        "ASHCOL LAGUNA",
        "ASHCOL BATANGAS",
        "ASHCOL CANDELARIA QUEZON PROVINCE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_employee);

        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.Email_val);
        passwordInput = findViewById(R.id.Pass_val);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        roleInput = findViewById(R.id.roleInput);
        branchInput = findViewById(R.id.branchInput);

        // Set up role selection
        roleInput.setOnClickListener(v -> showRoleSelection());
        
        // Set up branch selection
        branchInput.setOnClickListener(v -> showBranchSelection());

        Button createEmployeeButton = findViewById(R.id.createEmployeeButton);
        createEmployeeButton.setOnClickListener(v -> createEmployee());

        Button backButton = findViewById(R.id.closeButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void showRoleSelection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Role");
        builder.setItems(roles, (dialog, which) -> {
            selectedRole = roles[which];
            roleInput.setText(selectedRole);
        });
        builder.show();
    }

    private void showBranchSelection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Branch");
        builder.setItems(branches, (dialog, which) -> {
            selectedBranch = branches[which];
            branchInput.setText(selectedBranch);
        });
        builder.show();
    }

    private void createEmployee() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || 
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedRole.isEmpty()) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBranch.isEmpty()) {
            Toast.makeText(this, "Please select a branch", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.show();

        // Create a secondary Firebase instance to avoid logging out current Admin
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
        com.google.firebase.FirebaseApp secondaryApp;
        try {
            secondaryApp = com.google.firebase.FirebaseApp.getInstance("SecondaryAppAdmin");
        } catch (IllegalStateException e) {
            secondaryApp = com.google.firebase.FirebaseApp.initializeApp(getApplicationContext(), options, "SecondaryAppAdmin");
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
                userData.put("username", username);
                userData.put("role", selectedRole);
                userData.put("branch", selectedBranch);
                userData.put("isApproved", true);
                userData.put("created_at", System.currentTimeMillis());
                
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                    .document(newUid).set(userData)
                    .addOnSuccessListener(aVoid -> {
                        loadingDialog.dismiss();
                        Toast.makeText(AdminAddEmployee.this, selectedRole + " created successfully", Toast.LENGTH_SHORT).show();
                        secondaryAuth.signOut();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        Log.e(TAG, "Failed to save user doc", e);
                        Toast.makeText(AdminAddEmployee.this, "Failed to save user details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Log.e(TAG, "Failed to create user", e);
                Toast.makeText(AdminAddEmployee.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
