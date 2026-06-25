package com.xapps.media.xmusic.callback;

public final class CallbackInterface {

    private static ActivityCallback activityCallback;
    private static ServiceCallback serviceCallback;
    private static FragmentCallback mlfCallback, searchFragmentCallback, settingsFragmentCallback;

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
    
    public static void setMlFragCallback(FragmentCallback callback) {
        mlfCallback = callback;
    }

    public static void clearMlFragCallback(FragmentCallback callback) {
        if (mlfCallback == callback) {
            mlfCallback = null;
        }
    }
    
    public static void setSgFragCallback(FragmentCallback callback) {
        settingsFragmentCallback = callback;
    }

    public static void clearSgFragCallback(FragmentCallback callback) {
        if (settingsFragmentCallback == callback) {
            settingsFragmentCallback = null;
        }
    }
    
    public static void setSrFragCallback(FragmentCallback callback) {
        searchFragmentCallback = callback;
    }

    public static void clearSrFragCallback(FragmentCallback callback) {
        if (searchFragmentCallback == callback) {
            searchFragmentCallback = null;
        }
    }

    public static ActivityCallback activity() {
        return activityCallback;
    }
    
    public static FragmentCallback mlFrag() {
        return mlfCallback;
    }
    
    public static FragmentCallback sgFrag() {
        return settingsFragmentCallback;
    }
    
    public static FragmentCallback srFrag() {
        return searchFragmentCallback;
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