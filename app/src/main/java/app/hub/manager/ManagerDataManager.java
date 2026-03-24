package app.hub.manager;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import app.hub.api.DashboardStatsResponse;
import app.hub.api.EmployeeResponse;
import app.hub.api.TicketListResponse;
import app.hub.util.TokenManager;

/**
 * Centralized data manager for manager dashboard
 * Loads all data at startup so tabs are instantly ready
 */
public class ManagerDataManager {
    private static final String TAG = "ManagerDataManager";

    // Static data storage
    private static String cachedBranchName = null;
    private static List<EmployeeResponse.Employee> cachedEmployees = null;
    private static List<TicketListResponse.TicketItem> cachedTickets = null;
    private static DashboardStatsResponse.Stats cachedDashboardStats = null;
    private static List<DashboardStatsResponse.RecentTicket> cachedRecentTickets = null;
    private static boolean isDataLoaded = false;
    private static boolean isLoading = false;
    private static long lastLoadTime = 0;
    private static final List<DataLoadCallback> activeCallbacks = new ArrayList<>();

    // Firebase real-time listener
    private static FirebaseManagerListener firebaseListener = null;

    // Cache duration - refresh if data is older than 30 seconds for profile photos
    private static final long CACHE_DURATION = 30 * 1000; // 30 seconds

    // Observer pattern for data changes
    private static final List<EmployeeDataChangeListener> employeeListeners = new ArrayList<>();
    private static final List<TicketDataChangeListener> ticketListeners = new ArrayList<>();
    private static final List<DashboardDataChangeListener> dashboardListeners = new ArrayList<>();

    // Listener interface for employee data changes
    public interface EmployeeDataChangeListener {
        void onEmployeeDataChanged(String branchName, List<EmployeeResponse.Employee> employees);
    }

    public interface TicketDataChangeListener {
        void onTicketDataChanged(List<TicketListResponse.TicketItem> tickets);
    }

    public interface DashboardDataChangeListener {
        void onDashboardDataChanged(DashboardStatsResponse.Stats stats,
                List<DashboardStatsResponse.RecentTicket> recentTickets);
    }

    // Callbacks for UI updates
    public interface DataLoadCallback {
        void onEmployeesLoaded(String branchName, List<EmployeeResponse.Employee> employees);

        void onTicketsLoaded(List<TicketListResponse.TicketItem> tickets);

        void onDashboardStatsLoaded(DashboardStatsResponse.Stats stats,
                List<DashboardStatsResponse.RecentTicket> recentTickets);

        void onLoadComplete();

        void onLoadError(String error);
    }

    /**
     * Register a listener for employee data changes
     */
    public static void registerEmployeeListener(EmployeeDataChangeListener listener) {
        if (listener != null && !employeeListeners.contains(listener)) {
            employeeListeners.add(listener);
            Log.d(TAG, "Employee listener registered. Total listeners: " + employeeListeners.size());
        }
    }

    /**
     * Unregister a listener for employee data changes
     */
    public static void unregisterEmployeeListener(EmployeeDataChangeListener listener) {
        if (listener != null) {
            employeeListeners.remove(listener);
            Log.d(TAG, "Employee listener unregistered. Total listeners: " + employeeListeners.size());
        }
    }

    public static void registerTicketListener(TicketDataChangeListener listener) {
        if (listener != null && !ticketListeners.contains(listener)) {
            ticketListeners.add(listener);
            Log.d(TAG, "Ticket listener registered. Total listeners: " + ticketListeners.size());
        }
    }

    public static void unregisterTicketListener(TicketDataChangeListener listener) {
        if (listener != null) {
            ticketListeners.remove(listener);
            Log.d(TAG, "Ticket listener unregistered. Total listeners: " + ticketListeners.size());
        }
    }

    public static void registerDashboardListener(DashboardDataChangeListener listener) {
        if (listener != null && !dashboardListeners.contains(listener)) {
            dashboardListeners.add(listener);
            Log.d(TAG, "Dashboard listener registered. Total listeners: " + dashboardListeners.size());
        }
    }

    public static void unregisterDashboardListener(DashboardDataChangeListener listener) {
        if (listener != null) {
            dashboardListeners.remove(listener);
            Log.d(TAG, "Dashboard listener unregistered. Total listeners: " + dashboardListeners.size());
        }
    }

