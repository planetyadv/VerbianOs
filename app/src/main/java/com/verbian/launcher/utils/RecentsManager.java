package com.verbian.launcher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import com.verbian.launcher.R;
import com.verbian.launcher.model.AppModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecentsManager {

    private static final String PREF_NAME = "verbian_launcher_prefs";
    private static final String KEY_RECENTS = "key_recent_items";
    private static final String KEY_WEB_SHORTCUTS = "key_web_shortcuts";
    private static final int MAX_RECENTS = 12;

    public static void recordLaunch(Context context, AppModel app) {
        if (app == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String raw = prefs.getString(KEY_RECENTS, "[]");
            JSONArray array = new JSONArray(raw);
            JSONArray updated = new JSONArray();

            // Add current item first
            JSONObject current = new JSONObject();
            current.put("title", app.getTitle());
            current.put("pkg", app.getPackageName());
            current.put("act", app.getActivityName());
            current.put("isWeb", app.isWebShortcut());
            current.put("url", app.getWebUrl() != null ? app.getWebUrl() : "");
            updated.put(current);

            // Copy others, avoiding duplicates
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String pkg = obj.optString("pkg");
                String url = obj.optString("url");
                if (app.isWebShortcut()) {
                    if (!url.equals(app.getWebUrl())) {
                        updated.put(obj);
                    }
                } else {
                    if (!pkg.equals(app.getPackageName())) {
                        updated.put(obj);
                    }
                }
                if (updated.length() >= MAX_RECENTS) break;
            }

            prefs.edit().putString(KEY_RECENTS, updated.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<AppModel> getRecentItems(Context context) {
        List<AppModel> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        PackageManager pm = context.getPackageManager();

        try {
            String raw = prefs.getString(KEY_RECENTS, null);
            if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) {
                // Initialize default defaults: default web shortcuts and installed TV apps
                list.addAll(getDefaultShortcuts(context));
                return list;
            }

            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                boolean isWeb = obj.optBoolean("isWeb", false);
                String title = obj.optString("title");
                if (isWeb) {
                    String url = obj.optString("url");
                    Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_browser);
                    list.add(new AppModel(title, url, icon));
                } else {
                    String pkg = obj.optString("pkg");
                    try {
                        Drawable icon = pm.getApplicationIcon(pkg);
                        String label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                        list.add(new AppModel(label, pkg, obj.optString("act"), icon));
                    } catch (PackageManager.NameNotFoundException ignored) {
                        // App was uninstalled, skip
                    }
                }
            }

            if (list.isEmpty()) {
                list.addAll(getDefaultShortcuts(context));
            }
        } catch (Exception e) {
            e.printStackTrace();
            list.addAll(getDefaultShortcuts(context));
        }
        return list;
    }

    public static List<AppModel> getDefaultShortcuts(Context context) {
        List<AppModel> defaults = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        // Check if YouTube app is installed
        Intent ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube.tv");
        if (ytIntent == null) {
            ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube");
        }
        if (ytIntent != null) {
            try {
                Drawable icon = pm.getApplicationIcon(ytIntent.getPackage());
                defaults.add(new AppModel("YouTube", ytIntent.getPackage(), "", icon));
            } catch (Exception ignored) {}
        } else {
            // Add YouTube Web shortcut (Extremely low memory footprint!)
            defaults.add(new AppModel("YouTube Lite", "https://m.youtube.com",
                    ContextCompat.getDrawable(context, R.drawable.ic_youtube)));
        }

        // Web Shortcuts with near-zero RAM footprint
        defaults.add(new AppModel("Twitch TV", "https://www.twitch.tv",
                ContextCompat.getDrawable(context, R.drawable.ic_browser)));
        defaults.add(new AppModel("HD Rezeka", "https://rezka.ag",
                ContextCompat.getDrawable(context, R.drawable.ic_browser)));
        defaults.add(new AppModel("Google Web", "https://www.google.com",
                ContextCompat.getDrawable(context, R.drawable.ic_browser)));

        return defaults;
    }

    public static void addCustomWebShortcut(Context context, String title, String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        AppModel model = new AppModel(title, url, ContextCompat.getDrawable(context, R.drawable.ic_browser));
        recordLaunch(context, model);
    }
}
