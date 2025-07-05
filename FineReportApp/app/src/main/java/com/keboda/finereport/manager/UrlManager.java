package com.keboda.finereport.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.keboda.finereport.model.ApiResponse;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class UrlManager {
    private static final String TAG = "UrlManager";
    private static final String PREFS_NAME = "TVAppPrefs";
    private static final String URL_KEY = "current_url";

    private final Context context;
    private final String deviceId;
    private final String serverUrl;
    private final Gson gson = new Gson();

    public interface UrlCallback {
        void onUrlLoaded(String url);
        void onError(String message);
    }

    public UrlManager(Context context, String deviceId, String serverUrl) {
        this.context = context;
        this.deviceId = deviceId;
        this.serverUrl = serverUrl;
    }

    public void loadUrl(UrlCallback callback, boolean forceNetwork) {
        if (!forceNetwork) {
            String currentUrl = getStoredUrl();
            if (currentUrl.isEmpty()) {
                fetchUrlFromServer(callback);
            } else {
                callback.onUrlLoaded(currentUrl);
            }
        }else{
            fetchUrlFromServer(callback);
        }

    }
    private void storeUrl(String url) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(URL_KEY, url);
        editor.apply();
        Log.d(TAG, "URL stored: " + url);
    }

    private String getStoredUrl() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentUrl = prefs.getString(URL_KEY, "");
        Log.d(TAG, "SharedPreferences URL: " + currentUrl);
        return currentUrl;
    }

    private void fetchUrlFromServer(UrlCallback callback) {
        Log.d(TAG, "Fetching URL from server...");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl + deviceId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error fetching URL from server", e);
                callback.onError("Error fetching URL from server");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    callback.onError("Unexpected response code: " + response.code());
                    return;
                }

                try {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    ApiResponse apiResponse = gson.fromJson(responseData, ApiResponse.class);
                    if (!apiResponse.getSuccess()) {
                        throw new RuntimeException("url请求失败: " + apiResponse.getMessage());
                    }
                    JsonObject json = gson.fromJson(gson.toJson(apiResponse.getData()), JsonObject.class);
                    Log.d(TAG, json.toString());
                    String url = json.get("url").getAsString();

                    if (!url.isEmpty()) {
                        storeUrl(url);
                        ((Activity)context).runOnUiThread(() -> callback.onUrlLoaded(url));
                    } else {
                        callback.onError("No URL available from server");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response", e);
                    callback.onError("Error parsing server response");
                }
            }
        });
    }
}