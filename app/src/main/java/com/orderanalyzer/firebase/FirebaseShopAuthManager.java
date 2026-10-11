package com.orderanalyzer.firebase;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirebaseShopAuthManager {

    private static final String PREF_NAME = "shop_auth_prefs";
    private static final String KEY_LOGIN_ID = "pref_login_id";
    private static final String KEY_SHOP_NAME = "pref_shop_name";
    private static final String KEY_SHOP_ID = "pref_shop_id";

    // Pseudo-domain for internal Firebase email translation — never shown in UI
    private static final String EMAIL_DOMAIN = "@orderanalyzer.internal";

    private static FirebaseShopAuthManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;

    public interface AuthCallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    private FirebaseShopAuthManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized FirebaseShopAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseShopAuthManager(context);
        }
        return instance;
    }

    /**
     * Checks whether Firebase is properly initialized and available.
     */
    public boolean isFirebaseAvailable() {
        try {
            if (!FirebaseApp.getApps(appContext).isEmpty()) {
                return true;
            }
            FirebaseApp app = FirebaseApp.initializeApp(appContext);
            return app != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoggedIn() {
        if (!isFirebaseAvailable()) {
            return false;
        }
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentShopId() {
        if (isLoggedIn()) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) return user.getUid();
            } catch (Exception ignored) {}
        }
        return prefs.getString(KEY_SHOP_ID, "");
    }

    public String getLoginId() {
        String saved = prefs.getString(KEY_LOGIN_ID, "");
        if (!saved.isEmpty()) return saved;
        if (isLoggedIn()) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    return emailToLoginId(user.getEmail());
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    public String getShopName() {
        return prefs.getString(KEY_SHOP_NAME, "My Shop");
    }

    public void setShopName(String name) {
        prefs.edit().putString(KEY_SHOP_NAME, name != null ? name.trim() : "").apply();
    }

    /** Converts a valid user-facing Login ID into its private Firebase email alias. */
    public static String loginIdToEmail(String loginId) {
        if (loginId == null) return "";
        String cleaned = loginId.trim().toLowerCase(Locale.US);
        if (!cleaned.matches("[a-z0-9][a-z0-9._-]{2,29}")) return "";
        return cleaned + EMAIL_DOMAIN;
    }

    /**
     * Converts an internal Firebase email back to the original Login ID.
     */
    public static String emailToLoginId(String email) {
        if (email == null) return "";
        if (email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return email;
    }

    /**
     * Logs in using Login ID and Password.
     */
    public void login(String loginId, String password, AuthCallback callback) {
        if (!isFirebaseAvailable()) {
            callback.onFailure("Firebase configuration (google-services.json) is not configured yet.");
            return;
        }

        String cleanedId = loginId != null ? loginId.trim().toLowerCase(Locale.US) : "";
        String email = loginIdToEmail(cleanedId);
        if (email.isEmpty()) {
            callback.onFailure("Login ID must be 3–30 characters and use letters, numbers, dots, underscores, or hyphens.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            callback.onFailure("Please enter your password.");
            return;
        }

        try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            String uid = user.getUid();
                            prefs.edit()
                                    .putString(KEY_LOGIN_ID, cleanedId)
                                    .putString(KEY_SHOP_ID, uid)
                                    .apply();

                            // Fetch profile from Firestore to restore shop name
                            fetchRemoteProfile(uid, () -> {
                                callback.onSuccess("Login successful!");
                            });
                        } else {
                            callback.onFailure("Unable to retrieve user details.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        String msg = e.getMessage();
                        if (msg != null && (msg.contains("user-not-found") || msg.contains("wrong-password") || msg.contains("invalid-credential"))) {
                            callback.onFailure("Invalid Login ID or password.");
                        } else {
                            callback.onFailure(msg != null ? msg : "Login failed.");
                        }
                    });
        } catch (Exception e) {
            callback.onFailure(e.getMessage() != null ? e.getMessage() : "Error connecting to authentication service.");
        }
    }

    /**
     * Creates a new Shop Account.
     */
    public void createShopAccount(String shopName, String loginId, String password, AuthCallback callback) {
        if (!isFirebaseAvailable()) {
            callback.onFailure("Firebase configuration (google-services.json) is not configured yet.");
            return;
        }

        final String cleanShopName = shopName != null ? shopName.trim() : "";
        final String cleanLoginId = loginId != null ? loginId.trim().toLowerCase(Locale.US) : "";

        if (cleanShopName.isEmpty()) {
            callback.onFailure("Please enter your Shop Name.");
            return;
        }
        if (loginIdToEmail(cleanLoginId).isEmpty()) {
            callback.onFailure("Login ID must be 3–30 characters and use letters, numbers, dots, underscores, or hyphens.");
            return;
        }
        if (password == null || password.length() < 6) {
            callback.onFailure("Password must be at least 6 characters.");
            return;
        }

        String email = loginIdToEmail(cleanLoginId);

        try {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            String uid = user.getUid();
                            prefs.edit()
                                    .putString(KEY_SHOP_NAME, cleanShopName)
                                    .putString(KEY_LOGIN_ID, cleanLoginId)
                                    .putString(KEY_SHOP_ID, uid)
                                    .apply();

                            // Store profile document in Firestore: shops/{uid}/profile/info
                            Map<String, Object> profileData = new HashMap<>();
                            profileData.put("shopName", cleanShopName);
                            profileData.put("loginId", cleanLoginId);
                            profileData.put("createdAt", System.currentTimeMillis());
                            profileData.put("updatedAt", System.currentTimeMillis());

                            FirebaseFirestore.getInstance()
                                    .collection("shops").document(uid)
                                    .collection("profile").document("info")
                                    .set(profileData, SetOptions.merge())
                                    .addOnSuccessListener(unused -> callback.onSuccess("Shop account created successfully!"))
                                    .addOnFailureListener(error -> callback.onFailure(
                                            "Account was created, but its shop profile could not be saved. Keep these credentials and try logging in again."));
                        } else {
                            callback.onFailure("Account created but failed to get user details.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("already in use")) {
                            callback.onFailure("This Login ID is already taken. Please choose another.");
                        } else {
                            callback.onFailure(msg != null ? msg : "Failed to create shop account.");
                        }
                    });
        } catch (Exception e) {
            callback.onFailure(e.getMessage() != null ? e.getMessage() : "Error connecting to authentication service.");
        }
    }

    /**
     * Changes the password for the currently authenticated shop account.
     */
    public void changePassword(String newPassword, AuthCallback callback) {
        if (!isFirebaseAvailable()) {
            callback.onFailure("Firebase configuration (google-services.json) is not configured yet.");
            return;
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            callback.onFailure("Password must be at least 6 characters.");
            return;
        }

        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                callback.onFailure("You must be logged in to change your password.");
                return;
            }

            user.updatePassword(newPassword.trim())
                    .addOnSuccessListener(unused -> callback.onSuccess("Password updated successfully! Both phones can now log in with the new password."))
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage() != null ? e.getMessage() : "Failed to change password."));
        } catch (Exception e) {
            callback.onFailure(e.getMessage() != null ? e.getMessage() : "Error changing password.");
        }
    }

    /**
     * Dispatches password reset for the account.
     */
    public void forgotPassword(String loginId, AuthCallback callback) {
        if (!isFirebaseAvailable()) {
            callback.onFailure("Firebase configuration (google-services.json) is not configured yet.");
            return;
        }

        String cleanedId = loginId != null ? loginId.trim().toLowerCase(Locale.US) : "";
        if (loginIdToEmail(cleanedId).isEmpty()) {
            callback.onFailure("Enter a valid Login ID.");
            return;
        }
        callback.onFailure("Shop accounts use private Login IDs for cross-device shared access without exposing personal emails. To update or reset credentials, change your password from Shop Profile on any logged-in phone, or reset it in Firebase Console > Authentication.");
    }

    public void logout() {
        if (isFirebaseAvailable()) {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
        }
        prefs.edit().clear().apply();
    }

    private void fetchRemoteProfile(String uid, Runnable onComplete) {
        try {
            FirebaseFirestore.getInstance()
                    .collection("shops").document(uid)
                    .collection("profile").document("info")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String name = snapshot.getString("shopName");
                            if (name != null && !name.isEmpty()) {
                                prefs.edit().putString(KEY_SHOP_NAME, name).apply();
                            }
                        }
                        if (onComplete != null) onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        if (onComplete != null) onComplete.run();
                    });
        } catch (Exception e) {
            if (onComplete != null) onComplete.run();
        }
    }
}
