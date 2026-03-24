package app.hub.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import app.hub.R;
import app.hub.api.CreateTicketRequest;
import app.hub.api.CreateTicketResponse;
import app.hub.api.UserResponse;
import app.hub.map.MapSelectionActivity;
import app.hub.util.TokenManager;

public class UserCreateTicketFragment extends Fragment {

    private static final String TAG = "UserCreateTicketFragment";
    private static final SimpleDateFormat DATE_FORMAT_API = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_FORMAT_DISPLAY = new SimpleDateFormat("MMM dd, yyyy",
            Locale.getDefault());
    private static final int PICK_IMAGE_REQUEST = 1002;
    private static final int MAX_IMAGE_SIZE_MB = 5;

    private EditText fullNameInput, contactInput, landmarkInput, descriptionInput, dateInput;
    private TextView amountView;
    private Button submitButton;
    private RelativeLayout mapLocationButton;
    private LinearLayout uploadButton;
    private Spinner serviceTypeSpinner;
    // unitTypeSpinner removed - now using checkboxes
    private TextView serviceTypeDisplay, locationHintText;
    private TokenManager tokenManager;
    private String selectedServiceType = "General Service";
    private String selectedUnitType;
    private Long selectedDateMillis = null;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private String selectedAddress = "";
    private Uri selectedImageUri = null;

    private final String[] serviceTypes = {"Cleaning", "Maintenance", "Repair", "Installation"};
    private final String[] unitTypes = {
            "Select Unit Type",
            "Window AC Unit",
            "Portable AC Unit",
            "Split-System (Mini-Split) AC Unit",
            "Central AC System",
            "Multi-Split System",
            "Packaged AC Unit",
            "Ductless Mini-Split",
            "Geothermal Heat Pump",
            "Chiller System"
    };

    public UserCreateTicketFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_service_request_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        fullNameInput = view.findViewById(R.id.etTitle);
        contactInput = view.findViewById(R.id.etContact);
        landmarkInput = view.findViewById(R.id.etLandmark);
        descriptionInput = view.findViewById(R.id.etDescription);
        dateInput = view.findViewById(R.id.etDate);
        amountView = view.findViewById(R.id.tvEstPrice);
        serviceTypeDisplay = view.findViewById(R.id.tvServiceType);
        submitButton = view.findViewById(R.id.btnSubmit);
        
        mapLocationButton = view.findViewById(R.id.btnMapLocation);
        uploadButton = view.findViewById(R.id.btnUpload);
        serviceTypeSpinner = view.findViewById(R.id.spinnerServiceType);
        // unitTypeSpinner removed - now using checkboxes
        
        locationHintText = view.findViewById(R.id.tvLocationHint);

        tokenManager = new TokenManager(getContext());

        String registeredName = getRegisteredName();
        if (fullNameInput != null && registeredName != null) {
            fullNameInput.setText(registeredName);
        }

        prefillContactFromProfile();

        // Set default service type
        if (serviceTypeDisplay != null) {
            serviceTypeDisplay.setText("Service Type:\n" + selectedServiceType);
        }

        // Set up service type spinner
        setupServiceTypeSpinner();
        
        // Unit type spinner removed - now using checkboxes in layout
        // setupUnitTypeSpinner();

        updatePresetAmount();

