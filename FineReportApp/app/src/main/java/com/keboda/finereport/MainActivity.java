package com.keboda.finereport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.keboda.finereport.ApiResponse;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {
    private static final String TAG = "TVUrlDisplay";
    private static final String PREFS_NAME = "TVAppPrefs";
    private static final String URL_KEY = "current_url";
    private static final String SERVER_URL = "http://172.17.199.141:3000/api/devices/";
    private static final String WS_SERVER_URL = "ws://172.17.199.141:3000";
    private Uri pendingInstallUri = null;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 100;
    private static final int INSTALL_PERMISSION_REQUEST_CODE = 101;
    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;
    private long backPressedTime = 0;
    private Toast backToast;
    private String deviceId;
    private FrameLayout container;
    private XWalkView xWalkView;
    private WebView webView;
    private WebSocketClient webSocketClient;
    private final Gson gson = new Gson();
    private final Handler handler = new Handler();
    private Runnable reconnectRunnable;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_main);
        container = findViewById(R.id.container);
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "Device ID: " + deviceId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        }

        if (isNetworkConnected()) {
            checkAndLoadUrl();
            initWebSocket();
        } else {
            displayMessage("无网络连接，请检查网络设置");
        }
        registerDownloadReceiver();
        checkStoragePermission();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * 初始化获取本地Url，未找到通过API请求
     */
    private void checkAndLoadUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUrl = prefs.getString(URL_KEY, "");
        Log.d(TAG, "SharedPreferences URL: " + currentUrl);
        if (currentUrl.isEmpty()) {
            fetchUrlFromServer();
        } else {
            loadUrl(currentUrl);
        }
    }

    /**
     * 请求Url
     */
    @SuppressLint("StaticFieldLeak")
    private void fetchUrlFromServer() {
        Log.d(TAG, "Fetching URL from server...");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SERVER_URL + deviceId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error fetching URL from server", e);
                showDeviceInfo();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    showDeviceInfo();
                    return;
                }

                try {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    ApiResponse apiResponse = gson.fromJson(responseData, ApiResponse.class);
                    if (apiResponse.getCode() != 200) {
                        throw new RuntimeException("url请求失败: " + apiResponse.getMessage());
                    }

                    JsonObject json = gson.fromJson(gson.toJson(apiResponse.getData()), JsonObject.class);
                    Log.d(TAG, json.toString());
                    String url = json.get("url").getAsString();

                    runOnUiThread(() -> {
                        if (!url.isEmpty()) {
                            Log.d(TAG, "Received URL from server: " + url);
                            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putString(URL_KEY, url);
                            editor.apply();
                            loadUrl(url);
                        } else {
                            Log.d(TAG, "No URL available from server");
                            showDeviceInfo();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response", e);
                    showDeviceInfo();
                }
            }
        });
    }

    /**
     * 初始化WebView/XWalkView，加载Url
     *
     * @param url 地址
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void loadUrl(String url) {
        Log.d(TAG, "Loading URL: " + url);
        container.removeAllViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (webView == null) {
                webView = new WebView(MainActivity.this);
                webView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setBuiltInZoomControls(false);
                webSettings.setDisplayZoomControls(false);
                webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "Page loaded: " + url);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });
                webView.setWebChromeClient(new WebChromeClient());
            }
            container.addView(webView);
            webView.loadUrl(url);
        } else {
            if (xWalkView == null) {
                xWalkView = new XWalkView(MainActivity.this);
                xWalkView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                XWalkSettings settings = xWalkView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setLoadWithOverviewMode(true);
                settings.setUseWideViewPort(true);

                xWalkView.setResourceClient(new XWalkResourceClient(xWalkView) {
                    @Override
                    public void onLoadFinished(XWalkView view, String url) {
                        super.onLoadFinished(view, url);
                        Log.d(TAG, "XWalkView page loaded: " + url);
                    }

                    @Override
                    public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView view, XWalkWebResourceRequest request) {
                        return super.shouldInterceptLoadRequest(view, request);
                    }
                });
            }
            container.addView(xWalkView);
            xWalkView.load(url, null);
        }
    }


    private void showDeviceInfo() {
        displayMessage("本机IP: " + getLocalIpAddress() + "\n本机DEVICE_ID: " + deviceId + "\n请联系管理员设置看板URL");
    }

    /**
     * 清空容器显示信息
     *
     * @param message 信息
     */
    private void displayMessage(String message) {
        Log.d(TAG, "Displaying message: " + message);
        runOnUiThread(() -> {
            container.removeAllViews();
            TextView textView = new TextView(this);
            textView.setText(message);
            textView.setTextSize(24);
            textView.setGravity(Gravity.CENTER);
            container.addView(textView);
        });
    }

    /**
     * 获取本机IP
     *
     * @return ip
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface into = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = into.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "Error getting local IP address", ex);
        }
        return "未知IP";
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setDuration(4000);
        snackbar.show();
    }

    /**
     * WebSocket初始化，发送设备信息注册
     */
    private void initWebSocket() {
        Log.d(TAG, "Initializing WebSocket...");

        try {
            webSocketClient = new WebSocketClient(new URI(WS_SERVER_URL)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    Log.d(TAG, "WebSocket connected");

                    if (webSocketClient != null && webSocketClient.isOpen()) {
                        JsonObject json = new JsonObject();
                        json.addProperty("device_id", deviceId);
                        json.addProperty("ip_address", getLocalIpAddress());
                        json.addProperty("type", "device_register");
                        json.addProperty("version", getCurrentVersion());
                        send(json.toString());
                        Log.d(TAG, "Sent device registration: " + json);
                    }
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WebSocket message received: " + message);

                    try {
                        JsonObject json = gson.fromJson(message, JsonObject.class);

                        if (json.has("type")) {
                            switch (json.get("type").getAsString()) {
                                case "url_update":
                                    fetchUrlFromServer();
                                    break;
                                case "clear_webView":
                                    showDeviceInfo();
                                    break;
                                case "show_info":
                                    showSnackbar(getLocalIpAddress());
                                    break;
                                case "upgrade_app":
                                    checkAppUpdate();
                                    break;
                                default:
                                    Log.d(TAG, "未知的任务类型:" + json.get("type").getAsString());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing WebSocket message", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket closed. Code: " + code + ", Reason: " + reason);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                    scheduleReconnect();
                }
            };
            webSocketClient.setConnectionLostTimeout(30);
            webSocketClient.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "WebSocket URI syntax error", e);
        }
    }

    /**
     * WebSocket重连
     */
    private void scheduleReconnect() {
        Log.d(TAG, "Scheduling WebSocket reconnect...");

        if (reconnectRunnable != null) {
            handler.removeCallbacks(reconnectRunnable);
        }

        final long RECONNECT_DELAY = 10000;

        reconnectRunnable = () -> {
            if (webSocketClient != null && !webSocketClient.isOpen() && isNetworkConnected()) {
                Log.d(TAG, "Attempting WebSocket reconnect...");
                try {
                    webSocketClient.reconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Reconnect failed", e);
                    handler.postDelayed(reconnectRunnable, RECONNECT_DELAY);
                }
            }
        };
        handler.postDelayed(reconnectRunnable, RECONNECT_DELAY);
    }

    /**
     * 检查app更新
     */
    private void checkAppUpdate() {
        Log.d(TAG, "Checking for app updates...");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SERVER_URL + "checkUpgrade")
                .post(new FormBody.Builder().build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error checking for updates", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    return;
                }

                try {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    ApiResponse apiResponse = gson.fromJson(responseData, ApiResponse.class);

                    if (apiResponse.getCode() != 200) {
                        throw new RuntimeException("url请求失败: " + apiResponse.getMessage());
                    }
                    JsonObject data = gson.fromJson(gson.toJson(apiResponse.getData()), JsonObject.class);

                    String latestVersion = data.get("version").getAsString();

                    if(latestVersion.compareTo(getCurrentVersion())>0){
                        String updateMessage = data.get("updateMessage").getAsString();
                        String downloadUrl = data.get("downloadUrl").getAsString();
                        boolean forceUpdate = data.get("forceUpdate").getAsBoolean();

                        runOnUiThread(() -> showUpdateDialog(
                                latestVersion,
                                updateMessage,
                                downloadUrl,
                                forceUpdate
                        ));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing update response", e);
                }
            }
        });
    }

    /**
     * 显示更新对话框
     */
    private void showUpdateDialog(String version, String message, String downloadUrl, boolean forceUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新版本可用 v" + version);
        builder.setMessage(message);
        builder.setPositiveButton("立即更新", (dialog, which) -> {
            downloadApk(downloadUrl);
        });

        if (!forceUpdate) {
            builder.setNegativeButton("稍后再说", null);
        }

        builder.setCancelable(!forceUpdate);
        builder.show();
    }

    /**
     * 下载APK文件
     */
    private void downloadApk(String downloadUrl) {
        Log.d(TAG, "Downloading APK from: " + downloadUrl);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("应用更新");
            request.setDescription("正在下载新版本...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            String fileName = "app_update_" + System.currentTimeMillis() + ".apk";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalFilesDir(this,
                        Environment.DIRECTORY_DOWNLOADS, fileName);
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            }

            request.setMimeType("application/vnd.android.package-archive");

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadId = downloadManager.enqueue(request);

            Toast.makeText(this, "开始下载更新...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Download failed", e);
            Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 注册下载完成接收器
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    checkDownloadStatus(id);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    /**
     * 检查下载状态并安装APK
     */
    private void checkDownloadStatus(long id) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            @SuppressLint("Range") String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

            switch (status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    Uri apkUri = Uri.parse(localUri);
                    Log.d(TAG, "Download completed, URI: " + apkUri);

                    String filePath = apkUri.getPath();
                    if (filePath != null && new File(filePath).exists()) {
                        installApk(apkUri);
                    } else {
                        Toast.makeText(this, "下载文件不存在", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    @SuppressLint("Range") int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                    String errorMsg = "下载失败: " + getDownloadErrorMessage(reason);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        cursor.close();
    }

    /**
     * 获取下载错误信息
     */
    private String getDownloadErrorMessage(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "无法恢复下载";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "存储设备未找到";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "文件已存在";
            case DownloadManager.ERROR_FILE_ERROR:
                return "文件错误";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "HTTP数据传输错误";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "存储空间不足";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "重定向过多";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "未知HTTP错误";
            default:
                return "未知错误";
        }
    }

    /**
     * 安装APK
     */
    private void installApk(Uri apkUri) {
        Log.d(TAG, "Installing APK: " + apkUri);

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        new File(apkUri.getPath()));
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    pendingInstallUri = apkUri;
                    checkInstallPermission();
                    return;
                }
            }

            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Installation failed", e);
            Toast.makeText(this, "安装失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取当前版本号
     */
    private String getCurrentVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting version info", e);
            return "未知版本";
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setTitle("需要存储权限")
                            .setMessage("应用需要存储权限来下载和安装更新")
                            .setPositiveButton("确定", (dialog, which) -> requestStoragePermission())
                            .setNegativeButton("取消", null)
                            .show();
                } else {
                    requestStoragePermission();
                }
            }
        }
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },
                STORAGE_PERMISSION_REQUEST_CODE);
    }

    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予
                Log.d(TAG, "存储权限已授予");
            } else {
                // 权限被拒绝
                Toast.makeText(this, "存储权限被拒绝，无法下载更新", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 检查并请求安装未知来源应用的权限
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean checkInstallPermission() {
        if (getPackageManager().canRequestPackageInstalls()) {
            return true;
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, INSTALL_PERMISSION_REQUEST_CODE);
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    getPackageManager().canRequestPackageInstalls()) {
                // 用户已授予安装权限，可以继续安装
                if (pendingInstallUri != null) {
                    installApk(pendingInstallUri);
                    pendingInstallUri = null;
                }
            } else {
                Toast.makeText(this, "安装权限被拒绝，无法安装更新", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");

        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
        }

        if (webSocketClient != null) {
            webSocketClient.close();
        }

        if (reconnectRunnable != null) {
            handler.removeCallbacks(reconnectRunnable);
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        if (xWalkView != null) {
            xWalkView.onDestroy();
            xWalkView = null;
        }

        pendingInstallUri = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
        if (xWalkView != null) {
            xWalkView.pauseTimers();
            xWalkView.onHide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        if (xWalkView != null) {
            xWalkView.onShow();
            xWalkView.resumeTimers();
        }
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

}