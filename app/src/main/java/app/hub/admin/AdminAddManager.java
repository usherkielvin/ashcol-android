package app.hub.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import app.hub.R;
import app.hub.util.LoadingDialog;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AdminAddManager extends AppCompatActivity {

    private TextInputEditText firstNameInput, lastNameInput, usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private Spinner branchSpinner;
    private String selectedBranch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_manager);

        initializeViews();
        setupBranchSpinner();
        setupButtons();
    }

    private void initializeViews() {
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.Email_val);
        passwordInput = findViewById(R.id.Pass_val);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        branchSpinner = findViewById(R.id.branchSpinner);
    }

    private void setupBranchSpinner() {
        // Create ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.branch_options, android.R.layout.simple_spinner_item);
        
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Apply the adapter to the spinner
        branchSpinner.setAdapter(adapter);
        
        // Set up the selection listener
        branchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip the first item "Select branch..."
                    selectedBranch = parent.getItemAtPosition(position).toString();
                } else {
                    selectedBranch = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBranch = "";
            }
        });
    }

    private void setupButtons() {
        Button createManagerButton = findViewById(R.id.createManagerButton);
        createManagerButton.setOnClickListener(v -> createManager());

        Button backButton = findViewById(R.id.closeButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void createManager() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validation
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

        if (username.isEmpty()) {
            usernameInput.setError("Username is required");
            usernameInput.requestFocus();
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

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        if (selectedBranch.isEmpty()) {
            Toast.makeText(this, "Please select a branch", Toast.LENGTH_SHORT).show();
            return;
        }

        LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.show();

        // Use a temporary Firebase instance to create the user without signing out the admin
        try {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            FirebaseApp tempApp;
            try {
                tempApp = FirebaseApp.getInstance("TempApp");
            } catch (IllegalStateException e) {
                tempApp = FirebaseApp.initializeApp(getApplicationContext(), options, "TempApp");
            }
            
            FirebaseAuth tempAuth = FirebaseAuth.getInstance(tempApp);
            tempAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    
                    // Create user data in Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("first_name", firstName);
                    userData.put("last_name", lastName);
                    userData.put("username", username);
                    userData.put("email", email);
                    userData.put("role", "manager");
                    userData.put("branch", selectedBranch);
                    userData.put("created_at", com.google.firebase.Timestamp.now());

                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            loadingDialog.dismiss();
                            Toast.makeText(AdminAddManager.this, "Manager created successfully!", Toast.LENGTH_SHORT).show();
                            tempAuth.signOut(); // Sign out from temp app
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            loadingDialog.dismiss();
                            Toast.makeText(AdminAddManager.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(AdminAddManager.this, "Error creating auth user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
