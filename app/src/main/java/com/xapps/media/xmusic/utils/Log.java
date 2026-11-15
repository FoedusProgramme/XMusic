package com.xapps.media.xmusic.utils;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

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
