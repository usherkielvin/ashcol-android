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
import app.hub.api.RegisterRequest;
import app.hub.api.RegisterResponse;
import app.hub.util.LoadingDialog;

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

        // Show loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.show();

        // Create manager request
        RegisterRequest registerRequest = new RegisterRequest(
            username, firstName, lastName, email, "", "", 
            password, confirmPassword, "manager", selectedBranch
        );

        ApiService apiService = ApiClient.getApiService();
        Call<RegisterResponse> call = apiService.register(registerRequest);

        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                loadingDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    if (registerResponse.isSuccess()) {
                        Toast.makeText(AdminAddManager.this, "Manager created successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // Set result for parent activity to refresh
                        finish();
                    } else {
                        String errorMessage = "Failed to create manager";
                        if (registerResponse.getErrors() != null) {
                            // Handle validation errors
                            StringBuilder sb = new StringBuilder();
                            if (registerResponse.getErrors().getEmail() != null) {
                                sb.append("Email: ").append(String.join(", ", registerResponse.getErrors().getEmail())).append("\n");
                            }
                            if (registerResponse.getErrors().getUsername() != null) {
                                sb.append("Username: ").append(String.join(", ", registerResponse.getErrors().getUsername())).append("\n");
                            }
                            if (sb.length() > 0) {
                                errorMessage = sb.toString().trim();
                            }
                        }
                        Toast.makeText(AdminAddManager.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(AdminAddManager.this, "Failed to create manager", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(AdminAddManager.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
