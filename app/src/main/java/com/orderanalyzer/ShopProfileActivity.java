package com.orderanalyzer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.orderanalyzer.firebase.FirebaseShopAuthManager;
import com.orderanalyzer.firebase.FirebaseSyncManager;
import com.orderanalyzer.firebase.SyncStatus;

public class ShopProfileActivity extends AppCompatActivity {

    private FirebaseShopAuthManager authManager;
    private FirebaseSyncManager syncManager;

    private TextView tvShopName, tvLoginId;
    private TextView tvSyncStatus, tvLastSync, tvAppVersion;

    private final FirebaseSyncManager.OnSyncStatusChangeListener syncListener =
            (status, lastSyncFormatted) -> runOnUiThread(() -> updateSyncUI(status, lastSyncFormatted));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_profile);
        authManager = FirebaseShopAuthManager.getInstance(this);
        syncManager = FirebaseSyncManager.getInstance(this);
        initViews();
        loadProfileData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncManager.addListener(syncListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        syncManager.removeListener(syncListener);
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvShopName   = findViewById(R.id.tv_profile_shop_name);
        tvLoginId    = findViewById(R.id.tv_profile_login_id);
        tvSyncStatus = findViewById(R.id.tv_profile_sync_status);
        tvLastSync   = findViewById(R.id.tv_profile_last_sync);
        tvAppVersion = findViewById(R.id.tv_profile_app_version);

        ImageButton btnEditShopName = findViewById(R.id.btn_edit_shop_name);
        if (btnEditShopName != null) {
            btnEditShopName.setOnClickListener(v -> showEditShopNameDialog());
        }

        View btnSyncNow = findViewById(R.id.btn_sync_now);
        if (btnSyncNow != null) {
            btnSyncNow.setOnClickListener(v -> {
                Toast.makeText(this, "Synchronizing orders with cloud\u2026", Toast.LENGTH_SHORT).show();
                syncManager.startSync();
            });
        }

        LinearLayout btnChangePassword = findViewById(R.id.btn_change_password);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        LinearLayout btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> confirmLogout());
        }
    }

    private void showEditShopNameDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 24, 60, 8);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("Enter new shop name:");
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvLabel);

        final EditText etShopName = new EditText(this);
        etShopName.setText(authManager.getShopName());
        etShopName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etShopName.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(50) });
        etShopName.selectAll();
        layout.addView(etShopName);

        new AlertDialog.Builder(this)
                .setTitle("Edit Shop Name")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etShopName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Shop name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    authManager.setShopName(newName);
                    tvShopName.setText(newName);
                    saveShopNameToFirestore(newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveShopNameToFirestore(String newName) {
        if (!authManager.isFirebaseAvailable() || !authManager.isLoggedIn()) return;
        try {
            String uid = authManager.getCurrentShopId();
            if (uid == null || uid.isEmpty()) return;
            java.util.Map<String, Object> update = new java.util.HashMap<>();
            update.put("shopName", newName);
            update.put("updatedAt", System.currentTimeMillis());
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("shops").document(uid)
                    .collection("profile").document("info")
                    .update(update)
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Shop name saved locally. Cloud sync will update shortly.",
                                    Toast.LENGTH_SHORT).show());
        } catch (Exception ignored) {}
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 24, 60, 8);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("Enter new password for " + authManager.getShopName() + " (min 6 characters):");
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvLabel);

        final EditText etNewPass = new EditText(this);
        etNewPass.setHint("\u00b7\u00b7\u00b7\u00b7\u00b7\u00b7\u00b7\u00b7");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        new AlertDialog.Builder(this)
                .setTitle("Change Shop Password")
                .setView(layout)
                .setPositiveButton("Update Password", (dialog, which) -> {
                    String newPass = etNewPass.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    authManager.changePassword(newPass, new FirebaseShopAuthManager.AuthCallback() {
                        @Override public void onSuccess(String message) {
                            new AlertDialog.Builder(ShopProfileActivity.this)
                                    .setTitle("Password Updated")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null).show();
                        }
                        @Override public void onFailure(String errorMessage) {
                            new AlertDialog.Builder(ShopProfileActivity.this)
                                    .setTitle("Update Failed")
                                    .setMessage(friendlyAuthError(errorMessage))
                                    .setPositiveButton("OK", null).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProfileData() {
        tvShopName.setText(authManager.getShopName());
        tvLoginId.setText(authManager.getLoginId());
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText(v);
        } catch (Exception ignored) {
            tvAppVersion.setText("1.0");
        }
        updateSyncUI(syncManager.getCurrentStatus(), syncManager.getLastSyncFormatted());
    }

    private void updateSyncUI(SyncStatus status, String lastSyncFormatted) {
        if (tvSyncStatus == null) return;
        tvSyncStatus.setText("\u25cf " + status.getLabel());
        try {
            tvSyncStatus.setTextColor(Color.parseColor(status.getTextColorHex()));
            tvSyncStatus.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor(status.getBgColorHex())));
        } catch (Exception ignored) {}
        if (tvLastSync != null) tvLastSync.setText(lastSyncFormatted);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log out of shop?")
                .setMessage("You can sign in again using your Login ID and Password.")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    authManager.logout();
                    syncManager.stopSync();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String friendlyAuthError(String rawMessage) {
        String msg = rawMessage == null ? "" : rawMessage.toLowerCase(java.util.Locale.US);
        if (msg.contains("recent login") || msg.contains("requires-recent-login"))
            return "For your security, sign in again before changing the password.";
        if (msg.contains("password") && (msg.contains("6") || msg.contains("weak")))
            return "Choose a password with at least 6 characters.";
        if (msg.contains("network") || msg.contains("timeout") || msg.contains("unavailable") || msg.contains("connection"))
            return "We could not connect. Check your internet connection and try again.";
        return "We could not update the password. Please try again in a moment.";
    }
}