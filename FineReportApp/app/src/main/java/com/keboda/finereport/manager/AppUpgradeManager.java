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
import android.util.Log;
import android.widget.Toast;

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
    private static final String DOWNLOAD_URL_PRE= "http://192.168.1.8/static/";
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
                if (!response.success) {
                    showToast("检查更新失败: " + response.message);
                    return;
                }

                AppVersion appVersion = gson.fromJson(gson.toJson(response.data), AppVersion.class);

                if (AppUtils.getCurrentVersionCode(context) >= appVersion.versionCode ) {
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
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity).setTitle("发现新版本 " + appVersion.versionName).setMessage(appVersion.updateMessage).setPositiveButton("立即更新", (dialog, which) -> startDownload(appVersion.downloadUrl)).setNegativeButton(appVersion.forceUpdate == 1 ? "退出" : "稍后再说", (dialog, which) -> {
            if (appVersion.forceUpdate == 1) {
                activity.finish();
            }
        }).setCancelable(appVersion.forceUpdate != 1).show());
    }

    private void startDownload(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DOWNLOAD_URL_PRE + url))
                .setTitle("应用更新")
                .setDescription("正在下载新版本")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        request.setDestinationUri(Uri.fromFile(new File(downloadsDir, APK_NAME)));

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);
        registerDownloadReceiver();
    }



    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query)) {
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    installApk();
                } else {
                    Log.e(TAG, "下载失败，状态码: " + status);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查下载状态异常", e);
        }
    }

    private void installApk() {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri apkUri = dm.getUriForDownloadedFile(downloadId);

        if (apkUri == null) {
            Log.e(TAG, "安装文件URI为空");
            return;
        }

        Intent install = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(install);
        } catch (Exception e) {
            Log.e(TAG, "启动安装界面失败", e);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        checkDownloadStatus();
                    }
                }
            }
        };

        // 正确的动态注册方式
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(downloadReceiver, filter);
        }
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

    private ApiResponse getLatestVersion(String checkUrl) throws IOException {
        Request request = new Request.Builder().url(checkUrl).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;

            assert response.body() != null;
            String json = response.body().string();
            return gson.fromJson(json, ApiResponse.class);
        }
    }

    private void showToast(String message) {
        activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_LONG).show());
    }

}