        // Set up map location button
        if (mapLocationButton != null) {
            mapLocationButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MapSelectionActivity.class);
                startActivityForResult(intent, 1001);
            });
        }

        // Set up date picker
        if (dateInput != null) {
            dateInput.setOnClickListener(v -> showDatePicker());
            dateInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) showDatePicker();
            });
        }

        // Set up image upload
        if (uploadButton != null) {
            uploadButton.setOnClickListener(v -> openImagePicker());
        }

        // Set up submit button
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> createTicket());
        }
    }

    private void setupServiceTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, serviceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceTypeSpinner.setAdapter(adapter);
        
        // Set default selection (Cleaning = index 0)
        serviceTypeSpinner.setSelection(0);
        
        serviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedServiceType = serviceTypes[position];
                if (serviceTypeDisplay != null) {
                    serviceTypeDisplay.setText("Service Type:\n" + selectedServiceType);
                }
                updatePresetAmount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    // Unit type spinner method removed - now using checkboxes
    /*
    private void setupUnitTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, unitTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitTypeSpinner.setAdapter(adapter);
        
        unitTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedUnitType = null; // "Select Unit Type" option
                } else {
                    selectedUnitType = unitTypes[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedUnitType = null;
            }
        });
    }
    */

    private void updatePresetAmount() {
        double amount = getPresetAmount(selectedServiceType);
        if (amountView != null) {
            amountView.setText(formatAmount(amount));
        }
    }

    private double getPresetAmount(String serviceType) {
        if (serviceType == null) {
            return 8000.0;
        }
        switch (serviceType.trim().toLowerCase(java.util.Locale.ENGLISH)) {
            case "cleaning":
                return 8000.0;
            case "maintenance":
                return 6500.0;
            case "repair":
                return 7000.0;
            case "installation":
                return 9000.0;
            default:
                return 8000.0;
        }
    }

    private String formatAmount(double amount) {
        return "Php " + String.format(java.util.Locale.getDefault(), "%,.2f", amount);
    }

    private void prefillContactFromProfile() {
        if (contactInput == null || tokenManager == null) {
            return;
        }

        String currentValue = contactInput.getText() != null
                ? contactInput.getText().toString().trim()
                : "";
        if (!currentValue.isEmpty()) {
            return;
        }

        String email = tokenManager.getEmail();
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    if (documentSnapshot.exists()) {
                        String phone = documentSnapshot.getString("phone");
                        if (phone != null && !phone.trim().isEmpty() && contactInput != null) {
                            String current = contactInput.getText() != null
                                    ? contactInput.getText().toString().trim()
                                    : "";
                            if (current.isEmpty()) {
                                contactInput.setText(phone.trim());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Ignore prefill failures.
                });
    }

    private void showDatePicker() {
        long today = MaterialDatePicker.todayInUtcMilliseconds();
        Calendar maxCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        maxCal.add(Calendar.YEAR, 2);

        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Select preferred service date");
        builder.setSelection(selectedDateMillis != null ? selectedDateMillis : today);
        builder.setCalendarConstraints(new CalendarConstraints.Builder()
                .setStart(today)
                .setEnd(maxCal.getTimeInMillis())
                .build());

        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMillis = selection;
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.setTimeInMillis(selection);
            if (dateInput != null)
                dateInput.setText(DATE_FORMAT_DISPLAY.format(cal.getTime()));
        });
        picker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0.0);
            selectedLongitude = data.getDoubleExtra("longitude", 0.0);
            String address = data.getStringExtra("address");
            selectedAddress = address != null ? address : "";

            if (locationHintText != null && address != null) {
                locationHintText.setText(address);
                locationHintText.setTextColor(getResources().getColor(android.R.color.black));
            }

            Toast.makeText(getContext(), "Location selected: " + address, Toast.LENGTH_SHORT).show();
        }
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
                if (inputStream != null) {
                    int fileSize = inputStream.available();
                    inputStream.close();
                    
                    int fileSizeMB = fileSize / (1024 * 1024);
                    if (fileSizeMB > MAX_IMAGE_SIZE_MB) {
                        Toast.makeText(getContext(), "Image size must be less than " + MAX_IMAGE_SIZE_MB + "MB", Toast.LENGTH_LONG).show();
                        selectedImageUri = null;
                        return;
                    }
                    
                    Toast.makeText(getContext(), "Image selected (" + fileSizeMB + "MB)", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking file size", e);
                Toast.makeText(getContext(), "Error selecting image", Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
            }
        }
    }

    private void createTicket() {
        String fullName = getRegisteredName();
        if (fullName == null) {
            fullName = fullNameInput != null ? fullNameInput.getText().toString().trim() : "";
        }
        String contact = contactInput != null ? contactInput.getText().toString().trim() : "";
        String landmark = landmarkInput != null ? landmarkInput.getText().toString().trim() : "";
        String description = descriptionInput != null ? descriptionInput.getText().toString().trim() : "";

        if (fullName.isEmpty() || contact.isEmpty() || selectedAddress.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = getPresetAmount(selectedServiceType);

        // Combine address with landmark
        String fullAddress = selectedAddress;
        if (!landmark.isEmpty()) {
            fullAddress += " - " + landmark;
        }

        String preferredDate = null;
        if (selectedDateMillis != null) {
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.setTimeInMillis(selectedDateMillis);
            preferredDate = DATE_FORMAT_API.format(cal.getTime());
        }

        // Add unit type to description if selected
        String fullDescription = description;
        if (selectedUnitType != null && !selectedUnitType.isEmpty()) {
            fullDescription = "Unit Type: " + selectedUnitType + "\n" + description;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Creating...");

        String userEmail = tokenManager.getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(getContext(), "User email not found.", Toast.LENGTH_SHORT).show();
            submitButton.setEnabled(true);
            submitButton.setText("Submit");
            return;
        }

        Map<String, Object> ticketData = new HashMap<>();
        String newTicketId = "TKT-" + System.currentTimeMillis();
        ticketData.put("ticketId", newTicketId);
        ticketData.put("title", fullName);
        ticketData.put("description", fullDescription);
        ticketData.put("service_type", selectedServiceType);
        ticketData.put("location", fullAddress);
        ticketData.put("contact", contact);
        ticketData.put("preferred_date", preferredDate);
        ticketData.put("latitude", selectedLatitude != 0.0 ? selectedLatitude : null);
        ticketData.put("longitude", selectedLongitude != 0.0 ? selectedLongitude : null);
        ticketData.put("amount", amount);
        ticketData.put("status", "pending");
        ticketData.put("customer_email", userEmail);
        ticketData.put("created_at", System.currentTimeMillis());
        ticketData.put("updated_at", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("tickets")
                .document(newTicketId)
                .set(ticketData)
                .addOnSuccessListener(aVoid -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit");

                    // Navigate to confirmation screen
                    Intent intent = new Intent(getActivity(), TicketConfirmationActivity.class);
                    intent.putExtra("ticket_id", newTicketId);
                    intent.putExtra("status", "pending");
                    startActivity(intent);

                    if (getActivity() != null) {
                        getActivity().finish();
                    }

                    clearForm();
                    Toast.makeText(getContext(), "Ticket created successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit");
                    Toast.makeText(getContext(), "Failed to create ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating ticket in Firestore", e);
                });
    }


    private void pushTicketToFirestore(CreateTicketResponse ticketResponse) {
        if (ticketResponse == null) {
            return;
        }

        CreateTicketResponse.TicketData ticketData = ticketResponse.getTicket();
        if (ticketData == null) {
            return;
        }

        String ticketId = ticketData.getTicketId();
        String status = ticketResponse.getStatus();
        if (status == null && ticketData.getStatus() != null) {
            status = ticketData.getStatus().getName();
        }

        String branchName = null;
        if (ticketData.getBranch() != null) {
            branchName = ticketData.getBranch().getName();
        }

        if (ticketId == null || ticketId.trim().isEmpty() || branchName == null || branchName.trim().isEmpty()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("status", status != null ? status : "pending");
        payload.put("branch", branchName);
        payload.put("updatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("tickets")
                .document(ticketId)
                .set(payload);
    }

    private String getRegisteredName() {
        if (tokenManager == null) {
            return null;
        }
        String name = tokenManager.getName();
        return isValidName(name) ? name.trim() : null;
    }

    private boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && !"null".equalsIgnoreCase(name.trim());
    }

    private void clearForm() {
        if (fullNameInput != null) fullNameInput.setText("");
        if (contactInput != null) contactInput.setText("");
        if (landmarkInput != null) landmarkInput.setText("");
        if (descriptionInput != null) descriptionInput.setText("");
        if (dateInput != null) dateInput.setText("");
        if (locationHintText != null) locationHintText.setText(R.string.hint_map);
        if (serviceTypeSpinner != null) serviceTypeSpinner.setSelection(0);
        // unitTypeSpinner removed - checkboxes will need to be unchecked separately if needed
        selectedDateMillis = null;
        selectedAddress = "";
        selectedUnitType = null;
        selectedImageUri = null;
    }
}
