package com.verbian.launcher.model;

import android.graphics.drawable.Drawable;

public class AppModel {
    private String title;
    private String packageName;
    private String activityName;
    private Drawable icon;
    private boolean isWebShortcut;
    private String webUrl;
    private long lastUsed;

    public AppModel(String title, String packageName, String activityName, Drawable icon) {
        this.title = title;
        this.packageName = packageName;
        this.activityName = activityName;
        this.icon = icon;
        this.isWebShortcut = false;
        this.webUrl = null;
        this.lastUsed = System.currentTimeMillis();
    }

    public AppModel(String title, String webUrl, Drawable icon) {
        this.title = title;
        this.packageName = "web:" + webUrl;
        this.activityName = "";
        this.icon = icon;
        this.isWebShortcut = true;
        this.webUrl = webUrl;
        this.lastUsed = System.currentTimeMillis();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isWebShortcut() {
        return isWebShortcut;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