    /**
     * Notify all listeners of employee data changes
     */
    private static void notifyEmployeeListeners() {
        if (cachedBranchName != null && cachedEmployees != null) {
            Log.d(TAG, "Notifying " + employeeListeners.size() + " listeners of employee data change");
            for (EmployeeDataChangeListener listener : employeeListeners) {
                listener.onEmployeeDataChanged(cachedBranchName, new ArrayList<>(cachedEmployees));
            }
        }
    }

    private static void notifyTicketListeners() {
        if (cachedTickets != null) {
            Log.d(TAG, "Notifying " + ticketListeners.size() + " listeners of ticket data change");
            List<TicketListResponse.TicketItem> snapshot = new ArrayList<>(cachedTickets);
            for (TicketDataChangeListener listener : ticketListeners) {
                listener.onTicketDataChanged(snapshot);
            }
        }
    }

    private static void notifyDashboardListeners() {
        Log.d(TAG, "Notifying " + dashboardListeners.size() + " listeners of dashboard data change");
        List<DashboardStatsResponse.RecentTicket> recentSnapshot = cachedRecentTickets != null
                ? new ArrayList<>(cachedRecentTickets)
                : new ArrayList<>();
        for (DashboardDataChangeListener listener : dashboardListeners) {
            listener.onDashboardDataChanged(cachedDashboardStats, recentSnapshot);
        }
    }

