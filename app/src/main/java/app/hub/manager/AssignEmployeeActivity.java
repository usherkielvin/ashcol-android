package app.hub.manager;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.util.TokenManager;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AssignEmployeeActivity extends AppCompatActivity {

    private TextView tvTitle, tvTicketId, tvTicketTitle, tvTicketDescription, tvTicketAddress, tvSchedulePreview;
    private CalendarView calendarView;
    private com.google.android.material.textfield.TextInputEditText etTime, etNotes;
    private AutoCompleteTextView actvEmployee;
    private MaterialButton btnBack;
    private MaterialButton btnCancel, btnAssign;
    private ProgressBar progressBar;

    private TokenManager tokenManager;
    private String ticketId;
    private String ticketTitle;
    private String ticketDescription;
    private String ticketAddress;
    private List<EmployeeResponse.Employee> employees;
    private TechnicianAdapter technicianAdapter;
    private int selectedEmployeeId = -1;
    private String selectedDate;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("AssignEmployee", "onCreate called");

        try {
            super.onCreate(savedInstanceState);
            android.util.Log.d("AssignEmployee", "super.onCreate completed");

            setContentView(R.layout.activity_assign_employee);
            android.util.Log.d("AssignEmployee", "setContentView completed");

            initViews();
            android.util.Log.d("AssignEmployee", "initViews completed");

            setupClickListeners();
            android.util.Log.d("AssignEmployee", "setupClickListeners completed");

            loadIntentData();
            android.util.Log.d("AssignEmployee", "loadIntentData completed");

            loadEmployees();
            android.util.Log.d("AssignEmployee", "loadEmployees completed");

            // Test if we can manually trigger employee loading
            actvEmployee.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && employees.isEmpty()) {
                    android.util.Log.d("AssignEmployee", "Manual technician reload triggered");
                    loadEmployees();
                }
            });

            android.util.Log.d("AssignEmployee", "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("AssignEmployee", "Error in onCreate", e);
            Toast.makeText(this, "Error loading assignment screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        android.util.Log.d("AssignEmployee", "initViews started");

        try {
            android.util.Log.d("AssignEmployee", "Finding tvTitle");
            tvTitle = findViewById(R.id.tvTitle);
            android.util.Log.d("AssignEmployee", "tvTitle found: " + (tvTitle != null));

            android.util.Log.d("AssignEmployee", "Finding tvTicketId");
            tvTicketId = findViewById(R.id.tvTicketId);
            android.util.Log.d("AssignEmployee", "tvTicketId found: " + (tvTicketId != null));

            android.util.Log.d("AssignEmployee", "Finding tvTicketTitle");
            tvTicketTitle = findViewById(R.id.tvTicketTitle);
            android.util.Log.d("AssignEmployee", "tvTicketTitle found: " + (tvTicketTitle != null));

            android.util.Log.d("AssignEmployee", "Finding tvTicketDescription");
            tvTicketDescription = findViewById(R.id.tvTicketDescription);
            android.util.Log.d("AssignEmployee", "tvTicketDescription found: " + (tvTicketDescription != null));

            android.util.Log.d("AssignEmployee", "Finding tvTicketAddress");
            tvTicketAddress = findViewById(R.id.tvTicketAddress);
            android.util.Log.d("AssignEmployee", "tvTicketAddress found: " + (tvTicketAddress != null));

            android.util.Log.d("AssignEmployee", "Finding tvSchedulePreview");
            tvSchedulePreview = findViewById(R.id.tvSchedulePreview);
            android.util.Log.d("AssignEmployee", "tvSchedulePreview found: " + (tvSchedulePreview != null));

            android.util.Log.d("AssignEmployee", "Finding calendarView");
            calendarView = findViewById(R.id.calendarView);
            android.util.Log.d("AssignEmployee", "calendarView found: " + (calendarView != null));

            android.util.Log.d("AssignEmployee", "Finding etTime");
            etTime = findViewById(R.id.etTime);
            android.util.Log.d("AssignEmployee", "etTime found: " + (etTime != null));

            android.util.Log.d("AssignEmployee", "Finding etNotes");
            etNotes = findViewById(R.id.etNotes);
            android.util.Log.d("AssignEmployee", "etNotes found: " + (etNotes != null));

            android.util.Log.d("AssignEmployee", "Finding actvEmployee");
            actvEmployee = findViewById(R.id.actvEmployee);
            android.util.Log.d("AssignEmployee", "actvEmployee found: " + (actvEmployee != null));

            android.util.Log.d("AssignEmployee", "Finding btnBack");
            btnBack = findViewById(R.id.btnBack);
            android.util.Log.d("AssignEmployee", "btnBack found: " + (btnBack != null));

            android.util.Log.d("AssignEmployee", "Finding btnCancel");
            btnCancel = findViewById(R.id.btnCancel);
            android.util.Log.d("AssignEmployee", "btnCancel found: " + (btnCancel != null));

            android.util.Log.d("AssignEmployee", "Finding btnAssign");
            btnAssign = findViewById(R.id.btnAssign);
            android.util.Log.d("AssignEmployee", "btnAssign found: " + (btnAssign != null));

            android.util.Log.d("AssignEmployee", "Finding progressBar");
            progressBar = findViewById(R.id.progressBar);
            android.util.Log.d("AssignEmployee", "progressBar found: " + (progressBar != null));

            android.util.Log.d("AssignEmployee", "All views found successfully");

            tokenManager = new TokenManager(this);
            employees = new ArrayList<>();

            android.util.Log.d("AssignEmployee", "initViews completed");
        } catch (Exception e) {
            android.util.Log.e("AssignEmployee", "Error in initViews", e);
            throw new RuntimeException("Failed to initialize views: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        // Time picker
        etTime.setOnClickListener(v -> showTimePicker());

        // Calendar date selection
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = sdf.format(cal.getTime());
                updateSchedulePreview();
            }
        });

        // Technician selection
        actvEmployee.setOnItemClickListener((parent, view, position, id) -> {
            android.util.Log.d("AssignEmployee", "Technician selected at position: " + position);
            if (position >= 0 && position < employees.size()) {
                selectedEmployeeId = employees.get(position).getId();
                android.util.Log.d("AssignEmployee", "Selected technician ID: " + selectedEmployeeId);
                updateSchedulePreview();
            } else {
                android.util.Log.e("AssignEmployee",
                        "Invalid position: " + position + ", technicians size: " + employees.size());
            }
        });

        // Also handle text change to clear selection if text is cleared
        actvEmployee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    selectedEmployeeId = -1;
                    updateSchedulePreview();
                }
            }
        });

        // Assign button
        btnAssign.setOnClickListener(v -> assignAndSchedule());
    }

    private void loadIntentData() {
        ticketId = getIntent().getStringExtra("ticket_id");
        ticketTitle = getIntent().getStringExtra("ticket_title");
        ticketDescription = getIntent().getStringExtra("ticket_description");
        ticketAddress = getIntent().getStringExtra("ticket_address");

        if (ticketId != null) {
            tvTicketId.setText("Ticket ID: " + ticketId);
        }

        if (ticketTitle != null) {
            tvTicketTitle.setText(ticketTitle);
        }

        if (ticketDescription != null) {
            tvTicketDescription.setText(ticketDescription);
        }

        if (ticketAddress != null) {
            tvTicketAddress.setText(ticketAddress);
        }

        // Set initial date to today
        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(today.getTime());
        updateSchedulePreview();
    }

    private void loadEmployees() {
        employees.clear();
        employees.addAll(ManagerDataManager.getCachedEmployees());

        android.util.Log.d("AssignEmployee", "Loaded " + employees.size() + " technicians from cache");

        technicianAdapter = new TechnicianAdapter(AssignEmployeeActivity.this, employees);
        actvEmployee.setAdapter(technicianAdapter);

        actvEmployee.setThreshold(0);

        // Clear text and show dropdown on click
        actvEmployee.setOnClickListener(v -> {
            if (actvEmployee.getText().length() > 0) {
                actvEmployee.setText("");
            }
            actvEmployee.showDropDown();
        });

        actvEmployee.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvEmployee.showDropDown();
            }
        });

        if (employees.isEmpty()) {
            Toast.makeText(AssignEmployeeActivity.this, "No technicians available for assignment", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePicker() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    etTime.setText(selectedTime);
                    updateSchedulePreview();
                }, hour, minute, false);

        timePickerDialog.show();
    }

    private void updateSchedulePreview() {
        StringBuilder preview = new StringBuilder();

        if (selectedDate != null) {
            preview.append("Date: ").append(selectedDate).append("\n");
        }

        if (selectedTime != null) {
            preview.append("Time: ").append(selectedTime).append("\n");
        }

        if (selectedEmployeeId != -1 && employees != null) {
            for (EmployeeResponse.Employee emp : employees) {
                if (emp.getId() == selectedEmployeeId) {
                    String name = (emp.getFirstName() != null ? emp.getFirstName() : "") +
                            " " + (emp.getLastName() != null ? emp.getLastName() : "");
                    if (name.trim().isEmpty()) {
                        name = emp.getEmail() != null ? emp.getEmail() : "Unknown Technician";
                    }
                    preview.append("Technician: ").append(name.trim()).append("\n");
                    break;
                }
            }
        }

        if (preview.length() == 0) {
            preview.append("No schedule selected");
        }

        tvSchedulePreview.setText(preview.toString().trim());
    }

    private void assignAndSchedule() {
        // Validate inputs
        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime == null || selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedEmployeeId == -1) {
            Toast.makeText(this, "Please select a technician", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnAssign.setEnabled(false);

        // Find employee name for ticket record
        String staffNameTemp = "Unknown Technician";
        for (EmployeeResponse.Employee emp : employees) {
            if (emp.getId() == selectedEmployeeId) {
                staffNameTemp = (emp.getFirstName() != null ? emp.getFirstName() : "") + " " + (emp.getLastName() != null ? emp.getLastName() : "");
                if (staffNameTemp.trim().isEmpty() && emp.getEmail() != null) {
                    staffNameTemp = emp.getEmail();
                }
                break;
            }
        }
        final String assignedStaffName = staffNameTemp.trim();

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("scheduledDate", selectedDate);
        updates.put("scheduledTime", selectedTime);
        updates.put("scheduleNotes", etNotes.getText().toString().trim());
        updates.put("assignedStaffId", String.valueOf(selectedEmployeeId));
        updates.put("assignedStaff", assignedStaffName);
        updates.put("status", "ongoing");
        updates.put("statusColor", "#2196F3"); // Blue for ongoing
        updates.put("updated_at", String.valueOf(System.currentTimeMillis()));

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                btnAssign.setEnabled(true);
                
                Toast.makeText(AssignEmployeeActivity.this, "Ticket assigned and scheduled successfully", Toast.LENGTH_SHORT).show();
                ManagerDataManager.updateTicketAssignmentInCache(
                        ticketId,
                        "ongoing",
                        assignedStaffName,
                        selectedDate,
                        selectedTime);

                setResult(RESULT_OK);
                finish();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                btnAssign.setEnabled(true);
                android.util.Log.e("AssignEmployee", "Failed to update ticket assignment", e);
                Toast.makeText(AssignEmployeeActivity.this, "Assignment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
