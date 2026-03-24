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
import app.hub.api.AboutResponse;
import com.facebook.shimmer.ShimmerFrameLayout;

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
        ApiService apiService = ApiClient.getApiService();
        Call<AboutResponse> call = apiService.getAbout();
        call.enqueue(new Callback<AboutResponse>() {
            @Override
            public void onResponse(@NonNull Call<AboutResponse> call, @NonNull Response<AboutResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AboutResponse.Data data = response.body().getData();
                    if (data != null) {
                        bindAbout(data);
                    }
                }
                setLoading(false);
            }

            @Override
            public void onFailure(@NonNull Call<AboutResponse> call, @NonNull Throwable t) {
                // Keep existing static content
                if (!isAdded()) return;
                setLoading(false);
            }
        });
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
