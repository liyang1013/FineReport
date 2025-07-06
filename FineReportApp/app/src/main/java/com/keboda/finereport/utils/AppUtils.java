package com.keboda.finereport.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class AppUtils {
    private static final String TAG = "AppUtils";

    public static String getCurrentVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting versionName info", e);
            return "未知版本";
        }
    }

    public static Integer getCurrentVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting versionCode info", e);
            return -1;
        }
    }

    public static String getDeviceManufacturer() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            return "huawei";
        } else if (manufacturer.contains("xiaomi")) {
            return "xiaomi";
        } else if (manufacturer.contains("oppo")) {
            return "oppo";
        } else if (manufacturer.contains("vivo")) {
            return "vivo";
        }
        return "other";
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}