package com.verbian.launcher.model;

import java.io.File;

public class FileModel {
    private final File file;
    private final String name;
    private final boolean isDirectory;
    private final boolean isApk;
    private final boolean isMedia;
    private final String sizeFormatted;

    public FileModel(File file) {
        this.file = file;
        this.name = file.getName();
        this.isDirectory = file.isDirectory();

        String lower = name.toLowerCase();
        this.isApk = lower.endsWith(".apk");
        this.isMedia = lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi")
                || lower.endsWith(".mp3") || lower.endsWith(".jpg") || lower.endsWith(".png");

        if (isDirectory) {
            this.sizeFormatted = "Folder";
        } else {
            long bytes = file.length();
            if (bytes < 1024) this.sizeFormatted = bytes + " B";
            else if (bytes < 1024 * 1024) this.sizeFormatted = String.format("%.1f KB", bytes / 1024.0);
            else this.sizeFormatted = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isApk() {
        return isApk;
    }

    public boolean isMedia() {
        return isMedia;
    }

    public String getSizeFormatted() {
        return sizeFormatted;
    }
}
