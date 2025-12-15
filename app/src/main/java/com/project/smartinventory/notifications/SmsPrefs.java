// SmsPrefs.java
package com.project.smartinventory.notifications;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SmsPrefs
 *
 * Thin wrapper around {@link SharedPreferences} for SMS alert settings.
 * Stores:
 *  - Master enable flag (on/off)
 *  - Destination phone number
 *  - Low-stock alert toggle
 *  - Out-of-stock alert toggle
 *
 * Notes:
 *  - This is intentionally synchronous and lightweight.
 *  - For production apps, consider migrating to Jetpack DataStore for
 *    transactional, schema-friendly preferences.
 */
public final class SmsPrefs {

    // ---- Storage ----
    private static final String PREFS          = "MyAppPrefs";   // shared with other simple flags
    // ---- Keys ----
    private static final String KEY_SMS_ENABLED = "sms_enabled";
    private static final String KEY_SMS_NUMBER  = "sms_number";
    private static final String KEY_LOW_STOCK   = "sms_low_stock";
    private static final String KEY_OOS         = "sms_oos";

    private SmsPrefs() { /* no instances */ }

    /** Returns the singleton SharedPreferences used by these helpers. */
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Master enable ----

    /** @return whether SMS alerts are globally enabled. Default: false. */
    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_SMS_ENABLED, false);
    }

    /** Sets whether SMS alerts are globally enabled. */
    public static void setEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_SMS_ENABLED, enabled).apply();
    }

    // ---- Phone number ----

    /** @return stored destination phone number (may be empty if unset). */
    public static String getNumber(Context c) {
        return sp(c).getString(KEY_SMS_NUMBER, "");
    }

    /** Persists destination phone number (no validation here; validate at UI layer). */
    public static void setNumber(Context c, String number) {
        sp(c).edit().putString(KEY_SMS_NUMBER, number).apply();
    }

    // ---- Alert toggles ----

    /** @return whether low-stock SMS alerts are enabled. Default: true. */
    public static boolean lowStock(Context c) {
        return sp(c).getBoolean(KEY_LOW_STOCK, true);
    }

    /** Sets whether low-stock SMS alerts are enabled. */
    public static void setLowStock(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_LOW_STOCK, enabled).apply();
    }

    /** @return whether out-of-stock SMS alerts are enabled. Default: true. */
    public static boolean outOfStock(Context c) {
        return sp(c).getBoolean(KEY_OOS, true);
    }

    /** Sets whether out-of-stock SMS alerts are enabled. */
    public static void setOutOfStock(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_OOS, enabled).apply();
    }

    // ---- Utility ----

    /**
     * Clears all SMS-related preferences. Does not affect other app prefs.
     * Use with caution (e.g., when the user disables SMS entirely).
     */
    public static void clear(Context c) {
        sp(c).edit()
                .remove(KEY_SMS_ENABLED)
                .remove(KEY_SMS_NUMBER)
                .remove(KEY_LOW_STOCK)
                .remove(KEY_OOS)
                .apply();
    }
}
