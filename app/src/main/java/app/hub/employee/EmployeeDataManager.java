package app.hub.employee;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import app.hub.api.EmployeeScheduleResponse;
import app.hub.api.TicketListResponse;
import app.hub.util.TokenManager;

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
        
        isLoadingTickets = true;
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getAuthToken();
        
        if (token == null) {
            isLoadingTickets = false;
            if (callback != null) {
                callback.onError("No auth token");
            }
            return;
        }
        
        ApiService apiService = ApiClient.getApiService();
        Call<TicketListResponse> call = apiService.getEmployeeTickets(token);
        
        call.enqueue(new Callback<TicketListResponse>() {
            @Override
            public void onResponse(Call<TicketListResponse> call, Response<TicketListResponse> response) {
                isLoadingTickets = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    TicketListResponse ticketResponse = response.body();
                    
                    if (ticketResponse.isSuccess() && ticketResponse.getTickets() != null) {
                        cachedTickets = new ArrayList<>(ticketResponse.getTickets());
                        lastTicketLoadTime = System.currentTimeMillis();
                        
                        Log.d(TAG, "Tickets loaded and cached: " + cachedTickets.size());
                        
                        if (callback != null) {
                            callback.onSuccess(new ArrayList<>(cachedTickets));
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("API returned no tickets");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("API error: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<TicketListResponse> call, Throwable t) {
                isLoadingTickets = false;
                Log.e(TAG, "Failed to load tickets", t);
                
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
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
        
        isLoadingSchedule = true;
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getAuthToken();
        
        if (token == null) {
            isLoadingSchedule = false;
            if (callback != null) {
                callback.onError("No auth token");
            }
            return;
        }
        
        ApiService apiService = ApiClient.getApiService();
        Call<EmployeeScheduleResponse> call = apiService.getEmployeeSchedule(token);
        
        call.enqueue(new Callback<EmployeeScheduleResponse>() {
            @Override
            public void onResponse(Call<EmployeeScheduleResponse> call, Response<EmployeeScheduleResponse> response) {
                isLoadingSchedule = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    cachedSchedule = response.body();
                    lastScheduleLoadTime = System.currentTimeMillis();
                    
                    Log.d(TAG, "Schedule loaded and cached");
                    
                    if (callback != null) {
                        callback.onSuccess(cachedSchedule);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("API error: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<EmployeeScheduleResponse> call, Throwable t) {
                isLoadingSchedule = false;
                Log.e(TAG, "Failed to load schedule", t);
                
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
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
