package com.rr.bubtbustracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rr.bubtbustracker.api.API;

public class App extends Application {

    static {
        System.loadLibrary("bus-tracker");
    }
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";

    private static SharedPreferences prefs;
    @Override
    public void onCreate() {
        super.onCreate();

        API api = API.initialize(this);

        prefs = getSharedPreferences("BusTracker", MODE_PRIVATE);

        api.firebaseInitialized(this);
        api.subscribeNotification(this, null);
    }

    public static void saveString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public static void saveInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public static void saveLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    public static void saveBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        return prefs.getString(key, defValue);
    }

    public static int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    public static long getLong(String key, long defValue) {
        return prefs.getLong(key, defValue);
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    public static void clearAll() {
        prefs.edit().clear().apply();
    }

    public static boolean isLogin() {
        return !getString("id", "").isEmpty() &&
                !getString("role", "").isEmpty() &&
                !getString("name", "").isEmpty() &&
                !getString("bus", "").isEmpty() &&
                !getString("email", "").isEmpty() &&
                !getString("refreshToken", "").isEmpty() &&
                !getString("requestToken", "").isEmpty() &&
                !getString("accessToken", "").isEmpty();
    }

    public static boolean isDriver() {
        return getString("role", "").equals("DRIVER");
    }

    public static boolean isStartTrip() {
        return getBoolean("start_trip", false);
    }


    public static native String encryption(String data);

    public static native String decryption(String data);

    public static native String getToken();
}