    /**
     * Load all manager data at startup
     */
    public static void loadAllData(Context context, DataLoadCallback callback) {
        long currentTime = System.currentTimeMillis();
        boolean isCacheStale = (currentTime - lastLoadTime) > CACHE_DURATION;

        if (isDataLoaded && !isCacheStale) {
            // Data is fresh, return cached data immediately
            if (callback != null) {
                if (cachedEmployees != null) {
                    callback.onEmployeesLoaded(cachedBranchName, cachedEmployees);
                }
                if (cachedTickets != null) {
                    callback.onTicketsLoaded(cachedTickets);
                }
                if (cachedDashboardStats != null) {
                    callback.onDashboardStatsLoaded(cachedDashboardStats, cachedRecentTickets);
                }
                callback.onLoadComplete();
            }
            Log.d(TAG, "Using fresh cached data");
            return;
        }

        if (isLoading) {
            Log.d(TAG, "Already loading data, queuing callback");
            if (callback != null && !activeCallbacks.contains(callback)) {
                activeCallbacks.add(callback);
            }
            return;
        }

        isLoading = true;
        if (callback != null && !activeCallbacks.contains(callback)) {
            activeCallbacks.add(callback);
        }

        TokenManager tokenManager = new TokenManager(context);
        String email = tokenManager.getEmail();

        if (email == null) {
            isLoading = false;
            notifyLoadError("Not authenticated");
            return;
        }

        // Fetch Manager Profile from Firestore to determine their Branch
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            isLoading = false;
            notifyLoadError("Firebase User not found");
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        firestore.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    cachedBranchName = documentSnapshot.getString("branch");
                    if (cachedBranchName == null || cachedBranchName.trim().isEmpty()) {
                        cachedBranchName = "No Branch Assigned";
                    }
                    
                    Log.d(TAG, "Manager branch identified: " + cachedBranchName);
                    
                    // Now load components based on branch
                    loadEmployees(cachedBranchName, null);
                    loadTickets(cachedBranchName, null);
                    loadDashboardStats(cachedBranchName, null);
                } else {
                    isLoading = false;
                    notifyLoadError("Manager document not found in Firestore");
                }
            })
            .addOnFailureListener(e -> {
                isLoading = false;
                notifyLoadError("Failed to fetch manager data: " + e.getMessage());
            });
    }

    private static void loadEmployees(String branch, DataLoadCallback callback) {
        if ("No Branch Assigned".equals(branch)) {
            cachedEmployees = new ArrayList<>();
            notifyEmployeeListeners();
            for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) cb.onEmployeesLoaded(branch, cachedEmployees);
            if (callback != null) callback.onEmployeesLoaded(branch, cachedEmployees);
            checkLoadComplete();
            return;
        }
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("branch", branch)
            .whereEqualTo("role", "technician")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                cachedEmployees = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    EmployeeResponse.Employee emp = new EmployeeResponse.Employee();
                    String uid = doc.getId();
                    emp.setId(Math.abs(uid.hashCode()));
                    emp.setFirstName(doc.getString("firstName"));
                    emp.setLastName(doc.getString("lastName"));
                    emp.setEmail(doc.getString("email"));
                    emp.setProfilePhoto(doc.getString("profilePhoto"));
                    // ticketCount could be added if maintained in user doc, or let adapter handle it
                    cachedEmployees.add(emp);
                }

                Log.d(TAG, "Technicians loaded from Firebase: " + cachedEmployees.size());
                notifyEmployeeListeners();

                for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) {
                    cb.onEmployeesLoaded(branch, cachedEmployees);
                }
                if (callback != null) {
                    callback.onEmployeesLoaded(branch, cachedEmployees);
                }

                checkLoadComplete();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Technician fetch failed: " + e.getMessage());
                cachedEmployees = new ArrayList<>();
                notifyEmployeeListeners();
                for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) cb.onEmployeesLoaded(branch, cachedEmployees);
                if (callback != null) callback.onEmployeesLoaded(branch, cachedEmployees);
                checkLoadComplete();
            });
    }

    private static void loadTickets(String branch, DataLoadCallback callback) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("branch", branch)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                cachedTickets = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    TicketListResponse.TicketItem ticket = doc.toObject(TicketListResponse.TicketItem.class);
                    if (ticket != null) {
                        if (ticket.getTicketId() == null || ticket.getTicketId().isEmpty()) {
                            ticket.setTicketId(doc.getId());
                        }
                        cachedTickets.add(ticket);
                    }
                }
                
                // Sort by descending created_at
                java.util.Collections.sort(cachedTickets, (t1, t2) -> {
                    String d1 = t1.getCreatedAt();
                    String d2 = t2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                });

                Log.d(TAG, "Tickets loaded from Firebase: " + cachedTickets.size());
                notifyTicketListeners();

                for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) {
                    cb.onTicketsLoaded(cachedTickets);
                }
                if (callback != null) {
                    callback.onTicketsLoaded(cachedTickets);
                }

                checkLoadComplete();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Ticket fetch failed: " + e.getMessage());
                cachedTickets = new ArrayList<>();
                notifyTicketListeners();
                for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) cb.onTicketsLoaded(cachedTickets);
                if (callback != null) callback.onTicketsLoaded(cachedTickets);
                checkLoadComplete();
            });
    }

    private static void loadDashboardStats(String branch, DataLoadCallback callback) {
        if (cachedTickets == null) {
            // Wait until tickets are loaded to compute stats
            return;
        }
        
        // Compute stats locally based on fetched cachedTickets
        DashboardStatsResponse.Stats stats = new DashboardStatsResponse.Stats();
        int total = cachedTickets.size();
        int pending = 0;
        int inProgress = 0;
        int completed = 0;
        
        List<DashboardStatsResponse.RecentTicket> recent = new ArrayList<>();
        
        for (TicketListResponse.TicketItem ticket : cachedTickets) {
            String status = ticket.getStatus() != null ? ticket.getStatus().toLowerCase() : "";
            
            if (status.equals("pending") || status.equals("open") || status.equals("scheduled")) {
                pending++;
            } else if (status.equals("in progress") || status.equals("in-progress") || status.contains("progress") || status.equals("active")) {
                inProgress++;
            } else if (status.equals("completed") || status.equals("closed") || status.equals("resolved") || status.equals("paid")) {
                completed++;
            }
            
            // Build recent ticket list max 5
            if (recent.size() < 5) {
                DashboardStatsResponse.RecentTicket rt = new DashboardStatsResponse.RecentTicket();
                rt.setTicketId(ticket.getTicketId());
                rt.setStatus(ticket.getStatus());
                rt.setStatusColor(ticket.getStatusColor());
                rt.setCustomerName(ticket.getCustomerName());
                rt.setServiceType(ticket.getServiceType());
                rt.setDescription(ticket.getDescription());
                rt.setAddress(ticket.getAddress());
                rt.setCreatedAt(ticket.getCreatedAt());
                recent.add(rt);
            }
        }
        
        stats.setTotalTickets(total);
        stats.setPending(pending);
        stats.setInProgress(inProgress);
        stats.setCompleted(completed);
        
        cachedDashboardStats = stats;
        cachedRecentTickets = recent;
        
        notifyDashboardListeners();

        for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) {
            cb.onDashboardStatsLoaded(cachedDashboardStats, cachedRecentTickets);
        }
        if (callback != null) {
            callback.onDashboardStatsLoaded(cachedDashboardStats, cachedRecentTickets);
        }

        checkLoadComplete();
    }

    private static void checkLoadComplete() {
        boolean employeesReady = cachedEmployees != null;
        boolean ticketsReady = cachedTickets != null;
        boolean statsReady = cachedDashboardStats != null;

        if (employeesReady && ticketsReady && statsReady) {
            isDataLoaded = true;
            isLoading = false;
            lastLoadTime = System.currentTimeMillis();
            Log.d(TAG, "All data loaded successfully");

            for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) {
                cb.onLoadComplete();
            }
            activeCallbacks.clear();
        }
    }

    private static void notifyLoadError(String error) {
        for (DataLoadCallback cb : new ArrayList<>(activeCallbacks)) {
            cb.onLoadError(error);
        }
        isLoading = false;
        activeCallbacks.clear();
    }

    // Getter methods for cached data
    public static String getCachedBranchName() {
        return cachedBranchName;
    }

    public static List<EmployeeResponse.Employee> getCachedEmployees() {
        return cachedEmployees != null ? new ArrayList<>(cachedEmployees) : new ArrayList<>();
    }

    public static List<TicketListResponse.TicketItem> getCachedTickets() {
        return cachedTickets != null ? new ArrayList<>(cachedTickets) : new ArrayList<>();
    }

    public static DashboardStatsResponse.Stats getCachedDashboardStats() {
        return cachedDashboardStats;
    }

    public static List<DashboardStatsResponse.RecentTicket> getCachedRecentTickets() {
        return cachedRecentTickets != null ? new ArrayList<>(cachedRecentTickets) : new ArrayList<>();
    }

    public static boolean isDataLoaded() {
        return isDataLoaded;
    }

    // Clear cache methods
    public static void clearEmployeeCache() {
        cachedEmployees = null;
        cachedBranchName = null;
        lastLoadTime = 0; // Force refresh on next load
        Log.d(TAG, "Employee cache cleared");
    }
    
    /**
     * Force refresh employee data (ignores cache)
     * Call this when profile photos are updated
     */
    public static void forceRefreshEmployees(Context context, DataLoadCallback callback) {
        Log.d(TAG, "Force refreshing employee data...");
        lastLoadTime = 0; // Reset cache timer
        cachedEmployees = null; // Clear cache
        
        if (cachedBranchName != null) {
            loadEmployees(cachedBranchName, callback);
        } else {
            loadAllData(context, callback);
        }
    }

    public static void clearTicketCache() {
        cachedTickets = null;
        Log.d(TAG, "Ticket cache cleared");
    }

    public static void updateTicketStatusInCache(String ticketId, String status) {
        if (ticketId == null || ticketId.trim().isEmpty() || status == null) {
            return;
        }

        boolean updated = false;
        if (cachedTickets != null) {
            for (TicketListResponse.TicketItem ticket : cachedTickets) {
                if (ticket != null && ticketId.equals(ticket.getTicketId())) {
                    ticket.setStatus(status);
                    updated = true;
                    break;
                }
            }
        }

        if (cachedRecentTickets != null) {
            for (DashboardStatsResponse.RecentTicket ticket : cachedRecentTickets) {
                if (ticket != null && ticketId.equals(ticket.getTicketId())) {
                    ticket.setStatus(status);
                    updated = true;
                    break;
                }
            }
        }

        if (updated) {
            notifyTicketListeners();
            notifyDashboardListeners();
        }
    }

    public static void updateTicketAssignmentInCache(String ticketId, String status, String assignedStaff,
            String scheduledDate, String scheduledTime) {
        if (ticketId == null || ticketId.trim().isEmpty() || status == null) {
            return;
        }

        TicketListResponse.TicketItem matchedTicket = null;
        if (cachedTickets != null) {
            for (TicketListResponse.TicketItem ticket : cachedTickets) {
                if (ticket != null && ticketId.equals(ticket.getTicketId())) {
                    ticket.setStatus(status);
                    if (assignedStaff != null) {
                        ticket.setAssignedStaff(assignedStaff);
                    }
                    if (scheduledDate != null) {
                        ticket.setScheduledDate(scheduledDate);
                    }
                    if (scheduledTime != null) {
                        ticket.setScheduledTime(scheduledTime);
                    }
                    if (ticket.getStatusColor() == null || ticket.getStatusColor().isEmpty()) {
                        ticket.setStatusColor("#2196F3");
                    }
                    matchedTicket = ticket;
                    break;
                }
            }
        }

        if (cachedRecentTickets != null) {
            DashboardStatsResponse.RecentTicket targetRecent = null;
            for (DashboardStatsResponse.RecentTicket ticket : cachedRecentTickets) {
                if (ticket != null && ticketId.equals(ticket.getTicketId())) {
                    ticket.setStatus(status);
                    if (ticket.getStatusColor() == null || ticket.getStatusColor().isEmpty()) {
                        ticket.setStatusColor("#2196F3");
                    }
                    targetRecent = ticket;
                    break;
                }
            }

            if (targetRecent == null && matchedTicket != null) {
                targetRecent = new DashboardStatsResponse.RecentTicket();
                targetRecent.setTicketId(matchedTicket.getTicketId());
                targetRecent.setStatus(matchedTicket.getStatus());
                targetRecent.setStatusColor(matchedTicket.getStatusColor());
                targetRecent.setCustomerName(matchedTicket.getCustomerName());
                targetRecent.setServiceType(matchedTicket.getServiceType());
                targetRecent.setDescription(matchedTicket.getDescription());
                targetRecent.setAddress(matchedTicket.getAddress());
                targetRecent.setCreatedAt(matchedTicket.getCreatedAt());
            }

            if (targetRecent != null) {
                removeRecentByTicketId(cachedRecentTickets, ticketId);
                cachedRecentTickets.add(0, targetRecent);
            }
        } else if (matchedTicket != null) {
            cachedRecentTickets = new ArrayList<>();
            DashboardStatsResponse.RecentTicket targetRecent = new DashboardStatsResponse.RecentTicket();
            targetRecent.setTicketId(matchedTicket.getTicketId());
            targetRecent.setStatus(matchedTicket.getStatus());
            targetRecent.setStatusColor(matchedTicket.getStatusColor());
            targetRecent.setCustomerName(matchedTicket.getCustomerName());
            targetRecent.setServiceType(matchedTicket.getServiceType());
            targetRecent.setDescription(matchedTicket.getDescription());
            targetRecent.setAddress(matchedTicket.getAddress());
            targetRecent.setCreatedAt(matchedTicket.getCreatedAt());
            cachedRecentTickets.add(targetRecent);
        }

        notifyTicketListeners();
        notifyDashboardListeners();
    }

    private static void removeRecentByTicketId(List<DashboardStatsResponse.RecentTicket> recentTickets,
            String ticketId) {
        if (recentTickets == null || ticketId == null) {
            return;
        }
        for (int i = recentTickets.size() - 1; i >= 0; i--) {
            DashboardStatsResponse.RecentTicket ticket = recentTickets.get(i);
            if (ticket != null && ticketId.equals(ticket.getTicketId())) {
                recentTickets.remove(i);
            }
        }
    }

    public static void clearAllCache() {
        cachedBranchName = null;
        cachedEmployees = null;
        cachedTickets = null;
        isDataLoaded = false;
        isLoading = false;
        lastLoadTime = 0;
        Log.d(TAG, "All cache cleared");
    }

    /**
     * Refresh specific data type
     */
    public static void refreshEmployees(Context context, DataLoadCallback callback) {
        Log.d(TAG, "Refreshing employees data");
        clearEmployeeCache();
        if (cachedBranchName != null) {
            isLoading = true;
            loadEmployees(cachedBranchName, callback);
        } else {
            loadAllData(context, callback);
        }
    }

    public static void refreshTickets(Context context, DataLoadCallback callback) {
        clearTicketCache();
        if (cachedBranchName != null) {
            loadTickets(cachedBranchName, callback);
        } else {
            loadAllData(context, callback);
        }
    }

    /**
     * Force refresh all data (ignores cache)
     */
    public static void forceRefreshAllData(Context context, DataLoadCallback callback) {
        Log.d(TAG, "Force refreshing all data");
        clearAllCache();
        loadAllData(context, callback);
    }

    // Firebase real-time sync methods
    public static void startFirebaseListeners(Context context) {
        if (firebaseListener == null) {
            firebaseListener = new FirebaseManagerListener(context);
        }
        firebaseListener.startListening();
    }

    public static void stopFirebaseListeners() {
        if (firebaseListener != null) {
            firebaseListener.stopListening();
        }
    }
}
