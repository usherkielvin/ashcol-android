package app.hub.employee;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import app.hub.api.EmployeeScheduleResponse;
import app.hub.api.TicketListResponse;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Centralized data manager for employee/technician
 * Handles caching and data loading for better performance
 */
public class EmployeeDataManager {
    private static final String TAG = "EmployeeDataManager";
    
    // Cache duration - 10 seconds for fast updates
    private static final long CACHE_DURATION = 10 * 1000; // 10 seconds
    
    // Cached data
    private static List<TicketListResponse.TicketItem> cachedTickets = null;
    private static EmployeeScheduleResponse cachedSchedule = null;
    private static long lastTicketLoadTime = 0;
    private static long lastScheduleLoadTime = 0;
    
    // Loading flags
    private static boolean isLoadingTickets = false;
    private static boolean isLoadingSchedule = false;
    
    /**
     * Load tickets with caching
     */
    public static void loadTickets(Context context, boolean forceRefresh, DataCallback<List<TicketListResponse.TicketItem>> callback) {
        long currentTime = System.currentTimeMillis();
        boolean isCacheValid = (currentTime - lastTicketLoadTime) < CACHE_DURATION;
        
        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && isCacheValid && cachedTickets != null) {
            Log.d(TAG, "Returning cached tickets: " + cachedTickets.size());
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(cachedTickets));
            }
            return;
        }
        
        // Prevent multiple simultaneous loads
        if (isLoadingTickets) {
            Log.d(TAG, "Already loading tickets, skipping");
            return;
        }
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError("User not logged in");
            }
            return;
        }

        isLoadingTickets = true;
        Log.d(TAG, "Loading tickets from Firestore for user: " + user.getUid());

        FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("assigned_staff_id", user.getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                isLoadingTickets = false;
                List<TicketListResponse.TicketItem> tickets = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    TicketListResponse.TicketItem ticket = doc.toObject(TicketListResponse.TicketItem.class);
                    if (ticket != null) {
                        ticket.setTicketId(doc.getId());
                        tickets.add(ticket);
                    }
                }
                
                cachedTickets = new ArrayList<>(tickets);
                lastTicketLoadTime = System.currentTimeMillis();
                
                Log.d(TAG, "Tickets loaded and cached from Firestore: " + cachedTickets.size());
                
                if (callback != null) {
                    callback.onSuccess(new ArrayList<>(cachedTickets));
                }
            })
            .addOnFailureListener(e -> {
                isLoadingTickets = false;
                Log.e(TAG, "Error loading tickets from Firestore: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }

    /**
     * Load schedule with caching
     */
    public static void loadSchedule(Context context, boolean forceRefresh, DataCallback<EmployeeScheduleResponse> callback) {
        long currentTime = System.currentTimeMillis();
        boolean isCacheValid = (currentTime - lastScheduleLoadTime) < CACHE_DURATION;
        
        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && isCacheValid && cachedSchedule != null) {
            Log.d(TAG, "Returning cached schedule");
            if (callback != null) {
                callback.onSuccess(cachedSchedule);
            }
            return;
        }
        
        // Prevent multiple simultaneous loads
        if (isLoadingSchedule) {
            Log.d(TAG, "Already loading schedule, skipping");
            return;
        }
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError("User not logged in");
            }
            return;
        }

        isLoadingSchedule = true;
        Log.d(TAG, "Loading schedule from Firestore for user: " + user.getUid());

        FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("assigned_staff_id", user.getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                isLoadingSchedule = false;
                List<EmployeeScheduleResponse.ScheduledTicket> tickets = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    EmployeeScheduleResponse.ScheduledTicket ticket = doc.toObject(EmployeeScheduleResponse.ScheduledTicket.class);
                    if (ticket != null) {
                        ticket.setTicketId(doc.getId());
                        tickets.add(ticket);
                    }
                }
                
                EmployeeScheduleResponse scheduleResponse = new EmployeeScheduleResponse();
                scheduleResponse.setSuccess(true);
                scheduleResponse.setTickets(tickets);
                
                cachedSchedule = scheduleResponse;
                lastScheduleLoadTime = System.currentTimeMillis();
                
                Log.d(TAG, "Schedule loaded and cached from Firestore: " + tickets.size());
                
                if (callback != null) {
                    callback.onSuccess(cachedSchedule);
                }
            })
            .addOnFailureListener(e -> {
                isLoadingSchedule = false;
                Log.e(TAG, "Error loading schedule from Firestore: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Clear all caches
     */
    public static void clearCache() {
        cachedTickets = null;
        cachedSchedule = null;
        lastTicketLoadTime = 0;
        lastScheduleLoadTime = 0;
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Clear ticket cache only
     */
    public static void clearTicketCache() {
        cachedTickets = null;
        lastTicketLoadTime = 0;
        Log.d(TAG, "Ticket cache cleared");
    }
    
    /**
     * Clear schedule cache only
     */
    public static void clearScheduleCache() {
        cachedSchedule = null;
        lastScheduleLoadTime = 0;
        Log.d(TAG, "Schedule cache cleared");
    }
    
    /**
     * Get cached tickets without loading
     */
    public static List<TicketListResponse.TicketItem> getCachedTickets() {
        return cachedTickets != null ? new ArrayList<>(cachedTickets) : new ArrayList<>();
    }
    
    /**
     * Callback interface for data loading
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}
