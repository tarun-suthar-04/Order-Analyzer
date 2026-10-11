package com.orderanalyzer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.orderanalyzer.firebase.FirebaseShopAuthManager;
import com.orderanalyzer.firebase.FirebaseSyncManager;

public class LoginActivity extends AppCompatActivity {

    private FirebaseShopAuthManager authManager;

    private EditText etLoginId, etPassword;
    private Button btnLogin, btnCreateAccount;
    private TextView btnForgotPassword;
    private ProgressBar progressLogin;
    private ImageButton btnPasswordVisibility;
    private boolean loginPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = FirebaseShopAuthManager.getInstance(this);

        // If already logged in, navigate straight to DailyProductOrdersActivity
        if (authManager.isLoggedIn()) {
            startActivity(new Intent(this, DailyProductOrdersActivity.class));
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        etLoginId = findViewById(R.id.et_login_id);
        etPassword = findViewById(R.id.et_password);
        btnPasswordVisibility = findViewById(R.id.btn_password_visibility);
        btnLogin = findViewById(R.id.btn_login);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
        progressLogin = findViewById(R.id.progress_login);

        // Pre-fill previously remembered login ID if available
        String remembered = authManager.getLoginId();
        if (!remembered.isEmpty()) {
            etLoginId.setText(remembered);
            etPassword.requestFocus();
        }

        btnLogin.setOnClickListener(v -> handleLogin());
        btnCreateAccount.setOnClickListener(v -> showCreateAccountDialog());
        btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        btnPasswordVisibility.setOnClickListener(v -> {
            loginPasswordVisible = !loginPasswordVisible;
            updatePasswordVisibility(etPassword, btnPasswordVisibility, loginPasswordVisible);
        });
    }

    private void handleLogin() {
        String loginId = etLoginId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (loginId.isEmpty()) {
            Toast.makeText(this, "Please enter your Login ID", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        authManager.login(loginId, password, new FirebaseShopAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                // Start real-time sync with cloud Firestore
                FirebaseSyncManager.getInstance(LoginActivity.this).startSync();

                Intent intent = new Intent(LoginActivity.this, DailyProductOrdersActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                showAuthError("Login Failed", errorMessage);
            }
        });
    }

    private void showCreateAccountDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_create_shop_account, null, false);
        EditText etShopName = content.findViewById(R.id.et_new_shop_name);
        EditText etNewLoginId = content.findViewById(R.id.et_new_login_id);
        EditText etNewPassword = content.findViewById(R.id.et_new_password);
        Button btnSubmit = content.findViewById(R.id.btn_create_submit);
        Button btnCancel = content.findViewById(R.id.btn_create_cancel);
        ImageButton btnClose = content.findViewById(R.id.btn_create_close);
        ImageButton btnNewPasswordVisibility = content.findViewById(R.id.btn_new_password_visibility);
        ProgressBar progress = content.findViewById(R.id.progress_create_account);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(content).create();
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int horizontalMargin = (int) (20 * getResources().getDisplayMetrics().density);
                window.setLayout(getResources().getDisplayMetrics().widthPixels - 2 * horizontalMargin,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                WindowManager.LayoutParams params = window.getAttributes();
                params.dimAmount = 0.42f;
                window.setAttributes(params);
            }
        });
        dialog.setCanceledOnTouchOutside(true);

        final boolean[] newPasswordVisible = {false};
        btnNewPasswordVisibility.setOnClickListener(v -> {
            newPasswordVisible[0] = !newPasswordVisible[0];
            updatePasswordVisibility(etNewPassword, btnNewPasswordVisibility, newPasswordVisible[0]);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String shopName = etShopName.getText().toString().trim();
            String loginId = etNewLoginId.getText().toString().trim();
            String password = etNewPassword.getText().toString().trim();
            setCreateDialogLoading(dialog, btnSubmit, btnCancel, btnClose, etShopName, etNewLoginId,
                    etNewPassword, progress, true);

            authManager.createShopAccount(shopName, loginId, password, new FirebaseShopAuthManager.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    if (isFinishing()) return;
                    dialog.dismiss();
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    FirebaseSyncManager.getInstance(LoginActivity.this).startSync();

                    Intent intent = new Intent(LoginActivity.this, DailyProductOrdersActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (isFinishing()) return;
                    setCreateDialogLoading(dialog, btnSubmit, btnCancel, btnClose, etShopName, etNewLoginId,
                            etNewPassword, progress, false);
                    showAuthError("Account Could Not Be Created", errorMessage);
                }
            });
        });
        dialog.show();
    }

    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Password Recovery")
                .setMessage("Order Analyzer shop accounts use private Login IDs for cross-device shared access without exposing personal email addresses.\n\n"
                        + "To reset or change your shop password:\n\n"
                        + "• From any logged-in phone: Go to Shop Profile > Change Shop Password to set a new password at any time.\n\n"
                        + "• From Firebase Console: Your administrator can set a new password in Firebase Console > Authentication > Users > Reset Password.\n\n"
                        + "Once updated, each device used by this shop can log in with the new credentials.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "" : "LOG IN");
        btnCreateAccount.setEnabled(!loading);
        etLoginId.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        btnPasswordVisibility.setEnabled(!loading);
        btnForgotPassword.setEnabled(!loading);
    }

    private void setCreateDialogLoading(AlertDialog dialog, Button submit, Button cancel, ImageButton close,
                                        EditText shopName, EditText loginId, EditText password,
                                        ProgressBar progress, boolean loading) {
        dialog.setCancelable(!loading);
        dialog.setCanceledOnTouchOutside(!loading);
        submit.setEnabled(!loading);
        submit.setText(loading ? "" : "CREATE ACCOUNT");
        cancel.setEnabled(!loading);
        close.setEnabled(!loading);
        shopName.setEnabled(!loading);
        loginId.setEnabled(!loading);
        password.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updatePasswordVisibility(EditText field, ImageButton toggle, boolean visible) {
        int selection = field.getSelectionStart();
        field.setTransformationMethod(visible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        field.setSelection(Math.max(0, Math.min(selection, field.length())));
        toggle.setImageResource(visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        toggle.setContentDescription(visible ? "Hide password" : "Show password");
    }

    private void showAuthError(String title, String rawMessage) {
        String safeMessage = friendlyAuthError(rawMessage);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(safeMessage)
                .setPositiveButton("OK", null)
                .show();
    }

    private String friendlyAuthError(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.toLowerCase(java.util.Locale.US);
        if (message.contains("already") && (message.contains("in use") || message.contains("taken"))) {
            return "That Login ID is already in use. Choose a different Login ID.";
        }
        if (message.contains("invalid login") || message.contains("login id must") || message.contains("valid login id")) {
            return rawMessage;
        }
        if (message.contains("password") && (message.contains("6") || message.contains("weak"))) {
            return "Choose a password with at least 6 characters.";
        }
        if (message.contains("firebase configuration") || message.contains("not configured")) {
            return "Account services are not configured on this app build yet. Please contact the app administrator.";
        }
        if (message.contains("network") || message.contains("timeout") || message.contains("timed out")
                || message.contains("unavailable") || message.contains("connection")) {
            return "We couldn’t connect. Check your internet connection and try again.";
        }
        if (message.contains("invalid") || message.contains("credential") || message.contains("wrong-password")
                || message.contains("user-not-found")) {
            return "The Login ID or password is incorrect. Check your details and try again.";
        }
        return "We couldn’t complete that request. Please try again in a moment.";
    }
}
