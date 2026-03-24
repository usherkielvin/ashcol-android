package app.hub.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
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
import app.hub.api.TicketListResponse;
import app.hub.map.MapSelectionActivity;
import app.hub.util.TokenManager;

public class ServiceSelectActivity extends AppCompatActivity {

    private static final String TAG = "ServiceSelectActivity";
    private static final SimpleDateFormat DATE_FORMAT_API = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_FORMAT_DISPLAY = new SimpleDateFormat("MMM dd, yyyy",
            Locale.getDefault());
    private static final int PICK_IMAGE_REQUEST = 1002;
    private static final int MAX_IMAGE_SIZE_MB = 5;

    private EditText fullNameInput, contactInput, landmarkInput, descriptionInput, dateInput;
    private TextView amountView;
    private Button submitButton;
    private RelativeLayout mapLocationButton;
    private HorizontalScrollView imageScrollView;
    private LinearLayout uploadButton, imagePreviewContainer, priceContainer;
    private RelativeLayout imagePreview1Container, imagePreview2Container;
    private ImageView imagePreview1, imagePreview2;
    private ImageButton btnRemoveImage1, btnRemoveImage2, btnShowSummary;
    private Spinner serviceTypeSpinner;
    private CheckBox cbSplit, cbWindow, cbARF;
    private TextView locationHintText;
    private TokenManager tokenManager;
    private String selectedServiceType;
    private Map<String, Integer> unitQuantities = new HashMap<>();
    private Long selectedDateMillis = null;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private String selectedAddress = "";
    private Uri selectedImageUri1 = null;
    private Uri selectedImageUri2 = null;
    private int currentImageSlot = 0; // 0 = none, 1 = first slot, 2 = second slot
    private BottomSheetDialog summaryBottomSheet;

    private final String[] serviceTypes = {"Cleaning", "Maintenance", "Repair", "Installation"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_service_request_form);

        // Initialize views
        fullNameInput = findViewById(R.id.etTitle);
        contactInput = findViewById(R.id.etContact);
        landmarkInput = findViewById(R.id.etLandmark);
        descriptionInput = findViewById(R.id.etDescription);
        dateInput = findViewById(R.id.etDate);
        amountView = findViewById(R.id.tvEstPrice);
        submitButton = findViewById(R.id.btnSubmit);
        
        mapLocationButton = findViewById(R.id.btnMapLocation);
        uploadButton = findViewById(R.id.btnUpload);
        serviceTypeSpinner = findViewById(R.id.spinnerServiceType);
        priceContainer = findViewById(R.id.priceContainer);
        btnShowSummary = findViewById(R.id.btnShowSummary);
        
        // Initialize checkboxes
        cbSplit = findViewById(R.id.cbSplit);
        cbWindow = findViewById(R.id.cbWindow);
        cbARF = findViewById(R.id.cbARF);
        
        imageScrollView = findViewById(R.id.imageScrollView);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        imagePreview1Container = findViewById(R.id.imagePreview1Container);
        imagePreview2Container = findViewById(R.id.imagePreview2Container);
        imagePreview1 = findViewById(R.id.imagePreview1);
        imagePreview2 = findViewById(R.id.imagePreview2);
        btnRemoveImage1 = findViewById(R.id.btnRemoveImage1);
        btnRemoveImage2 = findViewById(R.id.btnRemoveImage2);
        
        locationHintText = findViewById(R.id.tvLocationHint);

        tokenManager = new TokenManager(this);

        String registeredName = getRegisteredName();
        if (fullNameInput != null && registeredName != null) {
            fullNameInput.setText(registeredName);
        }

        prefillContactFromProfile();

        // Get the selected service type from the intent
        selectedServiceType = getIntent().getStringExtra("serviceType");

        // Set up service type spinner
        setupServiceTypeSpinner();
        
        // Set up unit checkboxes
        setupUnitCheckboxes();

        updatePresetAmount();
        
        // Set up summary button
        if (btnShowSummary != null) {
            btnShowSummary.setOnClickListener(v -> showSummaryBottomSheet());
        }
        if (priceContainer != null) {
            priceContainer.setOnClickListener(v -> showSummaryBottomSheet());
        }

