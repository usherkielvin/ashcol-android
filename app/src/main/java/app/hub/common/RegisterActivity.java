package app.hub.common;

import app.hub.R;
import app.hub.api.ApiClient;
import app.hub.api.ApiService;

import app.hub.api.GoogleSignInRequest;
import app.hub.api.GoogleSignInResponse;
import app.hub.api.SetInitialPasswordRequest;
import app.hub.api.SetInitialPasswordResponse;
import app.hub.api.RegisterRequest;
import app.hub.api.RegisterResponse;
import app.hub.api.UpdateProfileRequest;
import app.hub.api.UserResponse;
import app.hub.api.VerifyEmailResponse;
import app.hub.user_emailOtp;
import app.hub.util.FCMTokenHelper;
import app.hub.util.TokenManager;
import app.hub.util.UserLocationManager;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ArrayAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Activity for user registration - Multi-step flow container
public class RegisterActivity extends AppCompatActivity {

	private static final String TAG = "RegisterActivity";
	private TokenManager tokenManager;
	private UserLocationManager userLocationManager;
	private FragmentManager fragmentManager;

	// Registration data to pass between steps
	private String userEmail;
	private String userFirstName;
	private String userLastName;
	private String userName;
	private String userPhone;
	private String userLocation;
	private String userPassword;

	// Track if user signed in with Google (skip OTP for Google users)
	private boolean isGoogleSignIn = false;

	// Views for activity_register.xml (Tell us step)
	private View fragmentContainer;
	private ConstraintLayout templateLayout;
	private TextInputEditText firstNameInput, lastNameInput, usernameInput, phoneInput, locationInput;
	private MaterialAutoCompleteTextView regionInput, cityInput;
	private Map<String, List<String>> regionCityMap;
	private TextView fval, lval, uval, phoneVal;
	private MaterialButton registerButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		try {
			tokenManager = new TokenManager(this);
			userLocationManager = new UserLocationManager(this);
			fragmentManager = getSupportFragmentManager();

			// Setup back press handling using OnBackPressedDispatcher
			setupBackPressHandler();

			// If launched with Google account details from MainActivity, jump straight into
			// Google sign-up flow instead of the welcome screen.
			if (savedInstanceState == null && getIntent() != null && getIntent().hasExtra("google_email")) {
				String email = getIntent().getStringExtra("google_email");
				String givenName = getIntent().getStringExtra("google_given_name");
				String familyName = getIntent().getStringExtra("google_family_name");
				String displayName = getIntent().getStringExtra("google_display_name");
				String idToken = getIntent().getStringExtra("google_id_token");

				handleGoogleSignInSuccess(
					email != null ? email : "",
					givenName,
					familyName,
					displayName,
					idToken != null ? idToken : ""
				);
			} else if (savedInstanceState == null) {
				// Default: Step 1 - Create New Account welcome screen
				showCreateNewAccountFragment();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
			// Silent fail – just finish activity, no popup
			finish();
		}
	}

