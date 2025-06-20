package com.keboda.finereport;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.keboda.finereport.network.ApiResponse;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {
    private static final String TAG = "TVUrlDisplay";
    private static final String PREFS_NAME = "TVAppPrefs";
    private static final String URL_KEY = "current_url";
    private static final String SERVER_URL = "http://172.17.168.89:3000/api/devices/";
    private static final String WS_SERVER_URL = "ws://172.17.168.89:3000";
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
                    String url =  json.get("url").getAsString();

                    runOnUiThread(() -> {
                        if (!url.isEmpty()) {
                            Log.d(TAG, "Received URL from server: " + url);
                            if(url.equals(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(URL_KEY, ""))){
                                return;
                            }
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
            // Android 6.0以下使用XWalkView
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

                    JsonObject json = new JsonObject();
                    json.addProperty("device_id", deviceId);
                    json.addProperty("ip_address", getLocalIpAddress());
                    json.addProperty("type", "device_register");
                    send(json.toString());
                    Log.d(TAG, "Sent device registration: " + json);
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

        reconnectRunnable = () -> {
            if (webSocketClient != null && !webSocketClient.isOpen() && isNetworkConnected()) {
                Log.d(TAG, "Attempting WebSocket reconnect...");
                webSocketClient.reconnect();
            }
        };
        handler.postDelayed(reconnectRunnable, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");

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