package com.keboda.finereport.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkView;

public class WebViewManager {
    private static final String TAG = "WebViewManager";
    private final Context context;
    private final FrameLayout container;
    private XWalkView xWalkView;
    private WebView webView;

    public WebViewManager(Context context, FrameLayout container) {
        this.context = context;
        this.container = container;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        }
    }

    public void loadUrl(String url) {
        Log.d(TAG, "Loading URL: " + url);
        container.removeAllViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupWebView(url);
        } else {
            setupXWalkView(url);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(String url) {
        if (webView == null) {
            webView = new WebView(context);
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
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupXWalkView(String url) {
        if (xWalkView == null) {
            xWalkView = new XWalkView(context);
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
            });
        }
        container.addView(xWalkView);
        xWalkView.load(url, null);
    }

    public void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        if (xWalkView != null) {
            xWalkView.pauseTimers();
            xWalkView.onHide();
        }
    }

    public void onResume() {
        if (webView != null) {
            webView.onResume();
        }
        if (xWalkView != null) {
            xWalkView.onShow();
            xWalkView.resumeTimers();
        }
    }

    public void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        if (xWalkView != null) {
            xWalkView.onDestroy();
            xWalkView = null;
        }
    }
}