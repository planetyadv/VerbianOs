package com.verbian.launcher;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.verbian.launcher.model.AppModel;
import com.verbian.launcher.ui.AppAdapter;
import com.verbian.launcher.utils.RecentsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllAppsActivity extends AppCompatActivity {

    private RecyclerView rvAllApps;
    private TextView tvTitle;
    private Button btnBack;
    private final List<AppModel> appList = new ArrayList<>();
    private AppAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);

        rvAllApps = findViewById(R.id.rv_all_apps);
        tvTitle = findViewById(R.id.tv_all_apps_title);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // TV Grid with 4 columns
        rvAllApps.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new AppAdapter(this, appList, new AppAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppModel app) {
                AllAppsActivity.this.onAppClicked(app);
            }

            @Override
            public void onItemLongClick(AppModel app) {
                AllAppsActivity.this.showAppContextMenu(app);
            }
        });
        rvAllApps.setAdapter(adapter);

        loadAllApps();
    }

    private void loadAllApps() {
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<AppModel> foundApps = new ArrayList<>();
            Set<String> addedPackages = new HashSet<>();
            String myPackage = getPackageName();

            // 1. Check TV Leanback launcher apps
            Intent tvIntent = new Intent(Intent.ACTION_MAIN, null);
            tvIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<ResolveInfo> tvApps = pm.queryIntentActivities(tvIntent, 0);

            for (ResolveInfo ri : tvApps) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(myPackage) && addedPackages.add(pkg)) {
                    String title = ri.loadLabel(pm).toString();
                    Drawable icon = ri.loadIcon(pm);
                    foundApps.add(new AppModel(title, pkg, ri.activityInfo.name, icon));
                }
            }

            // 2. Check standard launcher apps
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> standardApps = pm.queryIntentActivities(mainIntent, 0);

            for (ResolveInfo ri : standardApps) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(myPackage) && addedPackages.add(pkg)) {
                    String title = ri.loadLabel(pm).toString();
                    Drawable icon = ri.loadIcon(pm);
                    foundApps.add(new AppModel(title, pkg, ri.activityInfo.name, icon));
                }
            }

            Collections.sort(foundApps, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));

            mainHandler.post(() -> {
                appList.clear();
                appList.addAll(foundApps);
                adapter.notifyDataSetChanged();
                tvTitle.setText(getString(R.string.all_apps_title) + " (" + foundApps.size() + ")");
                if (!appList.isEmpty() && rvAllApps.getChildCount() > 0) {
                    rvAllApps.getChildAt(0).requestFocus();
                }
            });
        });
    }

    private void onAppClicked(AppModel app) {
        RecentsManager.recordLaunch(this, app);
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error launching: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppContextMenu(AppModel app) {
        new AlertDialog.Builder(this)
                .setTitle(app.getTitle())
                .setItems(new String[]{"Launch App", "App Settings / Details", "Uninstall App"}, (dialog, which) -> {
                    if (which == 0) {
                        onAppClicked(app);
                    } else if (which == 1) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + app.getPackageName()));
                        startActivity(intent);
                    } else if (which == 2) {
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        intent.setData(Uri.parse("package:" + app.getPackageName()));
                        startActivity(intent);
                    }
                })
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
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
