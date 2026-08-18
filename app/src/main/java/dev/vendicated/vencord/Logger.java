package dev.vendicated.vencord;

import android.util.Log;

public final class Logger {
    private static final String TAG = "VendroidCanary";

    private Logger() {}

    public static void d(String msg) {
        Log.d(TAG, msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable t) {
        Log.e(TAG, msg, t);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }
}