        // Set up back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Set up map location button
        if (mapLocationButton != null) {
            mapLocationButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(ServiceSelectActivity.this, MapSelectionActivity.class);
                    startActivityForResult(intent, 1001);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting MapSelectionActivity", e);
                    Toast.makeText(this, "Error opening map", Toast.LENGTH_SHORT).show();
                }
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
        
        // Set up remove image buttons
        if (btnRemoveImage1 != null) {
            btnRemoveImage1.setOnClickListener(v -> removeImage(1));
        }
        if (btnRemoveImage2 != null) {
            btnRemoveImage2.setOnClickListener(v -> removeImage(2));
        }

        // Set up submit button
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> showCheckingScreen());
        }
    }

    private void setupServiceTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serviceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceTypeSpinner.setAdapter(adapter);
        
        // Set the pre-selected service type from intent
        if (selectedServiceType != null) {
            for (int i = 0; i < serviceTypes.length; i++) {
                if (serviceTypes[i].equalsIgnoreCase(selectedServiceType)) {
                    serviceTypeSpinner.setSelection(i);
                    break;
                }
            }
        }
        
        serviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedServiceType = serviceTypes[position];
                updatePresetAmount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupUnitCheckboxes() {
        // Set up checkbox listeners
        View.OnClickListener checkboxListener = v -> updatePriceFromCheckboxes();
        
        if (cbSplit != null) cbSplit.setOnClickListener(checkboxListener);
        if (cbWindow != null) cbWindow.setOnClickListener(checkboxListener);
        if (cbARF != null) cbARF.setOnClickListener(checkboxListener);
        
        // Initialize quantities for checked boxes
        unitQuantities.put("Split", 1);
        unitQuantities.put("Window", 1);
        unitQuantities.put("ARF", 1);
    }
    
    private void updatePriceFromCheckboxes() {
        double basePrice = getServicePrice(selectedServiceType);
        double totalPrice = 0.0;
        
        int totalUnits = 0;
        if (cbSplit != null && cbSplit.isChecked()) {
            int qty = unitQuantities.getOrDefault("Split", 1);
            totalPrice += basePrice * qty;
            totalUnits += qty;
        }
        if (cbWindow != null && cbWindow.isChecked()) {
            int qty = unitQuantities.getOrDefault("Window", 1);
            totalPrice += basePrice * qty;
            totalUnits += qty;
        }
        if (cbARF != null && cbARF.isChecked()) {
            int qty = unitQuantities.getOrDefault("ARF", 1);
            totalPrice += basePrice * qty;
            totalUnits += qty;
        }
        
        if (amountView != null) {
            amountView.setText(formatAmount(totalPrice));
        }
    }
    
    private double getServicePrice(String serviceType) {
        if (serviceType == null) return 8000.0;
        
        switch (serviceType.trim().toLowerCase(Locale.ENGLISH)) {
            case "installation":
                return 9000.0;
            case "repair":
                return 7000.0;
            case "maintenance":
                return 6500.0;
            case "cleaning":
                return 8000.0;
            default:
                return 8000.0;
        }
    }
    
    private void showSummaryBottomSheet() {
        // Check if any unit is selected
        boolean hasSelection = (cbSplit != null && cbSplit.isChecked()) ||
                              (cbWindow != null && cbWindow.isChecked()) ||
                              (cbARF != null && cbARF.isChecked());
        
        if (!hasSelection) {
            Toast.makeText(this, "Please select at least one AC unit type", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create bottom sheet
        summaryBottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_summary, null);
        summaryBottomSheet.setContentView(sheetView);
        
        LinearLayout itemsContainer = sheetView.findViewById(R.id.itemsContainer);
        TextView tvSummaryTotal = sheetView.findViewById(R.id.tvSummaryTotal);
        Button btnContinue = sheetView.findViewById(R.id.btnContinue);
        
        // Clear existing items
        if (itemsContainer != null) {
            itemsContainer.removeAllViews();
        }
        
        double basePrice = getServicePrice(selectedServiceType);
        
        // Add items for each selected unit type
        if (cbSplit != null && cbSplit.isChecked()) {
            View itemView = createUnitItemView("Split", basePrice, itemsContainer, tvSummaryTotal);
            itemsContainer.addView(itemView);
        }
        if (cbWindow != null && cbWindow.isChecked()) {
            View itemView = createUnitItemView("Window", basePrice, itemsContainer, tvSummaryTotal);
            itemsContainer.addView(itemView);
        }
        if (cbARF != null && cbARF.isChecked()) {
            View itemView = createUnitItemView("ARF", basePrice, itemsContainer, tvSummaryTotal);
            itemsContainer.addView(itemView);
        }
        
        // Update total
        updateSummaryTotal(tvSummaryTotal, basePrice);
        
        // Continue button
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                summaryBottomSheet.dismiss();
                // Optionally scroll to submit button or show next section
            });
        }
        
        summaryBottomSheet.show();
    }
    
    private View createUnitItemView(String unitType, double basePrice, LinearLayout container, TextView tvTotal) {
        View itemView = getLayoutInflater().inflate(R.layout.item_summary_unit, container, false);
        
        TextView tvItemName = itemView.findViewById(R.id.tvItemName);
        TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
        TextView tvQuantity = itemView.findViewById(R.id.tvQuantity);
        ImageButton btnMinus = itemView.findViewById(R.id.btnMinus);
        ImageButton btnPlus = itemView.findViewById(R.id.btnPlus);
        
        // Set item name
        String itemName = unitType + " AC " + (selectedServiceType != null ? selectedServiceType : "Service");
        tvItemName.setText(itemName);
        
        // Set price
        tvItemPrice.setText(formatAmount(basePrice));
        
        // Set quantity
        int currentQty = unitQuantities.getOrDefault(unitType, 1);
        tvQuantity.setText(String.valueOf(currentQty));
        
        // Minus button
        btnMinus.setOnClickListener(v -> {
            int qty = unitQuantities.getOrDefault(unitType, 1);
            if (qty > 1) {
                qty--;
                unitQuantities.put(unitType, qty);
                tvQuantity.setText(String.valueOf(qty));
                updateSummaryTotal(tvTotal, basePrice);
                updatePriceFromCheckboxes();
            }
        });
        
        // Plus button
        btnPlus.setOnClickListener(v -> {
            int qty = unitQuantities.getOrDefault(unitType, 1);
            qty++;
            unitQuantities.put(unitType, qty);
            tvQuantity.setText(String.valueOf(qty));
            updateSummaryTotal(tvTotal, basePrice);
            updatePriceFromCheckboxes();
        });
        
        return itemView;
    }
    
    private void updateSummaryTotal(TextView tvTotal, double basePrice) {
        if (tvTotal == null) return;
        
        double total = 0.0;
        
        if (cbSplit != null && cbSplit.isChecked()) {
            total += basePrice * unitQuantities.getOrDefault("Split", 1);
        }
        if (cbWindow != null && cbWindow.isChecked()) {
            total += basePrice * unitQuantities.getOrDefault("Window", 1);
        }
        if (cbARF != null && cbARF.isChecked()) {
            total += basePrice * unitQuantities.getOrDefault("ARF", 1);
        }
        
        tvTotal.setText(formatAmount(total));
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
                    if (isFinishing()) return;
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

    private void updatePresetAmount() {
        updatePriceFromCheckboxes();
    }

    private double getPresetAmount(String serviceType) {
        return getServicePrice(serviceType);
    }


    private String formatAmount(double amount) {
        return "Php " + String.format(java.util.Locale.getDefault(), "%,.2f", amount);
    }
    
    private double getTotalAmount() {
        double basePrice = getServicePrice(selectedServiceType);
        double total = 0.0;
        
        if (cbSplit != null && cbSplit.isChecked()) {
            total += basePrice * unitQuantities.getOrDefault("Split", 1);
        }
        if (cbWindow != null && cbWindow.isChecked()) {
            total += basePrice * unitQuantities.getOrDefault("Window", 1);
        }
        if (cbARF != null && cbARF.isChecked()) {
            total += basePrice * unitQuantities.getOrDefault("ARF", 1);
        }
        
        return total > 0 ? total : basePrice;
    }

    private void showDatePicker() {
        long today = MaterialDatePicker.todayInUtcMilliseconds();
        long minDate = today;
        Calendar maxCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        maxCal.add(Calendar.YEAR, 2);
        long maxDate = maxCal.getTimeInMillis();

        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Select preferred service date");
        builder.setSelection(selectedDateMillis != null ? selectedDateMillis : today);
        builder.setCalendarConstraints(new CalendarConstraints.Builder()
                .setStart(minDate)
                .setEnd(maxDate)
                .build());

        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMillis = selection;
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.setTimeInMillis(selection);
            String dateStr = DATE_FORMAT_DISPLAY.format(cal.getTime());
            if (dateInput != null)
                dateInput.setText(dateStr);
        });
        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void openImagePicker() {
        // Check if we already have 2 images
        if (selectedImageUri1 != null && selectedImageUri2 != null) {
            Toast.makeText(this, "Maximum 2 images allowed. Remove one to add another.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determine which slot to fill
        currentImageSlot = (selectedImageUri1 == null) ? 1 : 2;
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }
    
    private void removeImage(int slot) {
        if (slot == 1) {
            selectedImageUri1 = null;
            imagePreview1Container.setVisibility(View.GONE);
            imagePreview1.setImageURI(null);
        } else if (slot == 2) {
            selectedImageUri2 = null;
            imagePreview2Container.setVisibility(View.GONE);
            imagePreview2.setImageURI(null);
        }
        
        // Hide container if no images
        if (selectedImageUri1 == null && selectedImageUri2 == null) {
            imageScrollView.setVisibility(View.GONE);
        }
        
        Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
    }
    
    private void updateImagePreviews() {
        // Show/hide preview container
        if (selectedImageUri1 != null || selectedImageUri2 != null) {
            imageScrollView.setVisibility(View.VISIBLE);
        } else {
            imageScrollView.setVisibility(View.GONE);
        }
        
        // Update image 1
        if (selectedImageUri1 != null) {
            imagePreview1Container.setVisibility(View.VISIBLE);
            imagePreview1.setImageURI(selectedImageUri1);
        } else {
            imagePreview1Container.setVisibility(View.GONE);
        }
        
        // Update image 2
        if (selectedImageUri2 != null) {
            imagePreview2Container.setVisibility(View.VISIBLE);
            imagePreview2.setImageURI(selectedImageUri2);
        } else {
            imagePreview2Container.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra("address");
            selectedLatitude = data.getDoubleExtra("latitude", 0.0);
            selectedLongitude = data.getDoubleExtra("longitude", 0.0);
            selectedAddress = address != null ? address : "";

            if (locationHintText != null && address != null) {
                locationHintText.setText(address);
                locationHintText.setTextColor(getResources().getColor(android.R.color.black));
                Log.d(TAG, "Location set: " + address);
                Log.d(TAG, "Coordinates - lat: " + selectedLatitude + ", lng: " + selectedLongitude);
            }
            Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show();
        }
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            
            // Check file size
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedUri);
                if (inputStream != null) {
                    int fileSize = inputStream.available();
                    inputStream.close();
                    
                    int fileSizeMB = fileSize / (1024 * 1024);
                    if (fileSizeMB > MAX_IMAGE_SIZE_MB) {
                        Toast.makeText(this, "Image size must be less than " + MAX_IMAGE_SIZE_MB + "MB", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Assign to the appropriate slot
                    if (currentImageSlot == 1) {
                        selectedImageUri1 = selectedUri;
                    } else if (currentImageSlot == 2) {
                        selectedImageUri2 = selectedUri;
                    }
                    
                    updateImagePreviews();
                    
                    int imageCount = (selectedImageUri1 != null ? 1 : 0) + (selectedImageUri2 != null ? 1 : 0);
                    Toast.makeText(this, "Image " + imageCount + " added (" + fileSizeMB + "MB)", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking file size", e);
                Toast.makeText(this, "Error selecting image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showCheckingScreen() {
        // Validate inputs first
        String fullName = getRegisteredName();
        if (fullName == null) {
            fullName = fullNameInput != null ? fullNameInput.getText().toString().trim() : "";
        }
        String contact = contactInput != null ? contactInput.getText().toString().trim() : "";
        String description = descriptionInput != null ? descriptionInput.getText().toString().trim() : "";
        String date = dateInput != null ? dateInput.getText().toString().trim() : "";
        
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (contact.isEmpty()) {
            Toast.makeText(this, "Please enter your contact number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedAddress == null || selectedAddress.isEmpty()) {
            Toast.makeText(this, "Please select your address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate at least one unit type is selected
        boolean hasUnitSelected = (cbSplit != null && cbSplit.isChecked()) ||
                                  (cbWindow != null && cbWindow.isChecked()) ||
                                  (cbARF != null && cbARF.isChecked());
        
        if (!hasUnitSelected) {
            Toast.makeText(this, "Please select at least one AC unit type", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (description.isEmpty()) {
            Toast.makeText(this, "Please provide service details", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (date.isEmpty()) {
            Toast.makeText(this, "Please select a preferred date", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Prepare data for checking screen
        String serviceType = selectedServiceType != null ? selectedServiceType : "Service";
        String specificService = description; // Use description as specific service
        String unitType = getSelectedUnitTypesString();
        String landmark = landmarkInput != null ? landmarkInput.getText().toString().trim() : "";
        
        // Show checking fragment
        SRFCheckingFragment fragment = SRFCheckingFragment.newInstance(
                fullName, contact, selectedAddress, landmark,
                serviceType, specificService, unitType, description,
                date, selectedImageUri1, selectedImageUri2
        );
        
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }
    
    private String getSelectedUnitTypesString() {
        StringBuilder units = new StringBuilder();
        
        if (cbSplit != null && cbSplit.isChecked()) {
            int qty = unitQuantities.getOrDefault("Split", 1);
            units.append("Split (").append(qty).append(")");
        }
        if (cbWindow != null && cbWindow.isChecked()) {
            int qty = unitQuantities.getOrDefault("Window", 1);
            if (units.length() > 0) units.append(", ");
            units.append("Window (").append(qty).append(")");
        }
        if (cbARF != null && cbARF.isChecked()) {
            int qty = unitQuantities.getOrDefault("ARF", 1);
            if (units.length() > 0) units.append(", ");
            units.append("ARF (").append(qty).append(")");
        }
        
        return units.toString();
    }
    
    public void confirmAndCreateTicket() {
        // This method is called from SRFCheckingFragment when user confirms
        // Pop the checking fragment from back stack
        getSupportFragmentManager().popBackStack();
        // Create the ticket
        createTicket();
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
            Toast.makeText(this, "Please fill out all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedServiceType == null || selectedServiceType.isEmpty()) {
            selectedServiceType = "General Service";
        }

        // Don't combine address with landmark - keep address as is from map
        String fullAddress = selectedAddress;

        // Format preferred date for API (yyyy-MM-dd)
        String preferredDate = null;
        if (selectedDateMillis != null) {
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.setTimeInMillis(selectedDateMillis);
            preferredDate = DATE_FORMAT_API.format(cal.getTime());
        }

        // Build full description with unit types and quantities
        String fullDescription = "";
        
        // Add unit types if selected
        String unitTypesStr = getSelectedUnitTypesString();
        if (!unitTypesStr.isEmpty()) {
            fullDescription = "Unit Types: " + unitTypesStr + "\n";
        }
        
        // Add main description
        if (!description.isEmpty()) {
            fullDescription += description;
        }
        
        // Clean up if only whitespace
        fullDescription = fullDescription.trim();
        if (fullDescription.isEmpty()) {
            fullDescription = "Service request";
        }

        double amount = getTotalAmount();

        String userEmail = tokenManager.getEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Creating ticket with: Lat=" + selectedLatitude + ", Lng=" + selectedLongitude);

        submitButton.setEnabled(false);
        submitButton.setText("Creating...");

        String newTicketId = "TKT-" + System.currentTimeMillis();
        Map<String, Object> ticketData = new HashMap<>();
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

        // Check if we have any images to upload
        if (selectedImageUri1 != null || selectedImageUri2 != null) {
            uploadImagesAndSaveTicket(newTicketId, ticketData);
        } else {
            saveTicketToFirestore(newTicketId, ticketData);
        }
    }

    private void saveTicketToFirestore(String ticketId, Map<String, Object> ticketData) {
        FirebaseFirestore.getInstance()
                .collection("tickets")
                .document(ticketId)
                .set(ticketData)
                .addOnSuccessListener(aVoid -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit");
                    Toast.makeText(ServiceSelectActivity.this, "Ticket created successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to confirmation screen
                    Intent intent = new Intent(ServiceSelectActivity.this, TicketConfirmationActivity.class);
                    intent.putExtra("ticket_id", ticketId);
                    intent.putExtra("status", "pending");
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit");
                    Toast.makeText(ServiceSelectActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error saving ticket to Firestore", e);
                });
    }

    private void uploadImagesAndSaveTicket(String ticketId, Map<String, Object> ticketData) {
        java.util.List<Uri> urisToUpload = new java.util.ArrayList<>();
        if (selectedImageUri1 != null) urisToUpload.add(selectedImageUri1);
        if (selectedImageUri2 != null) urisToUpload.add(selectedImageUri2);

        java.util.List<String> downloadUrls = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger uploadCount = new java.util.concurrent.atomic.AtomicInteger();
        boolean[] hasFailed = new boolean[1];

        com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();

        for (int i = 0; i < urisToUpload.size(); i++) {
            Uri imageUri = urisToUpload.get(i);
            String fileName = "image_" + System.currentTimeMillis() + "_" + i + ".jpg";
            com.google.firebase.storage.StorageReference ref = storage.getReference()
                    .child("tickets/" + ticketId + "/" + fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            downloadUrls.add(uri.toString());
                            if (uploadCount.incrementAndGet() == urisToUpload.size() && !hasFailed[0]) {
                                ticketData.put("images", downloadUrls);
                                saveTicketToFirestore(ticketId, ticketData);
                            }
                        }).addOnFailureListener(e -> {
                            if (!hasFailed[0]) {
                                hasFailed[0] = true;
                                submitButton.setEnabled(true);
                                submitButton.setText("Submit");
                                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        if (!hasFailed[0]) {
                            hasFailed[0] = true;
                            submitButton.setEnabled(true);
                            submitButton.setText("Submit");
                            Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void handleTicketCreationResponse(Response<CreateTicketResponse> response) {
        submitButton.setEnabled(true);
        submitButton.setText("Submit");

        if (response.isSuccessful() && response.body() != null) {
            CreateTicketResponse ticketResponse = response.body();
            String ticketId = ticketResponse.getTicketId();
            String status = ticketResponse.getStatus();

            // Store ticket for instant display
            CreateTicketResponse.TicketData ticketData = ticketResponse.getTicket();
            if (ticketData != null) {
                TicketListResponse.TicketItem item = TicketListResponse.fromCreateResponse(
                        ticketData, status,
                        ticketData.getStatus() != null ? ticketData.getStatus().getColor() : null);
                if (item != null) {
                    UserTicketsFragment.setPendingNewTicket(item);
                }
            }

            Toast.makeText(ServiceSelectActivity.this, "Ticket created successfully!", Toast.LENGTH_SHORT).show();

            // Navigate to confirmation screen
            Intent intent = new Intent(ServiceSelectActivity.this, TicketConfirmationActivity.class);
            intent.putExtra("ticket_id", ticketId != null ? ticketId : "");
            intent.putExtra("status", status != null ? status : "Pending");
            startActivity(intent);
            finish();
        } else {
            String errorMsg = "Failed to create ticket";
            try {
                if (response.errorBody() != null) {
                    String errBody = response.errorBody().string();
                    Log.e(TAG, "Error response body: " + errBody);
                    
                    // Check if it's HTML (server error page)
                    if (errBody.contains("<!DOCTYPE") || errBody.contains("<html")) {
                        errorMsg = "Server error (HTTP " + response.code() + "). Please check your connection and try again.";
                    } else if (errBody.length() > 200) {
                        errorMsg = errBody.substring(0, 200) + "...";
                    } else {
                        errorMsg = errBody;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading error body", e);
            }
            Log.e(TAG, "Ticket creation failed with code: " + response.code());
            Toast.makeText(ServiceSelectActivity.this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void handleTicketCreationFailure(Throwable t) {
        submitButton.setEnabled(true);
        submitButton.setText("Submit");
        Log.e(TAG, "Ticket creation failed", t);
        Toast.makeText(ServiceSelectActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
}
