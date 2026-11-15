package com.xapps.media.xmusic.callback;
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

public final class CallbackInterface {

    private static ActivityCallback activityCallback;
    private static ServiceCallback serviceCallback;
    private static FragmentCallback songsFragmentCallback, searchFragmentCallback, settingsFragmentCallback, subFragmentCallback;

    private CallbackInterface() {
    }

    public static void setActivityCallback(ActivityCallback callback) {
        activityCallback = callback;
    }

    public static void clearActivityCallback(ActivityCallback callback) {
        if (activityCallback == callback) {
            activityCallback = null;
        }
    }
    
    public static void setSongsFragCallback(FragmentCallback callback) {
        songsFragmentCallback = callback;
    }

    public static void clearSongsFragCallback(FragmentCallback callback) {
        if (songsFragmentCallback == callback) {
            songsFragmentCallback = null;
        }
    }
    
    public static void setMlFragCallback(FragmentCallback callback) {
        setSongsFragCallback(callback);
    }

    public static void clearMlFragCallback(FragmentCallback callback) {
        clearSongsFragCallback(callback);
    }
    
    public static void setSettingsFragCallback(FragmentCallback callback) {
        settingsFragmentCallback = callback;
    }

    public static void clearSettingsFragCallback(FragmentCallback callback) {
        if (settingsFragmentCallback == callback) {
            settingsFragmentCallback = null;
        }
    }

    public static void setSgFragCallback(FragmentCallback callback) {
        setSettingsFragCallback(callback);
    }

    public static void clearSgFragCallback(FragmentCallback callback) {
        clearSettingsFragCallback(callback);
    }
    
    public static void setSearchFragCallback(FragmentCallback callback) {
        searchFragmentCallback = callback;
    }

    public static void clearSearchFragCallback(FragmentCallback callback) {
        if (searchFragmentCallback == callback) {
            searchFragmentCallback = null;
        }
    }

    public static void setSrFragCallback(FragmentCallback callback) {
        setSearchFragCallback(callback);
    }

    public static void clearSrFragCallback(FragmentCallback callback) {
        clearSearchFragCallback(callback);
    }
    
    public static void setSubFragmentCallback(FragmentCallback callback) {
        subFragmentCallback = callback;
    }
    
    public static void clearSubFragmentCallback(FragmentCallback callback) {
        if (callback == subFragmentCallback) subFragmentCallback = null;
    }

    public static ActivityCallback activity() {
        return activityCallback;
    }
    
    public static FragmentCallback subFrag() {
        return subFragmentCallback;
    }
    
    public static FragmentCallback songsFrag() {
        return songsFragmentCallback;
    }

    public static FragmentCallback mlFrag() {
        return songsFrag();
    }
    
    public static FragmentCallback settingsFrag() {
        return settingsFragmentCallback;
    }

    public static FragmentCallback sgFrag() {
        return settingsFrag();
    }
    
    public static FragmentCallback searchFrag() {
        return searchFragmentCallback;
    }

    public static FragmentCallback srFrag() {
        return searchFrag();
    }

    public static void setServiceCallback(ServiceCallback callback) {
        serviceCallback = callback;
    }

    public static void clearServiceCallback(ServiceCallback callback) {
        if (serviceCallback == callback) {
            serviceCallback = null;
        }
    }

    public static ServiceCallback service() {
        return serviceCallback;
    }
}