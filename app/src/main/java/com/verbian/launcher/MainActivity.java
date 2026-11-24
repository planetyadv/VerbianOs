package com.verbian.launcher;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.verbian.launcher.model.AppModel;
import com.verbian.launcher.ui.AppAdapter;
import com.verbian.launcher.utils.RamBooster;
import com.verbian.launcher.utils.RecentsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvRecents;
    private TextView tvClock;
    private TextView tvRamStatus;
    private Button btnSeeAll;
    private Button btnAddWeb;
    private Button btnOpenFiles;

    // Sidebar items
    private LinearLayout btnSideYoutube;
    private LinearLayout btnSideBrowser;
    private LinearLayout btnSideFiles;
    private LinearLayout btnSideBoost;
    private LinearLayout btnSideApps;
    private LinearLayout btnSideSettings;

    private final List<AppModel> recentItems = new ArrayList<>();
    private AppAdapter recentsAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvClock != null) {
                tvClock.setText(timeFormat.format(new Date()));
            }
            mainHandler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        setupRecentsRecyclerView();
        checkStoragePermissions();
    }

    private void initViews() {
        rvRecents = findViewById(R.id.rv_recents);
        tvClock = findViewById(R.id.tv_clock);
        tvRamStatus = findViewById(R.id.tv_ram_status);
        btnSeeAll = findViewById(R.id.btn_see_all);
        btnAddWeb = findViewById(R.id.btn_add_web);
        btnOpenFiles = findViewById(R.id.btn_open_files);

        btnSideYoutube = findViewById(R.id.btn_side_youtube);
        btnSideBrowser = findViewById(R.id.btn_side_browser);
        btnSideFiles = findViewById(R.id.btn_side_files);
        btnSideBoost = findViewById(R.id.btn_side_boost);
        btnSideApps = findViewById(R.id.btn_side_apps);
        btnSideSettings = findViewById(R.id.btn_side_settings);

        clockRunnable.run();
    }

    private void setupListeners() {
        // "See all apps on this TV"
        btnSeeAll.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AllAppsActivity.class)));

        // "+ Add Web Shortcut"
        btnAddWeb.setOnClickListener(v -> showAddWebShortcutDialog());

        // "Files / USB" button
        btnOpenFiles.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FileManagerActivity.class)));

        // Sidebar - YouTube
        btnSideYoutube.setOnClickListener(v -> launchYouTube());

        // Sidebar - Lite Web Browser
        btnSideBrowser.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LiteWebActivity.class);
            intent.putExtra(LiteWebActivity.EXTRA_URL, "https://www.google.com");
            intent.putExtra(LiteWebActivity.EXTRA_TITLE, "Google Lite");
            startActivity(intent);
        });

        // Sidebar - Files & USB
        btnSideFiles.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FileManagerActivity.class)));

        // Sidebar - RAM Turbo Boost
        btnSideBoost.setOnClickListener(v -> performRamBoost());

        // Sidebar - All Apps
        btnSideApps.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AllAppsActivity.class)));

        // Sidebar - TV Settings
        btnSideSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Settings unavailable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecentsRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvRecents.setLayoutManager(layoutManager);
        recentsAdapter = new AppAdapter(this, recentItems, new AppAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppModel app) {
                MainActivity.this.onItemClicked(app);
            }

            @Override
            public void onItemLongClick(AppModel app) {
                MainActivity.this.showAppContextMenu(app);
            }
        });
        rvRecents.setAdapter(recentsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentItems();
    }

    private void loadRecentItems() {
        executor.execute(() -> {
            List<AppModel> items = RecentsManager.getRecentItems(MainActivity.this);
            mainHandler.post(() -> {
                recentItems.clear();
                recentItems.addAll(items);
                recentsAdapter.notifyDataSetChanged();

                if (!recentItems.isEmpty() && getCurrentFocus() == null) {
                    rvRecents.post(() -> {
                        View firstChild = rvRecents.getChildAt(0);
                        if (firstChild != null) {
                            firstChild.requestFocus();
                        }
                    });
                }
            });
        });
    }

    private void onItemClicked(AppModel app) {
        if (app.isWebShortcut()) {
            RecentsManager.recordLaunch(this, app);
            Intent intent = new Intent(this, LiteWebActivity.class);
            intent.putExtra(LiteWebActivity.EXTRA_URL, app.getWebUrl());
            intent.putExtra(LiteWebActivity.EXTRA_TITLE, app.getTitle());
            startActivity(intent);
        } else {
            RecentsManager.recordLaunch(this, app);
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    Toast.makeText(this, "Cannot open " + app.getTitle(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAppContextMenu(AppModel app) {
        String[] options;
        if (app.isWebShortcut()) {
            options = new String[]{"Open Shortcut", "Shortcut Info"};
        } else {
            options = new String[]{"Open App", "App Settings / Details", "Uninstall App"};
        }

        new AlertDialog.Builder(this)
                .setTitle(app.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        onItemClicked(app);
                    } else if (which == 1) {
                        if (!app.isWebShortcut()) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + app.getPackageName()));
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "URL: " + app.getWebUrl(), Toast.LENGTH_LONG).show();
                        }
                    } else if (which == 2) {
                        if (!app.isWebShortcut()) {
                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            intent.setData(Uri.parse("package:" + app.getPackageName()));
                            startActivity(intent);
                        }
                    }
                })
                .show();
    }

    private void performRamBoost() {
        executor.execute(() -> {
            RamBooster.BoostResult result = RamBooster.boost(MainActivity.this);
            mainHandler.post(() -> {
                String text = "⚡ Freed: " + result.getFreedMbFormatted() + " | Free RAM: " + result.getAvailableMbFormatted();
                tvRamStatus.setText(text);
                Toast.makeText(MainActivity.this, "🚀 Turbo Boost: Freed " + result.getFreedMbFormatted() + " RAM!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void launchYouTube() {
        PackageManager pm = getPackageManager();
        Intent ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube.tv");
        if (ytIntent == null) {
            ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube");
        }
        if (ytIntent != null) {
            startActivity(ytIntent);
        } else {
            Intent intent = new Intent(this, LiteWebActivity.class);
            intent.putExtra(LiteWebActivity.EXTRA_URL, "https://m.youtube.com");
            intent.putExtra(LiteWebActivity.EXTRA_TITLE, "YouTube Lite");
            startActivity(intent);
        }
    }

    private void showAddWebShortcutDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shortcut, null);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        EditText etUrl = dialogView.findViewById(R.id.et_url);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String url = etUrl.getText().toString().trim();
            if (title.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Please enter title and URL", Toast.LENGTH_SHORT).show();
                return;
            }
            RecentsManager.addCustomWebShortcut(this, title, url);
            dialog.dismiss();
            loadRecentItems();
            Toast.makeText(this, "Web shortcut added!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Environment.isExternalStorageManager() check
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getCurrentFocus() != null && getCurrentFocus() != rvRecents) {
                View firstChild = rvRecents.getChildAt(0);
                if (firstChild != null) {
                    firstChild.requestFocus();
                    return true;
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(clockRunnable);
        executor.shutdown();
        super.onDestroy();
    }
}
