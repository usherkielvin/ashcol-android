package app.hub.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import java.util.Map;

public class TokenManager {
    private static final String PREF_NAME = "auth_pref";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_UID = "firebase_uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CONNECTION_STATUS = "connection_status";
    private static final String KEY_CURRENT_CITY = "current_city";
    private static final String KEY_FAILED_GOOGLE_LOGIN_PREFIX = "failed_google_login_";
    private static final String KEY_USER_BRANCH = "user_branch";

    // Branch Cache
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_EMPLOYEE_COUNT = "employee_count";
    private static final String KEY_BRANCH_CACHE_TIME = "branch_cache_time";
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30 minutes cache for branch data

    // Notification Settings
    private static final String KEY_PUSH_NOTIF = "push_notifications";
    private static final String KEY_EMAIL_NOTIF = "email_notifications";
    private static final String KEY_SMS_NOTIF = "sms_notifications";

    // Firebase Cloud Messaging Token
    private static final String KEY_FCM_TOKEN = "fcm_token";

    // Theme and Language Preferences
    private static final String KEY_THEME_PREFERENCE = "theme_preference";
    private static final String KEY_LANGUAGE_PREFERENCE = "language_preference";

    private final SharedPreferences sharedPreferences;

    public TokenManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public String getAuthToken() {
        String token = getToken();
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token;
        }
        return "Bearer " + token;
    }

    public void saveEmail(String email) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public File getProfileImageFile(Context context) {
        String fileName = buildProfileImageFileName();
        return new File(context.getFilesDir(), fileName);
    }

    public void saveName(String name) {
        sharedPreferences.edit().putString(KEY_NAME, name).apply();
    }

    public String getName() {
        return sharedPreferences.getString(KEY_NAME, null);
    }

    public void saveRole(String role) {
        sharedPreferences.edit().putString(KEY_ROLE, role).apply();
    }

    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, null);
    }

    public void saveUserId(int userId) {
        sharedPreferences.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public int getUserIdInt() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    public void clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply();
    }

    public void clearAuthToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply();
    }

    public void saveUid(String uid) {
        sharedPreferences.edit().putString(KEY_UID, uid).apply();
    }

    public String getUid() {
        return sharedPreferences.getString(KEY_UID, null);
    }

    public String getUserId() {
        // For backward compatibility, we can use email as user identifier
        return getEmail();
    }

    public String getUserRole() {
        return getRole();
    }

    public void saveUserBranch(String branch) {
        sharedPreferences.edit().putString(KEY_USER_BRANCH, branch).apply();
    }

    public String getUserBranch() {
        return sharedPreferences.getString(KEY_USER_BRANCH, null);
    }

    public void clear() {
        // Use commit() instead of apply() for immediate persistence
        sharedPreferences.edit().clear().commit();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveConnectionStatus(String status) {
        sharedPreferences.edit().putString(KEY_CONNECTION_STATUS, status).apply();
    }

    public String getConnectionStatus() {
        return sharedPreferences.getString(KEY_CONNECTION_STATUS, null);
    }

    public void clearConnectionStatus() {
        sharedPreferences.edit().remove(KEY_CONNECTION_STATUS).apply();
    }

    public void saveCurrentCity(String city) {
        sharedPreferences.edit().putString(KEY_CURRENT_CITY, city).apply();
    }

    public String getCurrentCity() {
        return sharedPreferences.getString(KEY_CURRENT_CITY, null);
    }

    /**
     * Check if a Google login has failed for a specific email
     */
    public boolean hasFailedGoogleLogin(String email) {
        if (email == null)
            return false;
        String key = KEY_FAILED_GOOGLE_LOGIN_PREFIX + sanitizeKey(email);
        return sharedPreferences.getBoolean(key, false);
    }

    /**
     * Mark a Google login as failed for a specific email
     */
    public void markFailedGoogleLogin(String email) {
        if (email == null)
            return;
        String key = KEY_FAILED_GOOGLE_LOGIN_PREFIX + sanitizeKey(email);
        sharedPreferences.edit().putBoolean(key, true).apply();
    }

    /**
     * Clear the failed Google login flag for a specific email
     */
    public void clearFailedGoogleLogin(String email) {
        if (email == null)
            return;
        String key = KEY_FAILED_GOOGLE_LOGIN_PREFIX + sanitizeKey(email);
        sharedPreferences.edit().remove(key).apply();
    }

    /**
     * Clear all failed Google login flags
     */
    public void clearAllFailedGoogleLogins() {
        // Get all keys that start with the failed login prefix
        Map<String, ?> allEntries = sharedPreferences.getAll();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(KEY_FAILED_GOOGLE_LOGIN_PREFIX)) {
                editor.remove(entry.getKey());
            }
        }
        editor.apply();
    }

    /**
     * Reset Google login tracking completely
     */
    public void resetGoogleLoginTracking() {
        clearAllFailedGoogleLogins();
    }

    /**
     * Sanitize email to be used as a SharedPreferences key
     */
    private String sanitizeKey(String key) {
        // Replace characters that are not allowed in SharedPreferences keys
        return key.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String buildProfileImageFileName() {
        int userId = getUserIdInt();
        if (userId > 0) {
            return "profile_image_" + userId + ".jpg";
        }

        String email = getEmail();
        if (email != null && !email.trim().isEmpty()) {
            return "profile_image_" + sanitizeKey(email.toLowerCase()) + ".jpg";
        }

        return "profile_image.jpg";
    }

    // Notification Settings Methods
    public void setPushEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_PUSH_NOTIF, enabled).apply();
    }

    public boolean isPushEnabled() {
        return sharedPreferences.getBoolean(KEY_PUSH_NOTIF, true);
    }

    public void setEmailNotifEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_EMAIL_NOTIF, enabled).apply();
    }

    public boolean isEmailNotifEnabled() {
        return sharedPreferences.getBoolean(KEY_EMAIL_NOTIF, true);
    }

    public void setSmsNotifEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SMS_NOTIF, enabled).apply();
    }

    public boolean isSmsNotifEnabled() {
        return sharedPreferences.getBoolean(KEY_SMS_NOTIF, false);
    }

    // FCM Token Methods
    public void saveFCMToken(String token) {
        sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply();
        android.util.Log.d("TokenManager", "FCM Token saved: " + token);
    }

    public String getFCMToken() {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Force immediate persistence of SharedPreferences.
     * This method ensures all pending changes are written to disk immediately.
     */
    public void forceCommit() {
        // Create a small commit operation to ensure all pending apply() calls are
        // flushed
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("_commit_sync_key", true);
        editor.commit(); // Synchronous commit to ensure immediate persistence
        // Clean up the temporary key
        sharedPreferences.edit().remove("_commit_sync_key").apply();
    }

    // Branch Cache Methods
    public void saveBranchInfo(String branch, int employeeCount) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_BRANCH, branch);
        editor.putInt(KEY_EMPLOYEE_COUNT, employeeCount);
        editor.putLong(KEY_BRANCH_CACHE_TIME, System.currentTimeMillis());
        editor.apply();
    }

    public String getCachedBranch() {
        if (isCacheValid()) {
            return sharedPreferences.getString(KEY_BRANCH, null);
        }
        return null;
    }

    public Integer getCachedEmployeeCount() {
        if (isCacheValid()) {
            return sharedPreferences.getInt(KEY_EMPLOYEE_COUNT, -1);
        }
        return null;
    }

    private boolean isCacheValid() {
        long cacheTime = sharedPreferences.getLong(KEY_BRANCH_CACHE_TIME, 0);
        return (System.currentTimeMillis() - cacheTime) < CACHE_DURATION;
    }

    public void clearBranchCache() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_BRANCH);
        editor.remove(KEY_EMPLOYEE_COUNT);
        editor.remove(KEY_BRANCH_CACHE_TIME);
        editor.apply();
    }

    // Theme and Language Preference Methods
    public void setThemePreference(String theme) {
        sharedPreferences.edit().putString(KEY_THEME_PREFERENCE, theme).apply();
    }

    public String getThemePreference() {
        return sharedPreferences.getString(KEY_THEME_PREFERENCE, "system");
    }

    public void setLanguagePreference(String language) {
        sharedPreferences.edit().putString(KEY_LANGUAGE_PREFERENCE, language).apply();
    }

    public String getLanguagePreference() {
        return sharedPreferences.getString(KEY_LANGUAGE_PREFERENCE, "english_us");
    }
}
