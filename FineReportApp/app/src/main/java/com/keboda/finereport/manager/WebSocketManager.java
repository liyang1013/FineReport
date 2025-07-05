package com.keboda.finereport.manager;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.keboda.finereport.listener.WebSocketListener;
import com.keboda.finereport.utils.AppUtils;
import com.keboda.finereport.utils.NetworkUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

import android.os.Handler;

public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    private WebSocketClient webSocketClient;
    private final Handler handler;
    private Runnable reconnectRunnable;
    private final Gson gson = new Gson();
    private final String deviceId;
    private final String serverUrl;
    private final Context context;
    private final WebSocketListener listener;

    public WebSocketManager(String deviceId, String serverUrl, WebSocketListener listener, Context context) {
        this.deviceId = deviceId;
        this.serverUrl = serverUrl;
        this.listener = listener;
        this.handler = new Handler();
        this.context = context;
    }

    public void connect() {
        Log.d(TAG, "Initializing WebSocket...");
        try {
            webSocketClient = new WebSocketClient(new URI(serverUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    Log.d(TAG, "WebSocket connected");
                    sendDeviceRegistration();
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WebSocket message received: " + message);
                    processMessage(message);
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

    private void sendDeviceRegistration() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            JsonObject json = new JsonObject();
            json.addProperty("device_id", deviceId);
            json.addProperty("ip_address", NetworkUtils.getLocalIpAddress());
            json.addProperty("type", "device_register");
            json.addProperty("version", AppUtils.getCurrentVersion(context));
            webSocketClient.send(json.toString());
            Log.d(TAG, "Sent device registration: " + json);
        }
    }

    private void processMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            if (json.has("type")) {
                switch (json.get("type").getAsString()) {
                    case "url_update":
                        listener.onUrlUpdate();
                        break;
                    case "clear_webView":
                        listener.onClearWebView();
                        break;
                    case "show_info":
                        listener.onShowInfo(NetworkUtils.getLocalIpAddress());
                        break;
                    case "upgrade_app":
                        listener.onUpgradeApp();
                        break;
                    default:
                        Log.d(TAG, "未知的任务类型:" + json.get("type").getAsString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing WebSocket message", e);
        }
    }

    private void scheduleReconnect() {
        Log.d(TAG, "Scheduling WebSocket reconnect...");
        if (reconnectRunnable != null) {
            handler.removeCallbacks(reconnectRunnable);
        }

        final long RECONNECT_DELAY = 10000;
        reconnectRunnable = () -> {
            if (webSocketClient != null && !webSocketClient.isOpen() && NetworkUtils.isNetworkConnected(context)) {
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

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (reconnectRunnable != null) {
            handler.removeCallbacks(reconnectRunnable);
        }
    }
}