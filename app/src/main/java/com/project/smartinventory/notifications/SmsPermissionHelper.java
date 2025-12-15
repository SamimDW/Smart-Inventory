// SmsPermissionHelper.java
package com.project.smartinventory.notifications;

import android.Manifest;
import android.app.Activity;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SmsPermissionHelper {

    public interface Callback {
        void onGranted();
        void onDenied();
    }

    public static ActivityResultLauncher<String> registerLauncher(
            // Change Activity to ComponentActivity here
            ComponentActivity activity, // Changed from Activity
            Callback cb) {
        return activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) cb.onGranted(); else cb.onDenied(); }
        );
    }

    public static void requestWithRationale(
            // Change Activity to ComponentActivity here as well for consistency
            ComponentActivity activity, // Changed from Activity
            ActivityResultLauncher<String> launcher,
            Runnable whenUserCancels // optional
    ) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("SMS Permission")
                .setMessage("To send you alerts as text messages, we need SMS permission. You can turn this off at any time.")
                .setPositiveButton("Continue", (d, w) ->
                        launcher.launch(Manifest.permission.SEND_SMS))
                .setNegativeButton("Not now", (d, w) -> {
                    if (whenUserCancels != null) whenUserCancels.run();
                })
                .show();
    }
}
