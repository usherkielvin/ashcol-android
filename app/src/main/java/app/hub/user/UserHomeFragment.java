package app.hub.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.hub.R;
import app.hub.api.TicketListResponse;
import app.hub.api.UserResponse;
import app.hub.util.TokenManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserHomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserHomeFragment extends Fragment {

    private static final String TAG = "UserHomeFragment";
    private ViewPager2 bannerViewPager;
    private BannerAdapter bannerAdapter;
    private LinearLayout dotsLayout;
    private Handler handler;
    private Runnable slideRunnable;
    private List<Integer> images = Arrays.asList(R.drawable.keepcoolallyear, R.drawable.nohiddenfees,
            R.drawable.professionalacinstall);
    private TextView tvAssignedBranch;
    private TokenManager tokenManager;
    
    // Service booking buttons
    private MaterialButton btnBookCleaning, btnBookMaintenance, btnBookInstallation, btnBookRepair;
    
    // Ticket count TextViews
    private TextView tvNewTicketsCount, tvPendingTicketsCount, tvCompletedTicketsCount;

    public UserHomeFragment() {
        // Required empty public constructor
    }

    public static UserHomeFragment newInstance(String param1, String param2) {
        UserHomeFragment fragment = new UserHomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private app.hub.common.FirestoreManager firestoreManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new app.hub.common.FirestoreManager(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user__home, container, false);

        tokenManager = new TokenManager(requireContext());
        if (firestoreManager == null) {
            firestoreManager = new app.hub.common.FirestoreManager(requireContext());
        }
        
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        dotsLayout = view.findViewById(R.id.dotsLayout);
        tvAssignedBranch = view.findViewById(R.id.tvAssignedBranch);
        
        // Initialize service booking buttons
        btnBookCleaning = view.findViewById(R.id.btnBookCleaning);
        btnBookMaintenance = view.findViewById(R.id.btnBookMaintenance);
        btnBookInstallation = view.findViewById(R.id.btnBookInstallation);
        btnBookRepair = view.findViewById(R.id.btnBookRepair);
        
        // Initialize ticket count TextViews
        tvNewTicketsCount = view.findViewById(R.id.tvNewTicketsCount);
        tvPendingTicketsCount = view.findViewById(R.id.tvPendingTicketsCount);
        tvCompletedTicketsCount = view.findViewById(R.id.tvCompletedTicketsCount);

        setupViewPager();
        updateDots(0); // Initialize dots
        loadBranchInfo();
        setupServiceButtonListeners();
        loadTicketCounts();

        return view;
    }

    private void setupViewPager() {
        bannerAdapter = new BannerAdapter(images);
        bannerViewPager.setAdapter(bannerAdapter);

        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);
                restartAutoSlide();
            }
        });

        handler = new Handler(Looper.getMainLooper());
        slideRunnable = new Runnable() {
            @Override
            public void run() {
                int next = (bannerViewPager.getCurrentItem() + 1) % images.size();
                bannerViewPager.setCurrentItem(next);
                handler.postDelayed(this, 4000); // 4 seconds
            }
        };
        handler.postDelayed(slideRunnable, 4000);
    }

    private void updateDots(int position) {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            View dot = dotsLayout.getChildAt(i);
            if (i == position) {
                dot.setBackgroundResource(R.drawable.dot_selected);
                // Adjust width for selected dot if needed, but here it's handled by drawable or
                // fixed in XML
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
                params.width = (int) (24 * getResources().getDisplayMetrics().density);
                params.height = (int) (8 * getResources().getDisplayMetrics().density);
                dot.setLayoutParams(params);
            } else {
                dot.setBackgroundResource(R.drawable.dot_unselected);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
                params.width = (int) (8 * getResources().getDisplayMetrics().density);
                params.height = (int) (8 * getResources().getDisplayMetrics().density);
                dot.setLayoutParams(params);
            }
        }
    }

    private void restartAutoSlide() {
        if (handler != null && slideRunnable != null) {
            handler.removeCallbacks(slideRunnable);
            handler.postDelayed(slideRunnable, 4000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null && slideRunnable != null) {
            handler.removeCallbacks(slideRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler != null && slideRunnable != null) {
            handler.postDelayed(slideRunnable, 4000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && slideRunnable != null) {
            handler.removeCallbacks(slideRunnable);
        }
        if (firestoreManager != null) {
            firestoreManager.stopListening();
            firestoreManager.stopTicketListening();
        }
    }

    private void loadBranchInfo() {
        // First try to load from cache
        String cachedLocation = tokenManager.getCurrentCity();
        if (cachedLocation != null && !cachedLocation.isEmpty()) {
            displayLocation(cachedLocation);
        }

        // Then fetch fresh data from API
        fetchUserData();
    }

    private void fetchUserData() {
        firestoreManager.listenToUserProfile(new app.hub.common.FirestoreManager.UserProfileListener() {
            @Override
            public void onProfileUpdated(UserResponse.Data profile) {
                if (profile != null) {
                    String location = buildLocationText(profile.getCity(), profile.getRegion());
                    if (location == null || location.isEmpty()) {
                        location = profile.getBranch();
                    }

                    if (location != null && !location.isEmpty()) {
                        if (getActivity() != null) {
                            tokenManager.saveCurrentCity(location);
                            displayLocation(location);
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to fetch user data from Firestore: " + e.getMessage());
            }
        });
    }

    private void displayLocation(String location) {
        if (tvAssignedBranch != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (tvAssignedBranch != null) {
                    tvAssignedBranch.setText(location);
                    tvAssignedBranch.setVisibility(View.VISIBLE);
                }
            });
        }
    }
    
    private void setupServiceButtonListeners() {
        if (btnBookCleaning != null) {
            btnBookCleaning.setOnClickListener(v -> openServiceRequest("Cleaning"));
        }
        
        if (btnBookMaintenance != null) {
            btnBookMaintenance.setOnClickListener(v -> openServiceRequest("Maintenance"));
        }
        
        if (btnBookInstallation != null) {
            btnBookInstallation.setOnClickListener(v -> openServiceRequest("Installation"));
        }
        
        if (btnBookRepair != null) {
            btnBookRepair.setOnClickListener(v -> openServiceRequest("Repair"));
        }
    }
    
    private void openServiceRequest(String serviceType) {
        Intent intent = new Intent(getActivity(), ServiceSelectActivity.class);
        intent.putExtra("serviceType", serviceType);
        startActivity(intent);
    }

    private String buildLocationText(String city, String region) {
        String trimmedCity = city != null ? city.trim() : "";
        String trimmedRegion = region != null ? region.trim() : "";

        if (!trimmedCity.isEmpty() && !trimmedRegion.isEmpty()) {
            return trimmedCity + ", " + trimmedRegion;
        }
        if (!trimmedCity.isEmpty()) {
            return trimmedCity;
        }
        if (!trimmedRegion.isEmpty()) {
            return trimmedRegion;
        }
        return null;
    }
    
    private void loadTicketCounts() {
        firestoreManager.listenToMyTickets(new app.hub.common.FirestoreManager.TicketListListener() {
            @Override
            public void onTicketsUpdated(List<app.hub.api.TicketListResponse.TicketItem> tickets) {
                if (tickets != null) {
                    updateTicketCounts(tickets);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to fetch tickets from Firestore: " + e.getMessage());
            }
        });
    }
    
    private void updateTicketCounts(List<TicketListResponse.TicketItem> tickets) {
        int newCount = 0;
        int pendingCount = 0;
        int completedCount = 0;
        
        for (TicketListResponse.TicketItem ticket : tickets) {
            String status = ticket.getStatus();
            if (status != null) {
                status = status.toLowerCase();
                if (status.contains("pending") || status.equals("new")) {
                    pendingCount++;
                } else if (status.contains("in progress") || status.contains("in_progress") || 
                           status.contains("scheduled") || status.contains("assigned")) {
                    newCount++;
                } else if (status.contains("completed") || status.contains("done")) {
                    completedCount++;
                }
            }
        }
        
        // Make variables effectively final for lambda
        final int finalNewCount = newCount;
        final int finalPendingCount = pendingCount;
        final int finalCompletedCount = completedCount;
        
        // Update UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (tvNewTicketsCount != null) {
                    tvNewTicketsCount.setText(String.valueOf(finalNewCount));
                }
                if (tvPendingTicketsCount != null) {
                    tvPendingTicketsCount.setText(String.valueOf(finalPendingCount));
                }
                if (tvCompletedTicketsCount != null) {
                    tvCompletedTicketsCount.setText(String.valueOf(finalCompletedCount));
                }
            });
        }
    }

}
