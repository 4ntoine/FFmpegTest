package com.splinex.streaming;

import android.text.TextUtils;

@SuppressWarnings("SameParameterValue")
public final class Log {
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "TwoCamera";

// -------------------------- STATIC METHODS --------------------------

    public static void d(String msg) {
        android.util.Log.d(TAG, getLocation() + msg);
    }

    private static String getLocation() {
        final String className = Log.class.getName();
        final StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        boolean found = false;

        for (StackTraceElement trace : traces) {
            try {
                if (found) {
                    if (!trace.getClassName().startsWith(className)) {
                        Class<?> clazz = Class.forName(trace.getClassName());
                        return "[" + getClassName(clazz) + ":" + trace.getMethodName() + ":" + trace.getLineNumber() + "]: ";
                    }
                } else if (trace.getClassName().startsWith(className)) {
                    found = true;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        return "[]: ";
    }

    private static String getClassName(Class<?> clazz) {
        if (clazz != null) {
            if (!TextUtils.isEmpty(clazz.getSimpleName())) {
                return clazz.getSimpleName();
            }

            return getClassName(clazz.getEnclosingClass());
        }

        return "";
    }

    public static void e(String msg) {
        android.util.Log.e(TAG, getLocation() + msg);
    }

    public static void e(String msg, Throwable throwable) {
        android.util.Log.e(TAG, getLocation() + msg, throwable);
    }

    public static void i(String msg) {
        android.util.Log.i(TAG, getLocation() + msg);
    }

    public static void i(String msg, Throwable throwable) {
        android.util.Log.i(TAG, getLocation() + msg, throwable);
    }

    public static void v(String msg, Throwable throwable) {
        android.util.Log.v(TAG, getLocation() + msg, throwable);
    }

    public static void v(String msg) {
        android.util.Log.v(TAG, getLocation() + msg);
    }

    public static void w(String msg, Throwable throwable) {
        android.util.Log.w(TAG, getLocation() + msg, throwable);
    }

    public static void w(String msg) {
        android.util.Log.w(TAG, getLocation() + msg);
    }
}