	// Setup modern back press handling
	private void setupBackPressHandler() {
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				// If template layout is visible (Tell Us step), handle back navigation
				if (templateLayout != null && templateLayout.getVisibility() == View.VISIBLE) {
					// For Google users, skip email fragment and go back to CreateNewAccountFragment
					if (isGoogleSignIn) {
						// Clear Google sign-in state so user can select different account
						isGoogleSignIn = false;
						showCreateNewAccountFragment();
					} else {
						// For regular email users, go back to email fragment
						showEmailFragment();
					}
					return;
				}

				// Otherwise handle fragment back stack
				if (fragmentManager.getBackStackEntryCount() > 0) {
					fragmentManager.popBackStack();
				} else {
					// If at first fragment, go back to login
					Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
					startActivity(intent);
					finish();
				}
			}
		};
		getOnBackPressedDispatcher().addCallback(this, callback);
	}

	// Step 1: Show create new account welcome fragment
	public void showCreateNewAccountFragment() {
		try {
			Log.d(TAG, "Showing CreateNewAccountFragment");
			// Hide template layout, show fragment container
			hideTemplateLayout();
			Fragment fragment = new CreateNewAccountFragment();
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.replace(R.id.fragment_container, fragment);
			transaction.commit();
		} catch (Exception e) {
			Log.e(TAG, "Error showing create new account fragment: " + e.getMessage(), e);
		}
	}

	// Step 2: Show email input fragment
	public void showEmailFragment() {
		try {
			Log.d(TAG, "Showing UserAddEmailFragment");
			// Hide template layout, show fragment container
			hideTemplateLayout();
			Fragment fragment = new UserAddEmailFragment();
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.replace(R.id.fragment_container, fragment);
			transaction.addToBackStack("email");
			transaction.commit();
		} catch (Exception e) {
			Log.e(TAG, "Error showing email fragment: " + e.getMessage(), e);
		}
	}

	// Step 3: Show personal info using activity_register.xml directly (Tell us
	// about yourself)
	public void showTellUsFragment() {
		try {
			Log.d(TAG, "Showing Tell Us form using activity_register.xml");
			// Hide fragment container, show template layout
			if (fragmentContainer != null) {
				fragmentContainer.setVisibility(View.GONE);
			}
			if (templateLayout != null) {
				templateLayout.setVisibility(View.VISIBLE);
			}
			initializeTellUsViews();
			setupTellUsValidation();
			setupTellUsButtons();
		} catch (Exception e) {
			Log.e(TAG, "Error showing tell us form: " + e.getMessage(), e);
		}
	}

	// Initialize views from activity_register.xml
	private void initializeTellUsViews() {
		fragmentContainer = findViewById(R.id.fragment_container);
		templateLayout = findViewById(R.id.template_layout);

		firstNameInput = findViewById(R.id.firstNameInput);
		lastNameInput = findViewById(R.id.lastNameInput);
		usernameInput = findViewById(R.id.usernameInput);
		phoneInput = findViewById(R.id.etPhone);
		locationInput = findViewById(R.id.etLocation); // hidden, kept for backward compatibility
		regionInput = findViewById(R.id.etRegion);
		cityInput = findViewById(R.id.etCity);

		registerButton = findViewById(R.id.registerButton);

		fval = findViewById(R.id.fname_val);
		lval = findViewById(R.id.lname_val);
		uval = findViewById(R.id.Uname_val);
		phoneVal = findViewById(R.id.phone_val);

		// Prefill with Google data if available
		if (isGoogleSignIn) {
			if (firstNameInput != null && userFirstName != null && !userFirstName.isEmpty()) {
				firstNameInput.setText(userFirstName);
			}
			if (lastNameInput != null && userLastName != null && !userLastName.isEmpty()) {
				lastNameInput.setText(userLastName);
			}
			if (usernameInput != null && userName != null && !userName.isEmpty()) {
				usernameInput.setText(userName);
			}
		}

		// Setup Region/City Logic
		setupRegionCityLogic();

		// Auto-fill location if detected
		try {
			String detectedCity = tokenManager.getCurrentCity();
			if (detectedCity != null && !detectedCity.isEmpty()) {
				// Legacy field support
				if (locationInput != null) {
					locationInput.setText(detectedCity);
				}
				Log.d(TAG, "Location auto-detected: " + detectedCity);

				// Try to match detected city to a region
				if (regionCityMap != null) {
					for (Map.Entry<String, List<String>> entry : regionCityMap.entrySet()) {
						for (String city : entry.getValue()) {
							if (city.equalsIgnoreCase(detectedCity)) {
								if (regionInput != null)
									regionInput.setText(entry.getKey(), false);
								updateCityAdapter(entry.getKey());
								if (cityInput != null)
									cityInput.setText(city, false);
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error auto-filling location", e);
		}

		// Hide validation messages initially
		if (fval != null)
			fval.setVisibility(View.GONE);
		if (lval != null)
			lval.setVisibility(View.GONE);
		if (uval != null)
			uval.setVisibility(View.GONE);
		if (phoneVal != null)
			phoneVal.setVisibility(View.GONE);

		// After prefill, jump user to the first missing field.
		focusFirstIncompleteTellUsField();
	}

	private void focusFirstIncompleteTellUsField() {
		View target = null;

		if (isEmpty(firstNameInput)) {
			target = firstNameInput;
		} else if (isEmpty(lastNameInput)) {
			target = lastNameInput;
		} else if (isEmpty(usernameInput)) {
			target = usernameInput;
		} else if (isEmpty(phoneInput)) {
			target = phoneInput;
		} else if (isEmpty(regionInput)) {
			target = regionInput;
		} else if (isEmpty(cityInput)) {
			target = cityInput;
		}

		if (target == null) {
			return;
		}

		final View focusTarget = target;
		focusTarget.post(() -> {
			try {
				focusTarget.requestFocus();

				if (focusTarget instanceof TextInputEditText) {
					TextInputEditText editText = (TextInputEditText) focusTarget;
					if (editText.getText() != null) {
						editText.setSelection(editText.getText().length());
					}
				}

				showKeyboard(focusTarget);

				if (focusTarget instanceof MaterialAutoCompleteTextView) {
					((MaterialAutoCompleteTextView) focusTarget).showDropDown();
				}
			} catch (Exception e) {
				Log.w(TAG, "Failed to focus Tell Us field: " + e.getMessage(), e);
			}
		});
	}

	private boolean isEmpty(TextView view) {
		return view == null || view.getText() == null || view.getText().toString().trim().isEmpty();
	}

	private void showKeyboard(View view) {
		try {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		} catch (Exception e) {
			Log.w(TAG, "Failed to show keyboard: " + e.getMessage(), e);
		}
	}

	// Setup validation listeners for Tell Us form
	private void setupTellUsValidation() {
		if (firstNameInput != null) {
			firstNameInput.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					validateFirstName(s.toString());
				}
			});
		}

		if (lastNameInput != null) {
			lastNameInput.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					validateLastName(s.toString());
				}
			});
		}

		if (usernameInput != null) {
			usernameInput.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					validateUsername(s.toString());
				}
			});
		}

		if (phoneInput != null) {
			phoneInput.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					validatePhone(s.toString());
				}
			});
		}
	}

	// Setup buttons for Tell Us form
	private void setupTellUsButtons() {
		ImageButton backButton = findViewById(R.id.backToLoginButton);
		if (backButton != null) {
			backButton.setOnClickListener(v -> {
				// For Google users, skip email fragment and go back to CreateNewAccountFragment
				if (isGoogleSignIn) {
					// Clear sign-in states so user can select different account
					clearAllSignInStates();
					showCreateNewAccountFragment();
				} else {
					// For regular email users, use fragment back stack or go to email fragment
					if (fragmentManager.getBackStackEntryCount() > 0) {
						fragmentManager.popBackStack();
					} else {
						showEmailFragment();
					}
				}
			});
		}

		if (registerButton != null) {
			registerButton.setOnClickListener(v -> {
				if (validateTellUsForm()) {
					savePersonalInfoAndContinue();
				}
			});
		}
	}

	// Validation helper methods
	private void showValidationError(TextView errorView, String message) {
		if (errorView != null) {
			errorView.setText(message);
			errorView.setVisibility(View.VISIBLE);
		}
	}

	private void hideValidationError(TextView errorView) {
		if (errorView != null) {
			errorView.setVisibility(View.GONE);
		}
	}

	private boolean containsNumbers(String text) {
		return text != null && text.matches(".*\\d+.*");
	}

	private int getPhoneDigitCount(String phone) {
		if (phone == null)
			return 0;
		return phone.replaceAll("[^0-9]", "").length();
	}

	// Validation methods
	private void validateFirstName(String firstName) {
		if (fval == null)
			return;

		if (containsNumbers(firstName)) {
			showValidationError(fval, "Name no numbers");
		} else if (firstName != null && firstName.length() < 2) {
			showValidationError(fval, "Name too short");
		} else {
			hideValidationError(fval);
		}
	}

	private void validateLastName(String lastName) {
		if (lval == null)
			return;

		if (containsNumbers(lastName)) {
			showValidationError(lval, "Name no numbers");
		} else {
			hideValidationError(lval);
		}
	}

	private void validateUsername(String username) {
		if (uval == null)
			return;

		if (username == null || username.isEmpty()) {
			hideValidationError(uval);
		} else if (username.contains(" ")) {
			showValidationError(uval, "Username no spaces");
		} else if (username.length() < 4) {
			showValidationError(uval, "Username min 4 chars");
		} else {
			hideValidationError(uval);
		}
	}

	private void validatePhone(String phone) {
		if (phoneVal == null)
			return;

		if (phone == null || phone.isEmpty()) {
			hideValidationError(phoneVal);
			return;
		}

		// Check if phone contains only numbers and + symbol
		if (!phone.matches("[0-9+]+")) {
			showValidationError(phoneVal, "Only numbers and + allowed");
			return;
		}

		int digitCount = getPhoneDigitCount(phone);
		// Allow up to 12 digits (not exactly 10)
		if (digitCount > 0 && digitCount <= 12) {
			hideValidationError(phoneVal);
		} else {
			showValidationError(phoneVal, "Maximum 12 digits allowed");
		}
	}

	// Validate all fields in Tell Us form
	private boolean validateTellUsForm() {
		String firstName = getText(firstNameInput);
		String lastName = getText(lastNameInput);
		String username = getText(usernameInput);
		String phone = getText(phoneInput);

		boolean isValid = true;

		if (firstName.isEmpty()) {
			showValidationError(fval, "First name required");
			isValid = false;
		}

		if (lastName.isEmpty()) {
			showValidationError(lval, "Last name required");
			isValid = false;
		}

		if (username.isEmpty()) {
			showValidationError(uval, "Username required");
			isValid = false;
		} else if (username.length() < 4) {
			showValidationError(uval, "Username min 4 chars");
			isValid = false;
		}

		if (phone.isEmpty()) {
			showValidationError(phoneVal, "Phone required");
			isValid = false;
		} else if (!phone.matches("[0-9+]+")) {
			showValidationError(phoneVal, "Only numbers and + allowed");
			isValid = false;
		} else if (getPhoneDigitCount(phone) > 12) {
			showValidationError(phoneVal, "Maximum 12 digits allowed");
			isValid = false;
		}

		if (getText(regionInput).isEmpty()) {
			regionInput.setError("Required");
			isValid = false;
		}
		if (getText(cityInput).isEmpty()) {
			cityInput.setError("Required");
			isValid = false;
		}

		return isValid;
	}

	// Save personal info and continue to password step
	private void savePersonalInfoAndContinue() {
		try {
			// Build a simple "Region, City" location string for backend auto-branching
			String region = getText(regionInput);
			String city = getText(cityInput);
			String combinedLocation = "";
			if (!region.isEmpty() && !city.isEmpty()) {
				combinedLocation = region + ", " + city;
			} else if (!city.isEmpty()) {
				combinedLocation = city;
			} else if (!region.isEmpty()) {
				combinedLocation = region;
			}

			// Get phone number and add +63 prefix
			String phoneNumber = getText(phoneInput);
			String fullPhoneNumber = "+63" + phoneNumber;

			setUserPersonalInfo(
					getText(firstNameInput),
					getText(lastNameInput),
					getText(usernameInput),
					fullPhoneNumber,
					combinedLocation);
		} catch (Exception e) {
			Log.e(TAG, "Error saving personal info", e);
			// Continue anyway with empty location
			String phoneNumber = getText(phoneInput);
			String fullPhoneNumber = "+63" + phoneNumber;
			setUserPersonalInfo(
					getText(firstNameInput),
					getText(lastNameInput),
					getText(usernameInput),
					fullPhoneNumber,
					"");
		}

		// If Google Sign-In user, register/login with backend immediately
		if (isGoogleSignIn) {
			registerGoogleUser();
			return;
		} else {
			// Regular email user - proceed to OTP verification
			showCreatePasswordFragment();
		}
	}

	// Store Google ID token
	private String googleIdToken;

	// Check if user signed in with Google
	public boolean isGoogleSignInUser() {
		return isGoogleSignIn;
	}

	// Update password for Google user (set initial password)
	public void updateGoogleUserPassword(String password, String confirmPassword) {
		String token = tokenManager.getToken();
		if (token == null || token.isEmpty()) {
			Log.w(TAG, "updateGoogleUserPassword: missing auth token");
			return;
		}

		Log.d(TAG, "Updating password for Google user");

		ApiService apiService = ApiClient.getApiService();
		SetInitialPasswordRequest request = new SetInitialPasswordRequest(password, confirmPassword);

		Call<SetInitialPasswordResponse> call = apiService.setInitialPassword(token, request);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(@NonNull Call<SetInitialPasswordResponse> call,
					@NonNull Response<SetInitialPasswordResponse> response) {
				if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
					Log.d(TAG, "Password updated successfully for Google user");

					// Navigate to Account Created
					showAccountCreatedFragment();
				} else {
					String errorMsg = "Failed to set password. Please try again.";
					if (response.body() != null && response.body().getMessage() != null) {
						errorMsg = response.body().getMessage();
					}
					Log.e(TAG, "setInitialPassword failed: " + errorMsg);
				}
			}

			@Override
			public void onFailure(@NonNull Call<SetInitialPasswordResponse> call, @NonNull Throwable t) {
				Log.e(TAG, "Error updating password: " + t.getMessage(), t);
			}
		});
	}

	// Register/login Google user
	private void registerGoogleUser() {
		// Ensure we have required data
		String email = getUserEmail();
		String firstName = getUserFirstName();
		String lastName = getUserLastName();
		String phone = getUserPhone();
		String location = getUserLocation();

		// Extract region and city from views directly if available, or try to parse
		// from location string
		String region = "";
		String city = "";

		if (regionInput != null && cityInput != null) {
			region = getText(regionInput);
			city = getText(cityInput);
		}

		// Fallback: if inputs are empty but we have a location string, try to split it
		if ((region.isEmpty() || city.isEmpty()) && location != null && location.contains(",")) {
			String[] parts = location.split(",", 2);
			if (parts.length == 2) {
				if (region.isEmpty())
					region = parts[0].trim();
				if (city.isEmpty())
					city = parts[1].trim();
			}
		}

		if (email == null || email.isEmpty()) {
			Log.w(TAG, "Email is required for Google registration");
			return;
		}

		Log.d(TAG, "Registering Google user with backend - Email: " + email);
		Log.d(TAG, "First Name: " + firstName + ", Last Name: " + lastName + ", Phone: " + phone);
		Log.d(TAG, "Location: " + location + ", Region: " + region + ", City: " + city);

		// Now actually register the user with the backend using the REGISTRATION
		// endpoint
		ApiService apiService = ApiClient.getApiService();
		String idToken = googleIdToken != null && !googleIdToken.isEmpty() ? googleIdToken : "";

		// Use the updated constructor with location fields
		GoogleSignInRequest request = new GoogleSignInRequest(
				idToken,
				email,
				firstName != null ? firstName : "",
				lastName != null ? lastName : "",
				phone != null ? phone : "",
				region,
				city,
				location != null ? location : "");

		Log.d(TAG, "Sending Google registration request - Email: " + email + ", First: " + firstName + ", Last: "
				+ lastName + ", Phone: " + phone);

		// Use the REGISTRATION endpoint for new users
		Call<GoogleSignInResponse> call = apiService.googleRegister(request);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(@NonNull Call<GoogleSignInResponse> call,
					@NonNull Response<GoogleSignInResponse> response) {
				if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
					handleGoogleRegistrationSuccess(response.body());
				} else {
					// HTTP 409 from backend means account already exists → send user to login
					if (response.code() == 409) {
						Log.w(TAG, "Google registration: account already exists, redirecting to login");
						navigateToLoginFromGoogleConflict();
						return;
					}

					// Log response body for debugging
					String errorBody = "";
					try {
						if (response.errorBody() != null) {
							errorBody = response.errorBody().string();
							Log.e(TAG, "Google registration error response: " + errorBody);
						}
					} catch (Exception e) {
						Log.e(TAG, "Error reading error response", e);
					}
					handleGoogleRegistrationError(response.code(), errorBody);
				}
			}

			@Override
			public void onFailure(@NonNull Call<GoogleSignInResponse> call, @NonNull Throwable t) {
				Log.e(TAG, "Error registering Google user: " + t.getMessage(), t);
			}
		});
	}

	private void checkGoogleAccountExistsForRegistration(String email, String firstName, String lastName,
			String phone) {
		ApiService apiService = ApiClient.getApiService();
		// id_token may be null if not configured, that's okay
		String idToken = googleIdToken != null && !googleIdToken.isEmpty() ? googleIdToken : "";

		// For check existence, we don't need location data, so pass nulls/empty
		GoogleSignInRequest request = new GoogleSignInRequest(
				idToken,
				email,
				firstName != null ? firstName : "",
				lastName != null ? lastName : "",
				phone != null ? phone : "");

		Log.d(TAG, "Checking Google account existence - Email: " + email + ", First: " + firstName + ", Last: "
				+ lastName + ", Phone: " + phone);

		// Use the SIGN-IN endpoint to check if account exists
		Call<GoogleSignInResponse> call = apiService.googleSignIn(request);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(@NonNull Call<GoogleSignInResponse> call,
					@NonNull Response<GoogleSignInResponse> response) {
				if (response.isSuccessful() && response.body() != null) {
					GoogleSignInResponse signInResponse = response.body();
					if (signInResponse.isSuccess()) {
						// Account already exists - log user in automatically
						Log.d(TAG, "Google account exists, logging user in");
						handleGoogleLoginSuccess(signInResponse);
					} else {
						// Account doesn't exist - proceed with registration
						proceedWithGoogleRegistration(email, firstName, lastName, phone, idToken);
					}
				} else if (response.code() == 404) {
					// 404 means account doesn't exist - proceed with registration
					Log.d(TAG, "Google account doesn't exist (404), proceeding with registration");
					proceedWithGoogleRegistration(email, firstName, lastName, phone, idToken);
				} else {
					// Handle other error responses
					Log.w(TAG, "Google account check failed with code " + response.code() + ", proceeding with registration");
					proceedWithGoogleRegistration(email, firstName, lastName, phone, idToken);
				}
			}

			@Override
			public void onFailure(@NonNull Call<GoogleSignInResponse> call, @NonNull Throwable t) {
				Log.e(TAG, "Error checking Google account existence: " + t.getMessage(), t);
				runOnUiThread(() -> {
					Toast.makeText(RegisterActivity.this, 
						"Network error. Please check your connection and try again.", 
						Toast.LENGTH_LONG).show();
				});
			}
		});
	}

	private void proceedWithGoogleRegistration(String email, String firstName, String lastName, String phone,
			String idToken) {
		Log.d(TAG, "Account doesn't exist, proceeding to Tell Us form");

		// Navigate to "Tell Us" to collect phone number and other details
		Log.d(TAG, "Prompting user for phone number after Google registration");
		showTellUsFragment();
	}

	// Handle successful Google login (account already exists)
	private void handleGoogleLoginSuccess(GoogleSignInResponse response) {
		Log.d(TAG, "handleGoogleLoginSuccess called in RegisterActivity");
		
		if (response.getData() == null || response.getData().getUser() == null) {
			Log.e(TAG, "Google login failed: missing user data");
			runOnUiThread(() -> {
				Toast.makeText(RegisterActivity.this, 
					"Login failed. Please try again.", 
					Toast.LENGTH_SHORT).show();
			});
			return;
		}

		// Save user data and token
		GoogleSignInResponse.User user = response.getData().getUser();
		Log.d(TAG, "Saving user data - Email: " + user.getEmail() + ", Role: " + user.getRole());
		
		tokenManager.saveToken("Bearer " + response.getData().getToken());
		tokenManager.saveUserId(user.getId());
		tokenManager.saveEmail(user.getEmail());
		tokenManager.saveRole(user.getRole()); // Added role saving

		// Build and save name
		String firstName = user.getFirstName();
		String lastName = user.getLastName();
		StringBuilder nameBuilder = new StringBuilder();
		if (firstName != null && !firstName.trim().isEmpty()) {
			nameBuilder.append(firstName.trim());
		}
		if (lastName != null && !lastName.trim().isEmpty()) {
			if (nameBuilder.length() > 0) {
				nameBuilder.append(" ");
			}
			nameBuilder.append(lastName.trim());
		}
		String fullName = nameBuilder.toString();
		if (!fullName.isEmpty()) {
			tokenManager.saveName(fullName);
			Log.d(TAG, "Saved name to cache: " + fullName);
		}

		// Force immediate token persistence
		tokenManager.forceCommit();

		// Register FCM token for push notifications
		FCMTokenHelper.registerTokenWithBackend(RegisterActivity.this);

		Log.d(TAG, "Navigating to dashboard for role: " + user.getRole());
		// Navigate to appropriate dashboard based on user role
		navigateToUserDashboard(user.getRole());
	}

	// Navigate to appropriate dashboard based on user role
	private void navigateToUserDashboard(String role) {
		Intent intent;

		if (role == null) {
			Log.w(TAG, "User role is null, defaulting to customer");
			role = "customer"; // Default role
		}

		switch (role.toLowerCase()) {
			case "admin":
				intent = new Intent(this, app.hub.admin.AdminDashboardActivity.class);
				break;
			case "manager":
				intent = new Intent(this, app.hub.manager.ManagerDashboardActivity.class);
				break;
			case "technician", "employee":
				intent = new Intent(this, app.hub.employee.EmployeeDashboardActivity.class);
				break;
			case "customer":
			default:
				intent = new Intent(this, app.hub.user.DashboardActivity.class);
				break;
		}

		startActivity(intent);
		finish();
	}

	// Clear all sign-in states (called from CreateNewAccountFragment)
	public void clearAllSignInStates() {
		clearGoogleSignInState();
	}

	// Clear Google sign-in state to allow user to select different account
	private void clearGoogleSignInState() {
		isGoogleSignIn = false;
		googleIdToken = null;
		userEmail = null;
		userFirstName = null;
		userLastName = null;
		userName = null;
		userPhone = null;
		userLocation = null;
		userPassword = null;
	}

	// Handle successful Google registration
	private void handleGoogleRegistrationSuccess(GoogleSignInResponse response) {
		if (response.getData() == null || response.getData().getUser() == null) {
			Log.e(TAG, "Google registration success response missing user data");
			return;
		}

		// Save user data and token
		GoogleSignInResponse.User user = response.getData().getUser();
		tokenManager.saveToken("Bearer " + response.getData().getToken());
		tokenManager.saveUserId(user.getId());
		tokenManager.saveEmail(user.getEmail());

		// Build and save name
		String firstName = user.getFirstName();
		String lastName = user.getLastName();
		StringBuilder nameBuilder = new StringBuilder();
		if (firstName != null && !firstName.trim().isEmpty()) {
			nameBuilder.append(firstName.trim());
		}
		if (lastName != null && !lastName.trim().isEmpty()) {
			if (nameBuilder.length() > 0) {
				nameBuilder.append(" ");
			}
			nameBuilder.append(lastName.trim());
		}
		String fullName = nameBuilder.toString();
		if (!fullName.isEmpty()) {
			tokenManager.saveName(fullName);
			Log.d(TAG, "Saved name to cache: " + fullName);
		}

		// Force immediate token persistence
		tokenManager.forceCommit();

		// Navigate to password creation (Google users still need to set a password)
		if (templateLayout != null) {
			templateLayout.setVisibility(View.GONE);
		}
		if (fragmentContainer != null) {
			fragmentContainer.setVisibility(View.VISIBLE);
		}
		showCreatePasswordFragment();
	}

	// Handle Google registration error
	private void handleGoogleRegistrationError(int statusCode, String errorBody) {
		String errorMsg;
		if (statusCode == 422) {
			errorMsg = "Invalid data for Google registration.";
			if (errorBody != null && !errorBody.isEmpty()) {
				Log.e(TAG, "Google registration validation errors: " + errorBody);
			}
		} else if (statusCode == 500) {
			errorMsg = "Server error during Google registration.";
		} else {
			errorMsg = "Google registration failed (" + statusCode + ").";
		}
		Log.e(TAG, errorMsg + " body=" + errorBody);
	}

	/**
	 * When backend says Google account already exists (HTTP 409), send user back to
	 * login screen.
	 */
	private void navigateToLoginFromGoogleConflict() {
		try {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
		} catch (Exception e) {
			Log.e(TAG, "Error navigating to login from Google conflict: " + e.getMessage(), e);
		}
	}

	private String getText(TextView editText) {
		if (editText == null || editText.getText() == null) {
			return "";
		}
		return editText.getText().toString().trim();
	}

	// Step 4: Show password creation fragment
	public void showCreatePasswordFragment() {
		try {
			Log.d(TAG, "Showing UserCreatePasswordFragment");
			// Hide template layout, show fragment container
			hideTemplateLayout();
			Fragment fragment = new UserCreatePasswordFragment();
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.replace(R.id.fragment_container, fragment);
			transaction.addToBackStack("password");
			transaction.commit();
		} catch (Exception e) {
			Log.e(TAG, "Error showing password fragment: " + e.getMessage(), e);
			Log.e(TAG, "Error showing password fragment: " + e.getMessage(), e);
		}
	}

	// Helper method to hide template layout and show fragment container
	private void hideTemplateLayout() {
		if (fragmentContainer == null) {
			fragmentContainer = findViewById(R.id.fragment_container);
		}
		if (templateLayout == null) {
			templateLayout = findViewById(R.id.template_layout);
		}
		if (fragmentContainer != null) {
			fragmentContainer.setVisibility(View.VISIBLE);
		}
		if (templateLayout != null) {
			templateLayout.setVisibility(View.GONE);
		}
	}

	private void setupRegionCityLogic() {
		regionCityMap = new HashMap<>();

		// Populate Regions and Cities - RESTRICTED TO AVAILABLE BRANCH LOCATIONS

		// 1. NCR - All cities in Metro Manila
		regionCityMap.put("NCR", Arrays.asList(
				"Caloocan City",
				"Las Piñas City",
				"Makati City",
				"Malabon City",
				"Mandaluyong City",
				"Manila City",
				"Marikina City",
				"Muntinlupa City",
				"Navotas City",
				"Parañaque City",
				"Pasay City",
				"Pasig City",
				"Pateros",
				"Quezon City",
				"San Juan City",
				"Taguig City",
				"Valenzuela City"));

		// 2. Calabarzon (Rizal, Cavite, Laguna, Batangas)
		regionCityMap.put("Calabarzon", Arrays.asList(
				"Rodriguez",
				"General Trias",
				"Dasmariñas",
				"Santa Rosa",
				"Santa Cruz",
				"Batangas City"));

		// 3. Central Luzon (Pampanga, Bulacan)
		regionCityMap.put("Central Luzon", Arrays.asList(
				"San Fernando",
				"Malolos City"));

		// Setup Region Adapter
		List<String> regions = new ArrayList<>(regionCityMap.keySet());
		ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
				regions);
		regionInput.setAdapter(regionAdapter);

		// Region Selection Listener
		regionInput.setOnItemClickListener((parent, view, position, id) -> {
			String selectedRegion = parent.getItemAtPosition(position).toString();
			updateCityAdapter(selectedRegion);
		});

		// Disable keyboard input for Region/City
		regionInput.setKeyListener(null);
		cityInput.setKeyListener(null);
	}

	private void updateCityAdapter(String region) {
		List<String> cities = regionCityMap.get(region);
		if (cities != null) {
			ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
					cities);
			cityInput.setAdapter(cityAdapter);
			cityInput.setText(""); // Clear previous selection
		}
	}

	// Step 5: Show OTP verification (Almost there)
	public void showOtpVerification() {
		// Skip OTP for Google Sign-In users
		if (isGoogleSignIn) {
			Log.d(TAG, "Skipping OTP for social Sign-In user");
			showAccountCreatedFragment();
			return;
		}

		// Get email from stored data (from email fragment)
		String email = getUserEmail();
		if (email == null || email.isEmpty()) {
			Log.e(TAG, "Email not found when starting OTP. Finishing registration.");
			finish();
			return;
		}

		// Show OTP fragment instead of dialog
		try {
			Log.d(TAG, "Showing user_emailOtp fragment");
			// Hide template layout, show fragment container
			hideTemplateLayout();
			Fragment fragment = new user_emailOtp();
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.replace(R.id.fragment_container, fragment);
			transaction.addToBackStack("otp_verification");
			transaction.commit();
		} catch (Exception e) {
			Log.e(TAG, "Error showing OTP fragment: " + e.getMessage(), e);
		}
	}

	// Step 6: Show Account Created fragment
	public void showAccountCreatedFragment() {
		try {
			Log.d(TAG, "Showing AccountCreatedFragment");

			// Update location for newly registered user
			updateLocationForNewUser();

			// Hide template layout, show fragment container
			hideTemplateLayout();
			Fragment fragment = new AccountCreatedFragment();
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.replace(R.id.fragment_container, fragment);
			transaction.addToBackStack("account_created");
			transaction.commit();
		} catch (Exception e) {
			Log.e(TAG, "Error showing account created fragment: " + e.getMessage(), e);
		}
	}

	// Handle successful OTP verification (called from fragment)
	public void handleOtpVerificationSuccess(VerifyEmailResponse response) {
		if (!response.isSuccess() || response.getData() == null) {
			String errorMsg = response.getMessage() != null ? response.getMessage() : "Invalid verification code";
			Log.w(TAG, "OTP verification failed: " + errorMsg);
			return;
		}

		// Check if user exists (for resend scenario) or is being created (registration)
		VerifyEmailResponse.User user = response.getData().getUser();

		if (user != null) {
			// User exists - save user data and token
			tokenManager.saveToken("Bearer " + response.getData().getToken());
			tokenManager.saveUserId(user.getId());
			tokenManager.saveEmail(user.getEmail());

			// Build and save name
			String firstName = user.getFirstName();
			String lastName = user.getLastName();
			StringBuilder nameBuilder = new StringBuilder();
			if (firstName != null && !firstName.trim().isEmpty()) {
				nameBuilder.append(firstName.trim());
			}
			if (lastName != null && !lastName.trim().isEmpty()) {
				if (nameBuilder.length() > 0) {
					nameBuilder.append(" ");
				}
				nameBuilder.append(lastName.trim());
			}
			String fullName = nameBuilder.toString();
			if (!fullName.isEmpty()) {
				tokenManager.saveName(fullName);
				Log.d(TAG, "Saved name to cache: " + fullName);
			}
		} else {
			// User doesn't exist yet (registration flow) - OTP verified, now create account
			Log.d(TAG, "OTP verified for registration, creating account...");
			createAccountAfterOtpVerification();
			return; // Don't navigate yet, wait for account creation
		}

		// Navigate to Account Created fragment
		showAccountCreatedFragment();
	}

	// Create account after OTP verification
	private void createAccountAfterOtpVerification() {
		// Get all collected user data
		String email = getUserEmail();
		String firstName = getUserFirstName();
		String lastName = getUserLastName();
		String username = getUserName();
		String password = getUserPassword();
		String phone = getUserPhone() != null ? getUserPhone() : "";
		String location = getUserLocation() != null ? getUserLocation() : "";

		// Validate required fields
		if (email == null || email.isEmpty()) {
			Log.w(TAG, "createAccountAfterOtpVerification: missing email");
			return;
		}
		if (firstName == null || firstName.isEmpty()) {
			Log.w(TAG, "createAccountAfterOtpVerification: missing first name");
			return;
		}
		if (lastName == null || lastName.isEmpty()) {
			Log.w(TAG, "createAccountAfterOtpVerification: missing last name");
			return;
		}
		if (username == null || username.isEmpty()) {
			Log.w(TAG, "createAccountAfterOtpVerification: missing username");
			return;
		}
		if (password == null || password.isEmpty()) {
			Log.w(TAG, "createAccountAfterOtpVerification: missing password");
			return;
		}

		// Default role to "customer" for regular registration
		String role = "customer";

		Log.d(TAG, "Creating account with - Email: " + email + ", Username: " + username + ", Role: " + role
				+ ", Location: " + location);

		ApiService apiService = ApiClient.getApiService();
		RegisterRequest request = new RegisterRequest(username, firstName, lastName, email, phone, location, password,
				password, role);

		Call<RegisterResponse> call = apiService.register(request);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
				if (response.isSuccessful() && response.body() != null) {
					RegisterResponse body = response.body();
					if (body.isSuccess() && body.getData() != null) {
						// Account created successfully
						RegisterResponse.User user = body.getData().getUser();
						String token = body.getData().getToken();

						// Save user data and token
						if (token != null) {
							tokenManager.saveToken("Bearer " + token);
						}
						if (user != null) {
							tokenManager.saveUserId(user.getId());
						}
						tokenManager.saveEmail(user != null ? user.getEmail() : email);

						// Build and save name
						if (user != null) {
							String userFirstName = user.getFirstName();
							String userLastName = user.getLastName();
							StringBuilder nameBuilder = new StringBuilder();
							if (userFirstName != null && !userFirstName.trim().isEmpty()) {
								nameBuilder.append(userFirstName.trim());
							}
							if (userLastName != null && !userLastName.trim().isEmpty()) {
								if (nameBuilder.length() > 0) {
									nameBuilder.append(" ");
								}
								nameBuilder.append(userLastName.trim());
							}
							String fullName = nameBuilder.toString();
							if (!fullName.isEmpty()) {
								tokenManager.saveName(fullName);
								Log.d(TAG, "Saved name to cache: " + fullName);
							}
						}

						// Force immediate token persistence
						tokenManager.forceCommit();

						Log.d(TAG, "Account created successfully");
						// Navigate to Account Created fragment
						showAccountCreatedFragment();
					} else {
						// Registration failed
						String errorMsg = body.getMessage() != null ? body.getMessage() : "Failed to create account";
						Log.e(TAG, "Account creation failed: " + errorMsg);
						Log.e(TAG, "Account creation failed (API error): " + errorMsg);
					}
				} else {
					// Response not successful
					String errorMsg = "Failed to create account. Please try again.";
					try {
						if (response.errorBody() != null) {
							com.google.gson.Gson gson = new com.google.gson.Gson();
							java.io.BufferedReader reader = new java.io.BufferedReader(
									new java.io.InputStreamReader(response.errorBody().byteStream()));
							String errorJson = reader.readLine();
							if (errorJson != null) {
								RegisterResponse errorResponse = gson.fromJson(errorJson, RegisterResponse.class);
								if (errorResponse != null && errorResponse.getMessage() != null) {
									errorMsg = errorResponse.getMessage();
								}
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
					}
					Log.e(TAG, "Account creation failed with status: " + response.code() + ", message: " + errorMsg);
					Log.e(TAG, "Account creation failed: " + errorMsg);
				}
			}

			@Override
			public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
				Log.e(TAG, "Error creating account: " + t.getMessage(), t);
			}
		});
	}

	// Setters for registration data (called by fragments)
	public void setUserEmail(String email) {
		this.userEmail = email;
		Log.d(TAG, "setUserEmail called with: " + (email != null ? email : "NULL"));
		Log.d(TAG, "userEmail field is now: " + (this.userEmail != null ? this.userEmail : "NULL"));
	}

	public void setUserPersonalInfo(String firstName, String lastName, String username, String phone, String location) {
		this.userFirstName = firstName;
		this.userLastName = lastName;
		this.userName = username;
		this.userPhone = phone;
		this.userLocation = location;
		Log.d(TAG, "Personal info set - Name: " + firstName + " " + lastName + ", Location: " + location);
	}

	public void setUserPassword(String password) {
		this.userPassword = password;
		Log.d(TAG, "Password set");
	}

	// Getters for registration data (used by fragments - may be used in future
	// fragments)
	@SuppressWarnings("unused")
	public String getUserEmail() {
		Log.d(TAG, "getUserEmail() called, returning: " + (userEmail != null ? userEmail : "NULL"));
		return userEmail;
	}

	@SuppressWarnings("unused")
	public String getUserFirstName() {
		return userFirstName;
	}

	@SuppressWarnings("unused")
	public String getUserLastName() {
		return userLastName;
	}

	@SuppressWarnings("unused")
	public String getUserName() {
		return userName;
	}

	@SuppressWarnings("unused")
	public String getUserPhone() {
		return userPhone;
	}

	@SuppressWarnings("unused")
	public String getUserLocation() {
		return userLocation;
	}

	@SuppressWarnings("unused")
	public String getUserPassword() {
		return userPassword;
	}

	// Handle Google Sign-In success
	public void handleGoogleSignInSuccess(String email, String givenName, String familyName,
			String displayName, String idToken) {
		Log.d(TAG, "handleGoogleSignInSuccess called - Email: " + email);

		// Mark as Google Sign-In user (skip OTP)
		isGoogleSignIn = true;

		// Store Google account data
		setUserEmail(email);

		// Use Google name if available, otherwise use display name
		String firstName = givenName != null && !givenName.isEmpty() ? givenName
				: (displayName != null && displayName.contains(" ") ? displayName.split(" ")[0] : displayName);
		String lastName = familyName != null && !familyName.isEmpty() ? familyName
				: (displayName != null && displayName.contains(" ")
						? displayName.substring(displayName.indexOf(" ") + 1)
						: "");

		// Generate username from email (before @)
		String username = email != null && email.contains("@") ? email.substring(0, email.indexOf("@"))
				: "user_" + System.currentTimeMillis();

		// Store personal info from Google
		setUserPersonalInfo(firstName, lastName, username, "", "");

		// Store Google ID token for backend API call
		googleIdToken = idToken;

		// Go straight to Tell Us form for Google sign-up
		showTellUsFragment();
	}

	/**
	 * Update location for newly registered user
	 */
	private void updateLocationForNewUser() {
		if (!tokenManager.isLoggedIn()) {
			Log.d(TAG, "User not logged in, skipping location update");
			return;
		}

		userLocationManager.updateUserLocation(new UserLocationManager.LocationUpdateCallback() {
			@Override
			public void onLocationUpdated(String location) {
				Log.d(TAG, "Location updated successfully for new user: " + location);
			}

			@Override
			public void onLocationUpdateFailed(String error) {
				Log.e(TAG, "Location update failed for new user: " + error);
				// Don't show error to user, just log it
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (userLocationManager != null) {
			userLocationManager.cleanup();
		}
	}
}
