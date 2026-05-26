package com.xapps.media.xmusic.utils;

import com.xapps.media.xmusic.BuildConfig;

public final class Log {

    public static void v(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            android.util.Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            android.util.Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            android.util.Log.e(tag, msg, tr);
        }
    }

    public static void wtf(String tag, String msg) {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            android.util.Log.wtf(tag, msg);
        }
    }
}
