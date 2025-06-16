package com.keboda.finereport;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

public class MainActivity extends Activity {
    private static final String TAG = "TVUrlDisplay";
    private static final String PREFS_NAME = "TVAppPrefs";
    private static final String URL_KEY = "current_url";
    private static final String SERVER_URL = "http://yourserver.com/api/url";
    private static final String WS_SERVER_URL = "ws://yourserver.com/ws";

    private String deviceId;
    private String currentUrl;
    private FrameLayout container;
    private WebView webView;
    private WebSocketClient webSocketClient;
    private Gson gson = new Gson();
    private Handler handler = new Handler();
    private Runnable reconnectRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "Device ID: " + deviceId);

        if (isNetworkConnected()) {
            checkAndLoadUrl();
//            initWebSocket();
        } else {
            displayMessage("无网络连接，请检查网络设置");
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void checkAndLoadUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUrl = prefs.getString(URL_KEY, "");
//        currentUrl = "http://www.baidu.com";
        Log.d(TAG, "SharedPreferences URL: " + currentUrl);

        if (currentUrl == null || currentUrl.isEmpty()) {
            fetchUrlFromServer();
        } else {
            loadUrl(currentUrl);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void fetchUrlFromServer() {
        Log.d(TAG, "Fetching URL from server...");
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    // 实际项目中实现HTTP请求
                    // 这里使用模拟数据
                    return null;
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching URL from server", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String url) {
                if (url != null && !url.isEmpty()) {
                    Log.d(TAG, "Received URL from server: " + url);

                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(URL_KEY, url);
                    editor.apply();

                    loadUrl(url);
                } else {
                    Log.d(TAG, "No URL available from server");
                    displayIpAndMessage();
                }
            }
        }.execute();
    }

    private void loadUrl(String url) {
        Log.d(TAG, "Loading URL: " + url);
        container.removeAllViews();

        if (webView == null) {
            webView = new WebView(MainActivity.this);
            webView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // 配置WebView
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);

            // 设置PC User-Agent
            webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // 设置WebView客户端
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

            // 设置WebChromeClient用于处理进度等
            webView.setWebChromeClient(new WebChromeClient());
        }

        container.addView(webView);
        webView.loadUrl(url);
    }

    private void displayIpAndMessage() {
        Log.d(TAG, "Displaying IP and message");
        container.removeAllViews();

        String ipAddress = getLocalIpAddress();
        TextView textView = new TextView(this);
        textView.setText("IP: " + ipAddress + "\n请联系管理员设置看板URL");
        textView.setTextSize(24);
        textView.setGravity(Gravity.CENTER);

        container.addView(textView);
    }

    private void displayMessage(String message) {
        Log.d(TAG, "Displaying message: " + message);
        container.removeAllViews();

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(24);
        textView.setGravity(Gravity.CENTER);

        container.addView(textView);
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
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

    private void initWebSocket() {
        Log.d(TAG, "Initializing WebSocket...");

        try {
            webSocketClient = new WebSocketClient(new URI(WS_SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "WebSocket connected");

                    JsonObject json = new JsonObject();
                    json.addProperty("device_id", deviceId);
                    json.addProperty("ip_address", getLocalIpAddress());
                    json.addProperty("type", "device_register");

                    send(json.toString());
                    Log.d(TAG, "Sent device registration: " + json.toString());
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WebSocket message received: " + message);

                    try {
                        JsonObject json = gson.fromJson(message, JsonObject.class);

                        if (json.has("type") && "url_update".equals(json.get("type").getAsString())) {
                            fetchUrlFromServer();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }
}