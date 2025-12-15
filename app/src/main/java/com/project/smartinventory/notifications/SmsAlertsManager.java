package com.project.smartinventory.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

public class SmsAlertsManager {

    public static boolean hasPermission(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canSend(Context c) {
        return SmsPrefs.isEnabled(c)
                && hasPermission(c)
                && !SmsPrefs.getNumber(c).trim().isEmpty();
    }

    public static void sendTest(Context c) { // safe no-op if not allowed
        if (!canSend(c)) return;
        send(c, SmsPrefs.getNumber(c), "Test SMS from Smart Inventory ✅");
    }

    public static void sendLowStock(Context c, String item, int qty) {
        if (!canSend(c) || !SmsPrefs.lowStock(c)) return;
        send(c, SmsPrefs.getNumber(c), "Low stock: " + item + " (qty: " + qty + ")");
    }

    public static void sendOutOfStock(Context c, String item) {
        if (!canSend(c) || !SmsPrefs.outOfStock(c)) return;
        send(c, SmsPrefs.getNumber(c), "Out of stock: " + item);
    }

    private static void send(Context c, String to, String msg) {
        SmsManager sm = SmsManager.getDefault();
        PendingIntent sentPI = PendingIntent.getBroadcast(
                c, 0, new Intent("SMS_SENT"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(
                c, 0, new Intent("SMS_DELIVERED"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        sm.sendTextMessage(to, null, msg, sentPI, deliveredPI);
    }

    /** Nice fallback: open SMS app with pre-filled text if permission/setting is off */
    public static void openComposer(Context c, String body) {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(android.net.Uri.parse("smsto:" + SmsPrefs.getNumber(c)));
        i.putExtra("sms_body", body);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }
}
