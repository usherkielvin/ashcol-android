package app.hub.employee;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import app.hub.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmployeeAboutUsFragment extends Fragment {

    private TextView tvAboutTitle;
    private TextView tvAboutBody;
    private TextView tvSupportEmail;
    private TextView tvSupportPhone;
    private TextView tvSupportHours;
    private ShimmerFrameLayout aboutShimmer;
    private View aboutCard;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_about_us, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvAboutTitle = view.findViewById(R.id.tvAboutTitle);
        tvAboutBody = view.findViewById(R.id.tvAboutBody);
        tvSupportEmail = view.findViewById(R.id.tvSupportEmail);
        tvSupportPhone = view.findViewById(R.id.tvSupportPhone);
        tvSupportHours = view.findViewById(R.id.tvSupportHours);
        aboutShimmer = view.findViewById(R.id.aboutShimmer);
        aboutCard = view.findViewById(R.id.aboutCard);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        loadAboutContent();
    }

    private void loadAboutContent() {
        setLoading(true);
        
        FirebaseFirestore.getInstance().collection("settings").document("about")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded()) return;
                
                if (documentSnapshot.exists()) {
                    String title = documentSnapshot.getString("title");
                    String desc = documentSnapshot.getString("description");
                    String email = documentSnapshot.getString("support_email");
                    String phone = documentSnapshot.getString("support_phone");
                    String hours = documentSnapshot.getString("support_hours");
                    
                    if (tvAboutTitle != null && title != null) tvAboutTitle.setText(title);
                    if (tvAboutBody != null && desc != null) tvAboutBody.setText(desc);
                    if (tvSupportEmail != null && email != null) tvSupportEmail.setText("Email: " + email);
                    if (tvSupportPhone != null && phone != null) tvSupportPhone.setText("Phone: " + phone);
                    if (tvSupportHours != null && hours != null) tvSupportHours.setText("Hours: " + hours);
                }
                setLoading(false);
            })
            .addOnFailureListener(e -> {
                if (isAdded()) setLoading(false);
            });
    }

    private void bindAbout(Object data) {
        // No longer used
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void setLoading(boolean isLoading) {
        if (aboutShimmer != null) {
            if (isLoading) {
                aboutShimmer.setVisibility(View.VISIBLE);
                aboutShimmer.startShimmer();
            } else {
                aboutShimmer.stopShimmer();
                aboutShimmer.setVisibility(View.GONE);
            }
        }
        if (aboutCard != null) {
            aboutCard.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }
}
