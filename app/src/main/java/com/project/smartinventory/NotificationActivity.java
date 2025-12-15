package com.project.smartinventory;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.project.smartinventory.notifications.SmsAlertsManager;
import com.project.smartinventory.notifications.SmsPrefs;

/**
 * NotificationActivity
 *
 * Screen for configuring SMS-based inventory alerts.
 * Responsibilities:
 *  - Display current SMS permission state and allow requesting it.
 *  - Enable/disable SMS alerts at the app level.
 *  - Persist the target phone number and alert-type preferences.
 *  - Provide a "Send test SMS" action to verify configuration.
 *
 * Design notes:
 *  - UI state is always derived from the single source of truth (permissions + SmsPrefs).
 *  - All user-facing actions call {@link #refreshUi()} to keep visuals in sync.
 *  - Permission request uses the Jetpack Activity Result API for lifecycle safety.
 */
public class NotificationActivity extends AppCompatActivity {

    // ---- Views ----
    private ImageView permissionStatusIcon;
    private TextView permissionStatusText;
    private MaterialButton requestPermissionButton;

    private SwitchMaterial enableNotificationsSwitch;
    private TextInputEditText phoneNumberEditText;
    private SwitchMaterial lowStockAlertsSwitch;
    private SwitchMaterial outOfStockAlertsSwitch;
    private MaterialButton sendTestSmsButton;

    private ActivityResultLauncher<String> smsPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Bind views
        permissionStatusIcon     = findViewById(R.id.permissionStatusIcon);
        permissionStatusText     = findViewById(R.id.permissionStatusText);
        requestPermissionButton  = findViewById(R.id.requestPermissionButton);

        enableNotificationsSwitch = findViewById(R.id.enableNotificationsSwitch);
        phoneNumberEditText       = findViewById(R.id.phoneNumberEditText);
        lowStockAlertsSwitch      = findViewById(R.id.lowStockAlertsSwitch);
        outOfStockAlertsSwitch    = findViewById(R.id.outOfStockAlertsSwitch);
        sendTestSmsButton         = findViewById(R.id.sendTestSmsButton);

        // Initialize from prefs
        enableNotificationsSwitch.setChecked(SmsPrefs.isEnabled(this));
        phoneNumberEditText.setText(SmsPrefs.getNumber(this));
        lowStockAlertsSwitch.setChecked(SmsPrefs.lowStock(this));
        outOfStockAlertsSwitch.setChecked(SmsPrefs.outOfStock(this));

        // Permission launcher
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        Toast.makeText(this, getString(R.string.toast_sms_granted), Toast.LENGTH_SHORT).show();
                        SmsPrefs.setEnabled(this, true);
                    } else {
                        Toast.makeText(this, getString(R.string.toast_sms_denied), Toast.LENGTH_LONG).show();
                        SmsPrefs.setEnabled(this, false);
                        enableNotificationsSwitch.setChecked(false);
                    }
                    refreshUi();
                }
        );

        // Request permission button
        requestPermissionButton.setOnClickListener(v -> requestPermissionWithChecks());

        // Master switch
        enableNotificationsSwitch.setOnCheckedChangeListener((btn, enabled) -> {
            if (enabled) {
                if (ensureNumberSaved()) {
                    enableNotificationsSwitch.setChecked(false);
                    return;
                }
                if (SmsAlertsManager.hasPermission(this)) {
                    SmsPrefs.setEnabled(this, true);
                    refreshUi();
                } else {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
                }
            } else {
                SmsPrefs.setEnabled(this, false);
                refreshUi();
            }
        });

        // Alert toggles
        lowStockAlertsSwitch.setOnCheckedChangeListener((b, value) -> SmsPrefs.setLowStock(this, value));
        outOfStockAlertsSwitch.setOnCheckedChangeListener((b, value) -> SmsPrefs.setOutOfStock(this, value));

        // Test SMS
        sendTestSmsButton.setOnClickListener(v -> {
            if (ensureNumberSaved()) return;
            if (SmsAlertsManager.canSend(this)) {
                SmsAlertsManager.sendTest(this);
                Toast.makeText(this, getString(R.string.toast_sending_test), Toast.LENGTH_SHORT).show();
            } else {
                SmsAlertsManager.openComposer(this, getString(R.string.test_sms_body));
            }
        });

        refreshUi();
    }

    private boolean ensureNumberSaved() {
        String number = String.valueOf(phoneNumberEditText.getText()).trim();
        if (TextUtils.isEmpty(number)) {
            Toast.makeText(this, getString(R.string.toast_enter_number), Toast.LENGTH_SHORT).show();
            phoneNumberEditText.requestFocus();
            return true;
        }
        SmsPrefs.setNumber(this, number);
        return false;
    }

    private void requestPermissionWithChecks() {
        if (ensureNumberSaved()) return;
        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
    }

    private void refreshUi() {
        boolean hasPermission = SmsAlertsManager.hasPermission(this);
        boolean isEnabled     = SmsPrefs.isEnabled(this);
        boolean canSendSms    = SmsAlertsManager.canSend(this);

        // Status visuals (icon + color + text)
        if (hasPermission) {
            permissionStatusIcon.setImageResource(R.drawable.checkmark);
            permissionStatusIcon.setColorFilter(getColor(R.color.success_color));
            permissionStatusText.setText(getString(R.string.status_sms_granted));
        } else {
            permissionStatusIcon.setImageResource(R.drawable.error);
            permissionStatusIcon.setColorFilter(getColor(R.color.warning_color));
            permissionStatusText.setText(getString(R.string.status_sms_required));
        }


        // Control states
        requestPermissionButton.setEnabled(!hasPermission);
        enableNotificationsSwitch.setChecked(isEnabled && hasPermission);
        lowStockAlertsSwitch.setEnabled(isEnabled && hasPermission);
        outOfStockAlertsSwitch.setEnabled(isEnabled && hasPermission);
        phoneNumberEditText.setEnabled(true);
        sendTestSmsButton.setEnabled(canSendSms);
    }
}

