package com.project.smartinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.project.smartinventory.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private View loginButtonContainer;
    private View dividerContainer;
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextInputLayout usernameInputLayout;
    private TextView forgotPasswordText;

    private MaterialButton loginButton;
    private MaterialButton registerButton;

    private LoginViewModel viewModel;

    private boolean isRegisterMode = false;

    private void enterRegisterMode() {
        isRegisterMode = true;

        // Show username input layout
        usernameInputLayout.setVisibility(View.VISIBLE);

        // Hide login-only UI
        loginButtonContainer.setVisibility(View.GONE);
        dividerContainer.setVisibility(View.GONE);

        // Change action buttons
        registerButton.setText(R.string.register_button_confirm_text);
        forgotPasswordText.setText(R.string.cancel_registration);
    }

    private void exitRegisterMode() {
        isRegisterMode = false;

        // Hide username input layout
        usernameInputLayout.setVisibility(View.GONE);


        // Show login UI again
        loginButtonContainer.setVisibility(View.VISIBLE);
        dividerContainer.setVisibility(View.VISIBLE);

        // Clear email and password fields
        usernameEditText.setText("");
        emailEditText.setText("");
        passwordEditText.setText("");

        // Reset texts
        registerButton.setText(R.string.sign_up);
        forgotPasswordText.setText(R.string.forgot_password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // ----- Bind views -----
        loginButtonContainer = findViewById(R.id.loginButtonContainer);
        dividerContainer     = findViewById(R.id.dividerContainer);

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        usernameInputLayout   = findViewById(R.id.usernameInputLayout);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        loginButton    = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);


        // ----- Initial state -----

        // Hide username input layout
        usernameInputLayout.setVisibility(View.GONE);
        isRegisterMode = false;

        // ----- ViewModel -----
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.isLoading.observe(this, loading -> {
            boolean isLoading = loading != null && loading;
            loginButton.setEnabled(!isLoading);
            registerButton.setEnabled(!isLoading);
            loginButton.setText(getString(isLoading
                    ? R.string.login_button_wait
                    : R.string.login_button_text));
        });

        viewModel.isSuccess.observe(this, success -> {
            if (success != null && success) {
                startActivity(new Intent(this, InventoryActivity.class));
                finish();
            }
        });

        viewModel.error.observe(this, code -> {
            if (code == null || code.trim().isEmpty()) return;

            String message = switch (code) {
                case "EMAIL_IN_USE"    -> getString(R.string.err_email_in_use);
                case "WRONG_PASSWORD"  -> getString(R.string.err_wrong_password);
                case "LOGIN_FAILED"    -> getString(R.string.err_login_failed);
                case "REGISTER_FAILED" -> getString(R.string.err_register_failed);
                default -> getString(R.string.error_unknown);
            };

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            viewModel.clearError();
        });

        // ----- Events -----

        // LOGIN: email + password
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            viewModel.login(email, password);
        });

        // REGISTER: toggle mode on first click, register on second
        registerButton.setOnClickListener(v -> {
            if (!isRegisterMode) {
                enterRegisterMode();
                Toast.makeText(this,
                        getString(R.string.register_enter_email_prompt),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String username = usernameEditText.getText().toString().trim();
            String email    = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();

            viewModel.register(username, email, password);
        });

        // FORGOT PASSWORD / CANCEL
        forgotPasswordText.setOnClickListener(v -> {
            if (isRegisterMode) {
                // In register mode → act as cancel
                exitRegisterMode();
            } else {
                // In login mode → placeholder for future reset flow
                Toast.makeText(this,
                        "Password reset coming soon.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
