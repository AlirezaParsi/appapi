package com.appapi.al;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class PackageChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "PackageChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            Log.i(TAG, "Package change detected: " + action);
            // Clear cache
            SharedPreferences prefs = context.getSharedPreferences("AppApiPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("app_cache");
            editor.remove("cache_timestamp");
            editor.apply();
            Log.i(TAG, "Cache cleared due to package change");
        }
    }
}
