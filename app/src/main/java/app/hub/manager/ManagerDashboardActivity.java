package app.hub.manager;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;
import app.hub.api.TicketListResponse;

public class ManagerDashboardActivity extends AppCompatActivity {

    private View navIndicator;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        app.hub.util.EdgeToEdgeHelper.enable(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        navIndicator = findViewById(R.id.navIndicator);
        bottomNav = findViewById(R.id.bottom_navigation);
        
        // Apply bottom inset only to the bottom navigation
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        disableNavigationTooltips(bottomNav);

        // as soon as the activity is created, we want to show the Home fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new ManagerHomeFragment()).commit();

            // Set initial indicator position
            bottomNav.post(() -> moveIndicatorToItem(R.id.nav_home, false));
        }

        // Load all manager data at startup so tabs are instantly ready
        loadAllManagerData();

        // Start Firebase real-time listeners
        ManagerDataManager.startFirebaseListeners(this);
    }

    private void disableNavigationTooltips(BottomNavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View view = navigationView.findViewById(item.getItemId());
            if (view != null) {
                view.setOnLongClickListener(v -> true);
                TooltipCompat.setTooltipText(view, null);
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new ManagerHomeFragment();
        } else if (itemId == R.id.nav_employee) {
            selectedFragment = new ManagerEmployeeFragment();
        } else if (itemId == R.id.nav_work) {
            selectedFragment = new ManagerWorkFragment();
        } else if (itemId == R.id.nav_records) {
            selectedFragment = new ManagerRecordsFragment();
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ManagerProfileFragment();
        }

        if (selectedFragment != null) {
            moveIndicatorToItem(itemId, true);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    selectedFragment).commit();
            return true;
        }

        return false;
    };

    private void moveIndicatorToItem(int itemId, boolean animate) {
        View itemView = bottomNav.findViewById(itemId);
        if (itemView == null || navIndicator == null)
            return;

        int itemWidth = itemView.getWidth();
        int indicatorWidth = navIndicator.getWidth();
        float targetX = itemView.getLeft() + (itemWidth / 2f) - (indicatorWidth / 2f);
        float targetY = 0f;

        if (animate) {
            navIndicator.animate()
                    .translationX(targetX)
                    .translationY(targetY)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            navIndicator.setTranslationX(targetX);
            navIndicator.setTranslationY(targetY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start Firebase listeners for real-time updates
        ManagerDataManager.startFirebaseListeners(this);

        // Only refresh if cache is stale (ManagerDataManager handles this internally)
        // This prevents unnecessary API calls when returning to the app quickly
        android.util.Log.d("ManagerDashboard", "App resumed - checking cache freshness");
        ManagerDataManager.loadAllData(this, new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {
            }

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
            }

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats,
                    List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {
            }

            @Override
            public void onLoadComplete() {
                android.util.Log.d("ManagerDashboard", "Data refresh check completed");
            }

            @Override
            public void onLoadError(String error) {
                android.util.Log.e("ManagerDashboard", "Error refreshing data: " + error);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Option to stop listeners here if we want to save battery/data when
        // backgrounded
        // For now we'll keep them until onDestroy for a "real-time" feel even when
        // switching apps briefly
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop Firebase listeners to prevent memory leaks
        ManagerDataManager.stopFirebaseListeners();
    }

    /**
     * Load all manager data at startup so tabs are instantly ready
     */
    private void loadAllManagerData() {
        android.util.Log.d("ManagerDashboard", "Loading all manager data at startup");

        ManagerDataManager.loadAllData(this, new ManagerDataManager.DataLoadCallback() {
            @Override
            public void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees) {
                android.util.Log.d("ManagerDashboard", "Employees loaded: " + employees.size() + " in " + branchName);
            }

            @Override
            public void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets) {
                android.util.Log.d("ManagerDashboard", "Tickets loaded: " + tickets.size());
            }

            @Override
            public void onDashboardStatsLoaded(app.hub.api.DashboardStatsResponse.Stats stats,
                    List<app.hub.api.DashboardStatsResponse.RecentTicket> recentTickets) {
                android.util.Log.d("ManagerDashboard", "Dashboard stats loaded");
            }

            @Override
            public void onLoadComplete() {
                android.util.Log.d("ManagerDashboard", "All manager data loaded successfully");
                // Data is now ready - tabs will load instantly
            }

            @Override
            public void onLoadError(String error) {
                android.util.Log.e("ManagerDashboard", "Error loading manager data: " + error);
                // Don't show error toast - fragments will handle individual errors
            }
        });
    }
}
