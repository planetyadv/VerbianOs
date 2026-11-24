package com.verbian.launcher;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.verbian.launcher.model.FileModel;
import com.verbian.launcher.ui.FileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileManagerActivity extends AppCompatActivity {

    private TextView tvPath;
    private RecyclerView rvFiles;
    private Button btnBack;
    private Button btnStorageInternal;
    private Button btnStorageUsb;
    private Button btnStorageDownloads;

    private File currentDirectory;
    private File internalRoot;
    private final List<FileModel> fileList = new ArrayList<>();
    private FileAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);

        initViews();
        setupRecycler();
        setupButtons();

        internalRoot = Environment.getExternalStorageDirectory();
        navigateTo(internalRoot);
    }

    private void initViews() {
        tvPath = findViewById(R.id.tv_current_path);
        rvFiles = findViewById(R.id.rv_files);
        btnBack = findViewById(R.id.btn_back);
        btnStorageInternal = findViewById(R.id.btn_storage_internal);
        btnStorageUsb = findViewById(R.id.btn_storage_usb);
        btnStorageDownloads = findViewById(R.id.btn_storage_downloads);
    }

    private void setupRecycler() {
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this, fileList, new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileModel file) {
                if (file.isDirectory()) {
                    navigateTo(file.getFile());
                } else if (file.isApk()) {
                    installApk(file.getFile());
                } else {
                    openGenericFile(file.getFile());
                }
            }

            @Override
            public void onFileLongClick(FileModel file) {
                showFileOptionsDialog(file);
            }
        });
        rvFiles.setAdapter(adapter);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> handleBackNavigation());

        btnStorageInternal.setOnClickListener(v -> navigateTo(Environment.getExternalStorageDirectory()));

        btnStorageDownloads.setOnClickListener(v -> {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads != null && downloads.exists()) {
                navigateTo(downloads);
            } else {
                navigateTo(new File(Environment.getExternalStorageDirectory(), "Download"));
            }
        });

        btnStorageUsb.setOnClickListener(v -> findAndOpenUsbDrive());
    }

    private void navigateTo(File dir) {
        if (dir == null || !dir.exists()) {
            Toast.makeText(this, "Directory unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDirectory = dir;
        tvPath.setText(dir.getAbsolutePath());

        executor.execute(() -> {
            File[] files = dir.listFiles();
            List<FileModel> results = new ArrayList<>();

            if (files != null) {
                List<File> list = Arrays.asList(files);
                Collections.sort(list, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });

                for (File f : list) {
                    if (!f.getName().startsWith(".")) {
                        results.add(new FileModel(f));
                    }
                }
            }

            mainHandler.post(() -> {
                fileList.clear();
                fileList.addAll(results);
                adapter.notifyDataSetChanged();
                if (!fileList.isEmpty() && rvFiles.getChildCount() > 0) {
                    rvFiles.getChildAt(0).requestFocus();
                }
            });
        });
    }

    private void findAndOpenUsbDrive() {
        File usb = detectUsbDrive();
        if (usb != null && usb.exists()) {
            Toast.makeText(this, "USB Found: " + usb.getName(), Toast.LENGTH_SHORT).show();
            navigateTo(usb);
        } else {
            Toast.makeText(this, "No USB flash drive detected", Toast.LENGTH_SHORT).show();
        }
    }

    private File detectUsbDrive() {
        // Strategy 1: Check ContextCompat external files
        File[] dirs = ContextCompat.getExternalFilesDirs(this, null);
        if (dirs != null && dirs.length > 1 && dirs[1] != null) {
            String path = dirs[1].getAbsolutePath();
            int idx = path.indexOf("/Android/data");
            if (idx != -1) {
                File root = new File(path.substring(0, idx));
                if (root.exists()) return root;
            }
        }

        // Strategy 2: Check /storage mounts
        File storage = new File("/storage");
        if (storage.exists() && storage.isDirectory()) {
            File[] mounts = storage.listFiles();
            if (mounts != null) {
                for (File f : mounts) {
                    String name = f.getName().toLowerCase();
                    if (!name.equals("emulated") && !name.equals("self") && f.canRead()) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private void installApk(File apkFile) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot install APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openGenericFile(File file) {
        try {
            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mime == null) mime = "*/*";

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFileOptionsDialog(FileModel file) {
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setMessage("Path: " + file.getFile().getAbsolutePath() + "\nSize: " + file.getSizeFormatted())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (file.getFile().delete()) {
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        navigateTo(currentDirectory);
                    } else {
                        Toast.makeText(this, "Could not delete", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleBackNavigation() {
        if (currentDirectory != null && currentDirectory.getParentFile() != null
                && !currentDirectory.equals(internalRoot)
                && currentDirectory.canRead()) {
            navigateTo(currentDirectory.getParentFile());
        } else {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackNavigation();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
