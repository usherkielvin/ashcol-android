package app.hub.user;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import app.hub.R;
import app.hub.util.TokenManager;

public class DashboardActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";
    private static final String TAG = "DashboardActivity";

    private TokenManager tokenManager;
    private View navIndicator;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabChatbot;
    private BroadcastReceiver fcmReceiver;

    public static final String EXTRA_SHOW_MY_TICKETS = "show_my_tickets";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        app.hub.util.EdgeToEdgeHelper.enable(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        tokenManager = new TokenManager(this);
        navIndicator = findViewById(R.id.navIndicator);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabChatbot = findViewById(R.id.fab_chatbot);

        // Apply bottom inset only to the bottom navigation
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = insets.bottom;
            v.setLayoutParams(params);
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });
        navIndicator = findViewById(R.id.navIndicator);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabChatbot = findViewById(R.id.fab_chatbot);

        // Request notification permission for Android 13+
        requestNotificationPermission();

        initFcmReceiver();

        setupFab(fabChatbot);
        disableNavigationTooltips(bottomNavigationView);

        if (savedInstanceState == null) {
            // If returning from ticket creation, optionally show My Tickets tab
            boolean showMyTickets = getIntent().getBooleanExtra(EXTRA_SHOW_MY_TICKETS, false);
            Fragment initialFragment = showMyTickets ? new UserTicketsFragment() : new UserHomeFragment();
            int selectedItemId = showMyTickets ? R.id.my_ticket : R.id.homebtn;

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, initialFragment)
                    .commit();

            bottomNavigationView.post(() -> moveIndicatorToItem(selectedItemId, false));
            if (showMyTickets) {
                bottomNavigationView.setSelectedItemId(R.id.my_ticket);
                if (fabChatbot != null) fabChatbot.hide();
            } else if (fabChatbot != null) {
                fabChatbot.show();
            }
            getIntent().removeExtra(EXTRA_SHOW_MY_TICKETS);
        } else {
            // Activity recreated (e.g., theme change) - restore navigation state
            restoreNavigationState();
        }

        handleNotificationIntent(getIntent());

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            // Handle Chatbot visibility based on fragment
            if (fabChatbot != null) {
                if (itemId == R.id.homebtn) {
                    fabChatbot.show();
                } else {
                    fabChatbot.hide();
                }
            }

            if (itemId == R.id.homebtn) {
                selectedFragment = new UserHomeFragment();
            } else if (itemId == R.id.my_ticket) {
                selectedFragment = new UserTicketsFragment();
            } else if (itemId == R.id.activitybtn) {
                selectedFragment = new UserNotificationFragment();
            } else if (itemId == R.id.Profile) {
                selectedFragment = new UserProfileFragment();
            } else if (itemId == R.id.blank) {
                return false; // Middle blank item for FAB
            }

            if (selectedFragment != null) {
                moveIndicatorToItem(itemId, true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView, selectedFragment)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
                return true;
            }

            return false;
        });

        FloatingActionButton openSheet = findViewById(R.id.servicebtn);
        openSheet.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(DashboardActivity.this);
            View view = getLayoutInflater().inflate(R.layout.uc_service_options, null);

            Button btnCleaning = view.findViewById(R.id.btn_cleaning);
            Button btnRepair = view.findViewById(R.id.btn_repair);
            Button btnInstallation = view.findViewById(R.id.btn_installation);
            Button btnMaintenance = view.findViewById(R.id.btn_maintenance);

            View.OnClickListener serviceClickListener = b -> {
                Button clickedButton = (Button) b;
                Intent intent = new Intent(this, ServiceSelectActivity.class);
                intent.putExtra("serviceType", clickedButton.getText().toString());
                startActivity(intent);
                bottomSheetDialog.dismiss();
            };

            btnCleaning.setOnClickListener(serviceClickListener);
            btnRepair.setOnClickListener(serviceClickListener);
            btnInstallation.setOnClickListener(serviceClickListener);
            btnMaintenance.setOnClickListener(serviceClickListener);

            bottomSheetDialog.setContentView(view);
            bottomSheetDialog.show();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShowMyTickets(intent);
        handleNotificationIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (fcmReceiver != null) {
            IntentFilter filter = new IntentFilter("com.ashcol.FCM_MESSAGE");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(fcmReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(fcmReceiver, filter);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fcmReceiver != null) {
            unregisterReceiver(fcmReceiver);
        }
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String type = intent.getStringExtra("type");
        String ticketId = intent.getStringExtra("ticket_id");

        if (type != null && type.equals("payment_pending") && ticketId != null) {
            if (fabChatbot != null) {
                fabChatbot.hide();
            }
            openPaymentFlow(ticketId);
        }
    }

    private void initFcmReceiver() {
        fcmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String type = intent.getStringExtra("type");
                String ticketId = intent.getStringExtra("ticket_id");
                if ("payment_pending".equals(type) && ticketId != null) {
                    if (fabChatbot != null) {
                        fabChatbot.hide();
                    }
                    openPaymentFlow(ticketId);
                }
            }
        };
    }

    private void openPaymentFlow(String ticketId) {
        startActivity(UserPaymentActivity.createIntent(this, ticketId, 0, 0.0, null, null));
    }

    private void handleShowMyTickets(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_SHOW_MY_TICKETS, false)) {
            intent.removeExtra(EXTRA_SHOW_MY_TICKETS);
            bottomNavigationView.setSelectedItemId(R.id.my_ticket);
            moveIndicatorToItem(R.id.my_ticket, true);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new UserTicketsFragment())
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .commit();
            if (fabChatbot != null) fabChatbot.hide();
        }
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

    private void moveIndicatorToItem(int itemId, boolean animate) {
        View itemView = bottomNavigationView.findViewById(itemId);
        if (itemView == null || navIndicator == null) return;

        itemView.post(() -> {
            int itemWidth = itemView.getWidth();
            int indicatorWidth = navIndicator.getWidth();
            float targetX = itemView.getLeft() + (itemWidth / 2f) - (indicatorWidth / 2f);

            if (animate) {
                navIndicator.animate()
                        .translationX(targetX)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            } else {
                navIndicator.setTranslationX(targetX);
            }
        });
    }

    private void setupFab(FloatingActionButton fab) {
        if (fab == null) return;
//        fab.setOnTouchListener(new View.OnTouchListener() {
//            private float initialX, initialY;
//            private float initialTouchX, initialTouchY;
//
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                ViewGroup parentView = (ViewGroup) view.getParent();
//                switch (event.getActionMasked()) {
//                    case MotionEvent.ACTION_DOWN:
//                        initialX = view.getX();
//                        initialY = view.getY();
//                        initialTouchX = event.getRawX();
//                        initialTouchY = event.getRawY();
//                        return true;
//
//                    case MotionEvent.ACTION_MOVE:
//                        float newX = initialX + (event.getRawX() - initialTouchX);
//                        float newY = initialY + (event.getRawY() - initialTouchY);
//                        newX = Math.max(0, Math.min(newX, parentView.getWidth() - view.getWidth()));
//                        newY = Math.max(0, Math.min(newY, parentView.getHeight() - view.getHeight()));
//                        view.setY(newY);
//                        view.setX(newX);
//                        return true;
//
//                    case MotionEvent.ACTION_UP:
//                        float endX = event.getRawX();
//                        float endY = event.getRawY();
//                        if (isAClick(initialTouchX, endX, initialTouchY, endY)) {
//                            view.performClick();
//                        } else {
//                            float center = parentView.getWidth() / 2f;
//                            float finalX = view.getX() < center - view.getWidth() / 2f ? 0 : parentView.getWidth() - view.getWidth();
//                            ObjectAnimator.ofFloat(view, "x", view.getX(), finalX).setDuration(200).start();
//                        }
//                        return true;
//                }
//                return false;
//            }
//        });

        fab.setOnClickListener(v -> showChatbotBottomSheet());
    }

    private boolean isAClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > 200 || differenceY > 200);
    }

    private void showChatbotBottomSheet() {
        try {
            // Check if bottom sheet is already showing
            androidx.fragment.app.Fragment existing = getSupportFragmentManager()
                    .findFragmentByTag("ChatbotBottomSheet");
            
            if (existing != null && existing.isVisible()) {
                return; // Already open, ignore
            }
            
            // Create and show the bottom sheet
            ChatbotBottomSheetFragment bottomSheet = new ChatbotBottomSheetFragment();
            bottomSheet.show(getSupportFragmentManager(), "ChatbotBottomSheet");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show chatbot bottom sheet", e);
            android.widget.Toast.makeText(this, "Unable to open chatbot. Please try again.", 
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.admin_profile) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new UserProfileFragment())
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Request notification permission for Android 13+ (API 33+)
     */
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                        1001
                );
            }
        }
    }
    
    /**
     * Restore navigation state after activity recreation (e.g., theme change).
     * Syncs the bottom navigation and chatbot visibility with the currently displayed fragment.
     */
    private void restoreNavigationState() {
        // Get the currently displayed fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        
        if (currentFragment == null) {
            return;
        }
        
        // Determine which navigation item should be selected based on the fragment
        int selectedItemId = R.id.homebtn; // Default to home
        boolean shouldShowChatbot = false;
        
        if (currentFragment instanceof UserHomeFragment) {
            selectedItemId = R.id.homebtn;
            shouldShowChatbot = true;
        } else if (currentFragment instanceof UserTicketsFragment) {
            selectedItemId = R.id.my_ticket;
        } else if (currentFragment instanceof UserNotificationFragment) {
            selectedItemId = R.id.activitybtn;
        } else if (currentFragment instanceof UserProfileFragment) {
            selectedItemId = R.id.Profile;
        }
        
        // Update bottom navigation selection
        final int finalSelectedItemId = selectedItemId;
        bottomNavigationView.post(() -> {
            bottomNavigationView.setSelectedItemId(finalSelectedItemId);
            moveIndicatorToItem(finalSelectedItemId, false);
        });
        
        // Update chatbot visibility
        if (fabChatbot != null) {
            if (shouldShowChatbot) {
                fabChatbot.show();
            } else {
                fabChatbot.hide();
            }
        }
    }
}
