package com.project.smartinventory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.project.smartinventory.viewmodel.LoginViewModel;

/**
 * LoginActivity
 *
 * Captures credentials and delegates auth/registration to LoginViewModel.
 * Reacts to loading/success/error states and navigates to InventoryActivity on success.
 */
public class LoginActivity extends AppCompatActivity {

    // ---- UI References ----
    private EditText usernameEditText;
    private EditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton registerButton;

    // ---- ViewModel ----
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Apply insets so content isn't obscured by system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ---- Bind UI ----
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton      = findViewById(R.id.loginButton);
        registerButton   = findViewById(R.id.registerButton);

        // Set static texts from resources (useful if layout didn’t already)
        loginButton.setText(getString(R.string.login_button_text));
        registerButton.setText(getString(R.string.register_button_text));

        // ---- ViewModel ----
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Loading state: disable buttons + change login text
        viewModel.isLoading.observe(this, loading -> {
            boolean isLoading = loading != null && loading;
            loginButton.setEnabled(!isLoading);
            registerButton.setEnabled(!isLoading);
            loginButton.setText(getString(isLoading
                    ? R.string.login_button_wait
                    : R.string.login_button_text));
        });

        // Success: navigate to inventory
        viewModel.isSuccess.observe(this, success -> {
            if (success != null && success) {
                // Optional success toast
                // Toast.makeText(this, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, InventoryActivity.class));
                finish();
            }
        });

        viewModel.error.observe(this, code -> {
            if (code == null || code.trim().isEmpty()) return; // ignore clears / no message

            String message = switch (code) {
                case "USER_NOT_FOUND" -> getString(R.string.err_user_not_found);
                case "USERNAME_TAKEN" -> getString(R.string.err_username_taken);
                case "CREATE_FAILED" -> getString(R.string.err_create_failed);
                default -> getString(R.string.error_unknown);
            };

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            // Clear so it won’t re-toast on rotation or when observers reattach
            viewModel.clearError();
        });

        // ---- Events ----
        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            viewModel.login(username, password);
        });

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            viewModel.register(username, password);
        });
    }
}
