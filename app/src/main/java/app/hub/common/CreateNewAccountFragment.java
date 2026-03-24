package app.hub.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import app.hub.R;
import app.hub.api.GoogleSignInRequest;
import app.hub.api.GoogleSignInResponse;


public class CreateNewAccountFragment extends Fragment {
	
	private static final String TAG = "CreateNewAccountFragment";
	private GoogleSignInClient googleSignInClient;
	private ActivityResultLauncher<Intent> googleSignInLauncher;
	private boolean hasHandledGoogleResult = false;
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_new_acc, container, false);

		googleSignInLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				this::handleGoogleActivityResult);
		
		// Setup Google Sign-In
		setupGoogleSignIn();
		
		// Setup buttons
		setupButtons(view);
		
		return view;
	}

	private void setupGoogleSignIn() {
		// Configure Google Sign-In
		Log.d(TAG, "Initializing Google Sign-In");
		String serverClientId = getClientIdFromResources();
		GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestEmail()
			.requestProfile();

		if (serverClientId != null && !serverClientId.trim().isEmpty()) {
			Log.d(TAG, "Using client ID for Google Sign-In");
			gsoBuilder.requestIdToken(serverClientId.trim());
		} else {
			Log.w(TAG, "No client ID found; ID token will be null.");
		}

		GoogleSignInOptions gso = gsoBuilder.build();
		
		googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
		Log.d(TAG, "Google Sign-In client initialized");
	}

	private String getClientIdFromResources() {
		int resId = requireContext().getResources().getIdentifier(
				"default_web_client_id",
				"string",
				requireContext().getPackageName());
		if (resId != 0) {
			return getString(resId);
		}
		resId = requireContext().getResources().getIdentifier(
				"server_client_id",
				"string",
				requireContext().getPackageName());
		if (resId != 0) {
			return getString(resId);
		}
		return "";
	}
	
	private void setupButtons(View view) {
		// Back button
		ImageButton backButton = view.findViewById(R.id.closeButton);
		if (backButton != null) {
			backButton.setOnClickListener(v -> {
				if (getActivity() != null) {
					getActivity().finish();
				}
			});
		}
		
		// Continue with Email button
		Button continueWithEmailButton = view.findViewById(R.id.OpenOTP);
		if (continueWithEmailButton != null) {
			continueWithEmailButton.setOnClickListener(v -> {
				RegisterActivity activity = (RegisterActivity) getActivity();
				if (activity != null) {
					activity.showEmailFragment();
				}
			});
		}
		
		// Google Sign-In button
		Button googleButton = view.findViewById(R.id.btnGoogle);
		if (googleButton != null) {
			googleButton.setOnClickListener(v -> {
				signInWithGoogle();
			});
		}
	}
	
	private void signInWithGoogle() {
		if (googleSignInClient == null) {
			Log.w(TAG, "Google Sign-In client is null, cannot launch picker");
			showToast("Google Sign-In not ready. Please try again.");
			return;
		}
		hasHandledGoogleResult = false;
		Log.d(TAG, "Signing out to force account picker");
		googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
			Log.d(TAG, "Launching Google Sign-In picker");
			Intent signInIntent = googleSignInClient.getSignInIntent();
			googleSignInLauncher.launch(signInIntent);
		});
	}

	private void handleGoogleActivityResult(ActivityResult result) {
		Log.d(TAG, "Google Sign-In result received. resultCode=" + result.getResultCode());
		if (result.getResultCode() != android.app.Activity.RESULT_OK) {
			Log.w(TAG, "Google Sign-In cancelled or failed. resultCode=" + result.getResultCode());
			showToast("Sign-in was cancelled");
			return;
		}
		Intent data = result.getData();
		if (data == null) {
			Log.w(TAG, "Google Sign-In returned with no data");
			showToast("Sign-in failed. Please try again.");
			return;
		}
		Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
		handleGoogleSignInResult(task);
	}
	
	private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
		try {
			GoogleSignInAccount account = completedTask.getResult(ApiException.class);
			if (account != null) {
				hasHandledGoogleResult = true;
				Log.d(TAG, "Google Sign-In success. email=" + account.getEmail());
				String email = account.getEmail();
				String displayName = account.getDisplayName();
				String givenName = account.getGivenName();
				String familyName = account.getFamilyName();
				String idToken = account.getIdToken();
				
				Log.d(TAG, "Google Sign-In successful - Email: " + email);
				
				// Send success to parent activity
				RegisterActivity activity = (RegisterActivity) getActivity();
				if (activity != null) {
					activity.setGoogleSignInAccount(account);
					activity.handleGoogleSignInSuccess(email, givenName, familyName, displayName, idToken);
				}
			}
		} catch (ApiException e) {
			Log.e(TAG, "Google Sign-In failed: " + e.getStatusCode(), e);
			String errorMessage = "Google Sign-In failed";
			
			switch (e.getStatusCode()) {
				case 10: // DEVELOPER_ERROR
					errorMessage = "Google Sign-In not configured. Please contact support.";
					break;
				case 12501: // SIGN_IN_CANCELLED
					errorMessage = "Sign-in was cancelled";
					break;
				case 7: // NETWORK_ERROR
					errorMessage = "Network error. Please check your connection.";
					break;
				case 8: // INTERNAL_ERROR
					errorMessage = "Google Sign-In error. Please try again.";
					break;
				default:
					errorMessage = "Google Sign-In failed. Error code: " + e.getStatusCode();
					break;
			}
			showToast(errorMessage);
		}
	}
	
	private void showToast(String message) {
		if (getContext() != null) {
			Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void clearSocialSignInStates() {
		// Clear Google sign-in cache
		if (googleSignInClient != null) {
			googleSignInClient.signOut();
		}
		
		// Clear any cached data in RegisterActivity
		RegisterActivity activity = (RegisterActivity) getActivity();
		if (activity != null) {
			activity.clearAllSignInStates();
		}
	}
}
