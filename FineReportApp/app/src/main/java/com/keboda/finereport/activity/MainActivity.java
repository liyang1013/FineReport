package com.keboda.finereport.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.keboda.finereport.R;
import com.keboda.finereport.listener.WebSocketListener;
import com.keboda.finereport.manager.AppUpgradeManager;
import com.keboda.finereport.manager.UrlManager;
import com.keboda.finereport.manager.WebSocketManager;
import com.keboda.finereport.manager.WebViewManager;
import com.keboda.finereport.utils.AppUtils;
import com.keboda.finereport.utils.NetworkUtils;

public class MainActivity extends Activity implements WebSocketListener, UrlManager.UrlCallback {

    private static final String SERVER_URL = "http://192.168.1.6:3000/api/devices/";
    private static final String WS_SERVER_URL = "ws://192.168.1.6:3000";
    private static final String CHECK_URL = "http://192.168.1.6:3000/api/app/checkUpgrade";
    private long backPressedTime = 0;
    private Toast backToast;
    private FrameLayout container;
    private String deviceId;
    private AppUpgradeManager appUpgradeManager;
    private WebViewManager webViewManager;
    private WebSocketManager webSocketManager;
    private UrlManager urlManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFullscreen();
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        deviceId = AppUtils.getDeviceId(this);
        appUpgradeManager = new AppUpgradeManager(this,this);
        webViewManager = new WebViewManager(this, container);
        webSocketManager = new WebSocketManager(deviceId, WS_SERVER_URL, this,this);
        urlManager = new UrlManager(this, deviceId, SERVER_URL);

        if (NetworkUtils.isNetworkConnected(this)) {
            urlManager.loadUrl(this, false);
            webSocketManager.connect();
        } else {
            displayMessage("无网络连接，请检查网络设置");
        }
    }

    private void setupFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onUrlLoaded(String url) {
        webViewManager.loadUrl(url);
    }

    @Override
    public void onError(String message) {
        showDeviceInfo();
    }

    @Override
    public void onUrlUpdate() {
        urlManager.loadUrl(this, true);
    }

    @Override
    public void onClearWebView() {
        showDeviceInfo();
    }

    @Override
    public void onShowInfo(String message) {
        showSnackbar(message);
    }

    @Override
    public void onUpgradeApp() {
        appUpgradeManager.checkAndUpgrade(CHECK_URL);
    }

    private void showDeviceInfo() {
        displayMessage("本机IP: " + NetworkUtils.getLocalIpAddress() +
                "\n本机DEVICE_ID: " + deviceId +
                "\n请联系管理员设置看板URL");
    }

    private void displayMessage(String message) {
        runOnUiThread(() -> {
            container.removeAllViews();
            TextView textView = new TextView(this);
            textView.setText(message);
            textView.setTextSize(24);
            textView.setGravity(Gravity.CENTER);
            container.addView(textView);
        });
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setDuration(4000);
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appUpgradeManager.unregisterReceiver();
        webSocketManager.disconnect();
        webViewManager.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webViewManager.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webViewManager.onResume();
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