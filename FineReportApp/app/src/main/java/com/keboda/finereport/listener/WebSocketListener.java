package com.keboda.finereport.listener;

public interface WebSocketListener {
    void onUrlUpdate();
    void onClearWebView();
    void onShowInfo(String message);
    void onUpgradeApp();
}