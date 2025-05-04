package com.appapi.al;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;
import android.content.SharedPreferences;

public class AppApiService extends Service {

    private static final String TAG = "AppApiService";
    private static final int PORT = 8080;
    private static final String CACHE_KEY = "app_cache";
    private static final String TIMESTAMP_KEY = "cache_timestamp";
    private static final long CACHE_TIMEOUT = TimeUnit.HOURS.toMillis(48);
    private static final int ICON_SIZE = 48;

    private HttpServer server;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("AppApiPrefs", MODE_PRIVATE);
        server = new HttpServer();
        try {
            server.start();
            Log.i(TAG, "Server started on port " + PORT);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            Log.i(TAG, "Server stopped");
        }
    }

    private class HttpServer extends NanoHTTPD {

        public HttpServer() {
            super(PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (session.getUri().equals("/api/apps")) {
                try {
                    String cachedData = prefs.getString(CACHE_KEY, null);
                    long timestamp = prefs.getLong(TIMESTAMP_KEY, 0);
                    if (cachedData != null && System.currentTimeMillis() - timestamp < CACHE_TIMEOUT) {
                        Log.i(TAG, "Serving cached app data");
                        return newFixedLengthResponse(Response.Status.OK, "application/json", cachedData);
                    }

                    JSONArray apps = getAppList();
                    String jsonResponse = apps.toString();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(CACHE_KEY, jsonResponse);
                    editor.putLong(TIMESTAMP_KEY, System.currentTimeMillis());
                    editor.apply();
                    Log.i(TAG, "Serving fresh app data");

                    return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);
                } catch (Exception e) {
                    Log.e(TAG, "Error serving /api/apps", e);
                    JSONObject error = new JSONObject();
                    try {
                        error.put("error", e.getMessage());
                    } catch (Exception ignored) {}
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", error.toString());
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
        }

        private JSONArray getAppList() throws Exception {
            JSONArray apps = new JSONArray();
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo app : packages) {
                // Filter third-party apps
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    JSONObject appInfo = new JSONObject();
                    appInfo.put("package_name", app.packageName);
                    appInfo.put("app_name", pm.getApplicationLabel(app).toString());

                    // Get and encode icon
                    String iconBase64 = getIconBase64(pm, app);
                    appInfo.put("icon", iconBase64 != null ? "data:image/png;base64," + iconBase64 : "");

                    apps.put(appInfo);
                }
            }
            return apps;
        }

        private String getIconBase64(PackageManager pm, ApplicationInfo app) {
            try {
                Drawable icon = pm.getApplicationIcon(app);
                if (icon instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] bytes = baos.toByteArray();
                    return Base64.encodeToString(bytes, Base64.NO_WRAP);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error encoding icon for " + app.packageName, e);
            }
            return null;
        }
    }
}
