package app.hub.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import app.hub.R;
import app.hub.api.AboutResponse;

public class ProfileAboutUsFragment extends Fragment {

    private TextView tvAboutTitle;
    private TextView tvAboutBody;
    private TextView tvSupportEmail;
    private TextView tvSupportPhone;
    private TextView tvSupportHours;
    private TextView tvFacebook;
    private View aboutShimmer;
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
        tvFacebook = view.findViewById(R.id.tvFacebook);
        aboutShimmer = view.findViewById(R.id.aboutShimmer);
        aboutCard = view.findViewById(R.id.aboutCard);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        if (tvFacebook != null) {
            tvFacebook.setPaintFlags(tvFacebook.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvFacebook.setOnClickListener(v -> openFacebookPage());
        }

        loadAboutContent();
    }

    private void loadAboutContent() {
        setLoading(false);
        // Hardcoded content as pure Firebase migration is in progress
        AboutResponse.Data data = new AboutResponse.Data();
        data.setTitle("Ashcol ServiceHub");
        data.setDescription("Your premium service provider for all your technical needs.");
        data.setSupportEmail("support@ashcol.com");
        data.setSupportPhone("+1 234 567 890");
        data.setSupportHours("Mon-Fri 9:00 AM - 6:00 PM");
        bindAbout(data);
    }

    private void bindAbout(AboutResponse.Data data) {
        if (tvAboutTitle != null && data.getTitle() != null) {
            tvAboutTitle.setText(data.getTitle());
        }
        if (tvAboutBody != null && data.getDescription() != null) {
            tvAboutBody.setText(data.getDescription());
        }
        if (tvSupportEmail != null && data.getSupportEmail() != null) {
            tvSupportEmail.setText("Email: " + data.getSupportEmail());
        }
        if (tvSupportPhone != null && data.getSupportPhone() != null) {
            tvSupportPhone.setText("Phone: " + data.getSupportPhone());
        }
        if (tvSupportHours != null && data.getSupportHours() != null) {
            tvSupportHours.setText("Hours: " + data.getSupportHours());
        }
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
            aboutShimmer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (aboutCard != null) {
            aboutCard.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void openFacebookPage() {
        if (getContext() == null) return;
        String url = "https://www.facebook.com/AshcolCorp";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
