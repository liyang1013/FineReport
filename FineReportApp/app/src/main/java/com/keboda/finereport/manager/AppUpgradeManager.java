package com.keboda.finereport.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.JsonObject;
import com.keboda.finereport.model.ApiResponse;
import com.keboda.finereport.model.AppVersion;
import com.google.gson.Gson;
import com.keboda.finereport.utils.AppUtils;

import okhttp3.*;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppUpgradeManager {
    private static final String TAG = "AppUpgradeManager";
    private static final String APK_NAME = "app_update.apk";
    private BroadcastReceiver downloadReceiver;
    private final Activity activity;
    private final ExecutorService executorService;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private long downloadId;
    private final Context context;

    public AppUpgradeManager(Activity activity, Context context) {
        this.activity = activity;
        this.executorService = Executors.newSingleThreadExecutor();
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.context = context;
    }

    public void checkAndUpgrade(String checkUrl) {
        executorService.execute(() -> {
            try {
                ApiResponse response = getLatestVersion(checkUrl);
                assert response != null;
                if (!response.getSuccess()) {
                    showToast("检查更新失败: " + response.getMessage());
                    return;
                }

                AppVersion appVersion = gson.fromJson(gson.toJson(response.getData()), AppVersion.class);

                if (!needUpgrade(appVersion.version)) {
                    showToast("当前已是最新版本");
                } else {
                    showUpdateDialog(appVersion);
                }

            } catch (Exception e) {
                Log.e(TAG, "升级出错", e);
                showToast("升级出错: " + e.getMessage());
            }
        });
    }

    public void showUpdateDialog(AppVersion appVersion) {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity).setTitle("发现新版本 " + appVersion.version).setMessage(appVersion.updateMessage).setPositiveButton("立即更新", (dialog, which) -> startDownload(appVersion.downloadUrl)).setNegativeButton(appVersion.forceUpdate == 1 ? "退出" : "稍后再说", (dialog, which) -> {
            if (appVersion.forceUpdate == 1) {
                activity.finish();
            }
        }).setCancelable(appVersion.forceUpdate != 1).show());
    }

    private void startDownload(String downloadUrl) {

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        File apkFile = new File(downloadDir, APK_NAME);
        if (apkFile.exists()) {
            apkFile.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl)).setTitle("应用更新").setDescription("正在下载新版本...").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_NAME).setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = downloadManager.enqueue(request);
        registerDownloadReceiver();
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long receivedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == receivedDownloadId) {
                    checkDownloadStatus();
                }
            }
        };
        ContextCompat.registerReceiver(activity, downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void checkDownloadStatus() {
        DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        installApk();
                        break;
                    case DownloadManager.STATUS_FAILED:
                        showToast("下载失败");
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        showToast("下载已暂停");
                        break;
                    case DownloadManager.STATUS_PENDING:
                        showToast("下载等待中");
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查下载状态失败", e);
        }
    }


    private ApiResponse getLatestVersion(String checkUrl) throws IOException {
        Request request = new Request.Builder().url(checkUrl).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;

            assert response.body() != null;
            String json = response.body().string();
            return gson.fromJson(json, ApiResponse.class);
        }
    }

    private boolean needUpgrade(String latestVersionCode) {
        return latestVersionCode.compareTo(AppUtils.getCurrentVersion(context)) > 0;
    }

    private void installApk() {
        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_NAME);

        if (!apkFile.exists()) {
            showToast("安装文件不存在");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri apkUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            apkUri = Uri.fromFile(apkFile);
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        activity.startActivity(intent);
    }

    private void showToast(String message) {
        activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_LONG).show());
    }

    public void unregisterReceiver() {
        try {
            if (downloadReceiver != null) {
                activity.unregisterReceiver(downloadReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "取消注册广播接收器失败", e);
        }
    }
}