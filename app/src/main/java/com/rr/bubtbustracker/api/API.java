package com.rr.bubtbustracker.api;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.activity.DashboardActivity;
import com.rr.bubtbustracker.db.TripDatabaseHelper;
import com.rr.bubtbustracker.interfaces.ApiCallback;
import com.rr.bubtbustracker.interfaces.LocationCallback;
import com.rr.bubtbustracker.interfaces.NotificationCallback;
import com.rr.bubtbustracker.interfaces.ScheduleCallback;
import com.rr.bubtbustracker.interfaces.TripStatusListener;
import com.rr.bubtbustracker.services.DriverNotificationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import dev.gustavoavila.websocketclient.WebSocketClient;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class API {

    private static final String BASE_URL = "https://bus-tracker.kbrms.xyz/";
//    private static final String BASE_URL = "http://192.168.0.246:9099/";

    public interface Callback {
        void onStatus(boolean status);
    }

    private static API mAPI;
    private Context mContext;
    private final OkHttpClient client;
    private final Handler handler;
    private Handler mPingHandler;
    private Runnable mPingRunnable;
    private WebSocketClient mWebSocketClient;
    private LocationManager locationManager;
    private LocationCallback mLocationCallback;
    private TripStatusListener mTripStatusListener;
    private ScheduleCallback mScheduleListener;
    private NotificationCallback mNotificationListener;
    private TripDatabaseHelper dbHelper;
    private boolean isSubscribe = false;
    private boolean isWebSocketConnected = false;

    private Timer timer;

    private double lastLat = Double.NaN;
    private double lastLng = Double.NaN;
    private ArrayList<LatLng> routePoints;
    private int load = 0;

    private boolean isDemo = false;

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static synchronized API getAPI(Context context) {
        if (mAPI == null) {
            mAPI = new API(context);
        }
        return mAPI;
    }

    public static synchronized API initialize(Context context) {
        mAPI = new API(context);
        return mAPI;
    }

    public API(Context context) {
        mContext = context;
        handler = new Handler(Looper.getMainLooper());
        client = new OkHttpClient.Builder().build();

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        routePoints = getDemoLocation();
    }

    public void setLocationCallback(LocationCallback callback) {
        mLocationCallback = callback;
    }

    public void setTripStatusCallback(TripStatusListener callback) {
        mTripStatusListener = callback;
    }

    public void setScheduleCallback(ScheduleCallback callback) {
        mScheduleListener = callback;
    }

    public void setNotificationCallback(NotificationCallback callback) {
        mNotificationListener = callback;
    }

    public void setDatabaseHelper(TripDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void startLocationShare(String bus, LatLng latLng) {
        if (timer != null) {
            return;
        }

        ContextCompat.startForegroundService(mContext, new Intent(mContext, DriverNotificationService.class));

        if (latLng != null) {
            if (mLocationCallback != null) mLocationCallback.onLocationReceived(latLng.latitude, latLng.longitude);
            if (dbHelper != null) handler.post(() -> dbHelper.insertLocation(latLng.latitude, latLng.longitude, System.currentTimeMillis()));
        }

        startLocationUpdates((lat, lng) -> {
            if (mLocationCallback != null) mLocationCallback.onLocationReceived(lat, lng);
            try {
                if (dbHelper != null) handler.post(() -> dbHelper.insertLocation(lat, lng, System.currentTimeMillis()));

                if (mWebSocketClient != null && isWebSocketConnected) {
                    JSONObject json = new JSONObject();
                    json.put("t", 3);
                    json.put("s", bus);
                    json.put("lat", lat);
                    json.put("lng", lng);
                    mWebSocketClient.send(json.toString());
                }
            } catch (Exception e) {}
        });
    }

    public void stopLocationShare() {
        App.saveInt("demo_load", 0);
        mContext.stopService(new Intent(mContext, DriverNotificationService.class));
        stopLocationUpdates();
        if (dbHelper != null) dbHelper.clearTrip();
    }

    public void connectWebsocket() {
        try {
            if (mWebSocketClient != null) {
                if (isWebSocketConnected) {
                    return;
                }

                try {
                    mWebSocketClient.disableAutomaticReconnection();
                    mWebSocketClient.close(0, 1000, "manual close");
                } catch (Exception ignored) {}

                mWebSocketClient = null;

            }
        } catch (Exception ignored) {}

        URI uri;
        try {
            uri = new URI("ws"+BASE_URL.substring(4));
        } catch (Exception e) {
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                isWebSocketConnected = true;
                startPing(mWebSocketClient);

                subscribeWS(App.getString("bus", ""));

                try {
                    if (mWebSocketClient != null && isWebSocketConnected) {
                        JSONObject json = new JSONObject();
                        json.put("t", 5);
                        json.put("version", App.getInt("schedule_v", 0));
                        Log.d("BusTrackerLog", "onOpen: "+json);
                        mWebSocketClient.send(json.toString());
                    }
                } catch (Exception ignored) {}

                try {
                    if (mWebSocketClient != null && isWebSocketConnected) {
                        JSONObject json = new JSONObject();
                        json.put("t", 6);
                        json.put("s", App.getString("bus", ""));
                        json.put("time", App.getLong("last_notify", 0));
                        mWebSocketClient.send(json.toString());
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onTextReceived(String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    Log.d("BusTrackerLog", "onTextReceived: "+json);
                    int type = json.optInt("t", 0);
                    if (type == 1) {
                        boolean status = json.optBoolean("s", false);
                        if (mTripStatusListener != null)  mTripStatusListener.tripStatus(json.optLong("id"), status, json.optString("from", ""), json.optString("to", ""));
                        if (status && mLocationCallback != null) mLocationCallback.onLocationReceived(json.optDouble("lat", 0), json.optDouble("lng", 0));
                    } else if (type == 2) {
                        if (mLocationCallback != null) mLocationCallback.onLocationReceived(json.optDouble("lat", 0), json.optDouble("lng", 0));
                    } else if (type == 3) {
                        handler.post(() -> {
                            ArrayList<LatLng> list = new ArrayList<>();
                            if (dbHelper != null) {
                                list = dbHelper.getAllLocationsAsLatLng();
                            }
                            JSONArray jsonArray = json.optJSONArray("data");
                            if (jsonArray != null) {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    try {
                                        JSONObject data = jsonArray.getJSONObject(i);
                                        double lat = data.optDouble("lat");
                                        double lng = data.optDouble("lng");
                                        long time = data.optLong("time");
                                        list.add(new LatLng(lat, lng));
                                        dbHelper.insertLocation(lat, lng, time);
                                    } catch (JSONException ignored) {}
                                }
                            }

                            if (mTripStatusListener != null && !list.isEmpty()) mTripStatusListener.readLocationRoute(list);
                        });
                    } else if (type == 4) {
                        handler.post(() -> {
                            try {
                                JSONArray jsonObject = json.optJSONArray("data");
                                if (jsonObject != null) {
                                    int version = json.optInt("version", 0);
                                    App.saveInt("schedule_v", version);
                                    App.saveString("schedule", jsonObject.toString());
                                    if (mScheduleListener != null) mScheduleListener.scheduleUpdate();
                                }
                            } catch (Exception ignored) {}
                        });
                    } else if (type == 5) {
                        handler.post(() -> {
                            try {
                                JSONArray jsonObject = json.optJSONArray("data");
                                if (jsonObject != null) {
                                    if (jsonObject.length() > 0) {
                                        JSONObject object = jsonObject.getJSONObject(0);
                                        App.saveLong("last_notify", object.optLong("time", 0));
                                    }
                                    JSONArray prevNotify = new JSONArray(App.getString("notification", "[]"));
                                    for (int i = 0; i < prevNotify.length(); i++) {
                                        jsonObject.put(prevNotify.get(i));
                                    }
                                    App.saveString("notification", jsonObject.toString());
                                    if (mNotificationListener != null) mNotificationListener.notificationUpdate();
                                }
                            } catch (Exception ignored) {}
                        });
                    }
                } catch (JSONException ignored) {}
            }

            @Override
            public void onBinaryReceived(byte[] data) {

            }

            @Override
            public void onPingReceived(byte[] data) {}

            @Override
            public void onPongReceived(byte[] data) {}

            @Override
            public void onException(Exception e) {
                isWebSocketConnected = false;
                isSubscribe = false;
                stopPing();
            }

            @Override
            public void onCloseReceived(int reason, String description) {
                isWebSocketConnected = false;
                isSubscribe = false;
                stopPing();
            }
        };

        mWebSocketClient.setConnectTimeout(30000);
        mWebSocketClient.setReadTimeout(600000);
        mWebSocketClient.enableAutomaticReconnection(10000);
        mWebSocketClient.connect();
    }

    public void closeConnection() {
        try {
            if (mWebSocketClient != null) {
                try {
                    mWebSocketClient.disableAutomaticReconnection();
                    mWebSocketClient.close(0, 1000, "manual close");
                } catch (Exception ignored) {}

                mWebSocketClient = null;

            }
        } catch (Exception ignored) {}
    }

    public boolean isConnected() {
        return mWebSocketClient != null && isWebSocketConnected;
    }

    public void readLocationRoute(long time, String bus) {
        try {
            if (mWebSocketClient != null && isWebSocketConnected) {
                JSONObject json = new JSONObject();
                json.put("t", 4);
                json.put("s", bus);
                json.put("time", time);
                mWebSocketClient.send(json.toString());
            } else {
                if (mTripStatusListener != null) mTripStatusListener.readLocationRoute(null);
            }
        } catch (Exception e) {
            if (mTripStatusListener != null) mTripStatusListener.readLocationRoute(null);
        }
    }

    public void subscribeWS(String bus) {
        try {
            if (!bus.isEmpty() && !App.isDriver() && !isSubscribe) {
                if (mWebSocketClient != null && isWebSocketConnected) {
                    JSONObject json = new JSONObject();
                    json.put("t", 1);
                    json.put("s", bus);
                    mWebSocketClient.send(json.toString());
                    isSubscribe = true;
                }
            }
        } catch (Exception e) {}
    }

    private void startPing(WebSocketClient webSocketClient) {
        stopPing();

        mPingHandler = new Handler(Looper.getMainLooper());
        mPingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (webSocketClient != null && isWebSocketConnected) {
                        webSocketClient.sendPing(new byte[0]);
                        mPingHandler.postDelayed(this, 60000);
                    }
                } catch (Exception ignored) {}
            }
        };

        mPingHandler.postDelayed(mPingRunnable, 60000);
    }

    private void stopPing() {
        if (mPingHandler != null && mPingRunnable != null) {
            mPingHandler.removeCallbacks(mPingRunnable);
            mPingHandler = null;
            mPingRunnable = null;
        }
    }

    @SuppressLint({"ServiceCast", "MissingPermission"})
    public void getCurrentLocation(LocationCallback callback) {
        try {
            if (isDemo) {
                callback.onLocationReceived(23.81185,90.35711);
                return;
            }
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (loc != null) {
                callback.onLocationReceived(loc.getLatitude(), loc.getLongitude());
                return;
            }

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                callback.onLocationReceived(0, 0);
                return;
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            callback.onLocationReceived(location.getLatitude(),location.getLongitude());
                            locationManager.removeUpdates(this);
                        }
                    }
            );

        } catch (Exception e) {}
    }

    @SuppressLint({"MissingPermission", "DiscouragedApi"})
    public void startLocationUpdates(LocationCallback callback) {
        load = App.getInt("demo_load", 0);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isDemo) {
                    if (load < routePoints.size()) {
                        LatLng latLng = routePoints.get(load++);
                        App.saveInt("demo_load", load);
                        if (callback != null) callback.onLocationReceived(latLng.latitude, latLng.longitude);
                    }
                } else {
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        double lat = loc.getLatitude();
                        double lng = loc.getLongitude();

                        if (lat != lastLat || lng != lastLng) {
                            lastLat = lat;
                            lastLng = lng;
                            if (callback == null) {
                                stopLocationUpdates();
                            } else {
                                callback.onLocationReceived(lat, lng);
                            }
                        }
                    } else {
                        stopLocationUpdates();
                    }
                }
            }
        }, 0, 5000);
    }

    public void stopLocationUpdates() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    public void scheduleData(ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request request = new Request.Builder().url(BASE_URL+"schedule").build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    if (jsonObject.getInt("status") == 200) {
                        handler.post(() -> {
                            if (callback != null) {
                                callback.onResult(jsonObject);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void login(String email, String password, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("email", email);
                    json.put("password", App.encryption(password));
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"login")
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void signUp(String name, String email, String password, String bus, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("name", name);
                    json.put("email", email);
                    json.put("password", App.encryption(password));
                    json.put("bus", bus);
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"sign_up")
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }

                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void reset(String email, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("email", email);
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"reset")
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void busChange(String id, String bus, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("id", id);
                    json.put("bus", bus);
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"bus_change")
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void verification(String refreshToken, String accessToken, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("accessToken", accessToken);
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"verification")
                            .addHeader("Authorization", "Bearer "+refreshToken)
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void checkStatus(String refreshToken, String token, String requestToken, long refreshTime, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;

            try {
                String[] tokenParts = App.decryption(requestToken).split("\\|");
                if (tokenParts.length >= 4) {
                    String accessToken = token;
                    String latestToken = null;
                    if (System.currentTimeMillis() > refreshTime) {
                        accessToken = getAccessToken(refreshToken, tokenParts[0], client);
                        latestToken = accessToken;
                    }

                    for (int i = 0; i < 2; i++) {
                        if (accessToken == null) {
                            break;
                        }

                        try {
                            JSONObject body = new JSONObject();
                            body.put("idToken", accessToken);

                            Request request = new Request.Builder()
                                    .url("https://www.googleapis.com/identitytoolkit/v3/relyingparty/getAccountInfo?key=" + tokenParts[0])
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("X-Android-Package", "com.rr.bubtbustracker")
                                    .addHeader("X-Android-Cert", tokenParts[1])
                                    .addHeader("Accept-Language", "en-GB, en-US")
                                    .addHeader("X-Client-Version", "Android/Fallback/X24000001/FirebaseCore-Android")
                                    .addHeader("X-Firebase-Gmpid", tokenParts[2])
                                    .addHeader("X-Firebase-Client", tokenParts[3])
                                    .addHeader("User-Agent", "Dalvik/2.1.0")
                                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8")))
                                    .build();

                            Response response = client.newCall(request).execute();
                            if (response.isSuccessful()) {
                                String respString = response.body().string();
                                JSONObject json = new JSONObject(respString);

                                if (json.has("users")) {
                                    JSONObject user = json.getJSONArray("users").getJSONObject(0);

                                    result = new JSONObject();
                                    result.put("status", "SUCCESS");
                                    result.put("id", user.getString("localId"));
                                    result.put("verified", user.optBoolean("emailVerified", false));
                                    result.put("passwordUpdatedAt", user.optLong("passwordUpdatedAt"));
                                    result.put("lastLoginAt", user.optLong("lastLoginAt"));
                                    result.put("createdAt", user.optLong("createdAt"));
                                    result.put("latestToken", latestToken);
                                    break;
                                }
                            } else {
                                String errorBody = response.body().string();
                                try {
                                    JSONObject errorJson = new JSONObject(errorBody);
                                    JSONObject err = errorJson.optJSONObject("error");
                                    if (err != null) {
                                        String msg = err.optString("message");
                                        if (msg.equals("INVALID_ID_TOKEN") || msg.equals("TOKEN_EXPIRED")) {
                                            accessToken = getAccessToken(refreshToken, tokenParts[0], client);
                                            latestToken = accessToken;
                                        }
                                    }
                                } catch (JSONException e) {}
                            }
                        } catch (Exception e) {
                            if (e instanceof IOException || (e.getMessage() != null && (e.getMessage().contains("INVALID_ID_TOKEN") || e.getMessage().contains("TOKEN_EXPIRED")))) {
                                accessToken = getAccessToken(refreshToken, tokenParts[0], client);
                                latestToken = accessToken;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;
            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void startTrip(String bus, double lat, double lng, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("lat", lat);
                    json.put("lng", lng);
                    json.put("bus", bus);
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"start_trip")
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    public void stopTrip(String bus, ApiCallback<JSONObject> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                String token = App.getToken();
                if (!token.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("bus", bus);
                    json.put("token", token);

                    Request request = new Request.Builder()
                            .url(BASE_URL+"stop_trip")
                            .post(RequestBody.create(json.toString(), JSON))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        result = new JSONObject(response.body().string());
                    }
                }
            } catch (Exception ignored) {}

            JSONObject finalResult = result;

            handler.post(() -> {
                if (callback != null) {
                    callback.onResult(finalResult);
                }
            });
        });
    }

    private String getAccessToken(String refreshToken, String API_KEY, OkHttpClient client) {
        try {
            JSONObject body = new JSONObject();
            body.put("grantType", "refresh_token");
            body.put("refreshToken", refreshToken);

            Request request = new Request.Builder()
                    .url("https://securetoken.googleapis.com/v1/token?key=" + API_KEY)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8")))
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json.optString("access_token", null);
            }
        } catch (Exception ignored) {}

        return null;
    }

    public void subscribeNotification(Context context, Callback callback) {
        try {
            if (App.getString("subscribe", "").isEmpty()) {
                boolean isConfig = firebaseInitialized(context);

                if (isConfig) {
                    String bus = App.getString("bus", "").toUpperCase();
                    if (App.isDriver()) {
                        bus = bus+"_DRIVER";
                    }
                    String finalBus = bus;
                    FirebaseMessaging.getInstance().subscribeToTopic(bus).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            App.saveString("subscribe", finalBus);
                            if (callback != null) callback.onStatus(true);
                        } else {
                            if (callback != null) callback.onStatus(false);
                        }
                    }).addOnFailureListener(e -> {
                        if (callback != null) callback.onStatus(false);
                    });
                } else {
                    if (callback != null) callback.onStatus(false);
                }
            } else {
                if (callback != null) callback.onStatus(false);
            }
        } catch (Exception e) {
            if (callback != null) callback.onStatus(false);
        }
    }

    public void unSubscribeNotification(Context context, Callback callback) {
        try {
            String bus = App.getString("subscribe", "");
            if (!bus.isEmpty()) {
                if (App.isDriver()) {
                    bus = bus+"_DRIVER";
                }
                boolean isConfig = firebaseInitialized(context);

                if (isConfig) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(bus).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            App.saveString("subscribe", "");
                            if (callback != null) callback.onStatus(true);
                        } else {
                            if (callback != null) callback.onStatus(false);
                        }
                    }).addOnFailureListener(e -> {
                        if (callback != null) callback.onStatus(false);
                    });
                } else {
                    if (callback != null) callback.onStatus(false);
                }
            } else {
                if (callback != null) callback.onStatus(false);
            }
        } catch (Exception e) {
            if (callback != null) callback.onStatus(false);
        }
    }

    public boolean firebaseInitialized(Context context) {
        boolean isConfig = true;
        if (FirebaseApp.getApps(context).isEmpty()) {
            isConfig = false;
            String requestToken = App.getString("requestToken", "");
            if (!requestToken.isEmpty()) {
                String[] tokenParts = App.decryption(requestToken).split("\\|");
                if (tokenParts.length >= 5) {
                    FirebaseOptions options = new FirebaseOptions.Builder()
                            .setApiKey(tokenParts[0])
                            .setApplicationId(tokenParts[2])
                            .setProjectId(tokenParts[4])
                            .build();

                    FirebaseApp.initializeApp(context, options);
                    isConfig = true;
                }
            }
        }
        return isConfig;
    }

    public String getMessage(String status) {
        switch (status) {
            case "FIELD_EMPTY":
                return "All fields are required!";
            case "EMAIL_EXISTS":
                return "This email is already registered!";
            case "INVALID_EMAIL":
            case "WRONG_EMAIL":
                return "Please enter a valid email address!";
            case "PASSWORD_LENGTH_SHORT":
                return "Password must be at least 6 characters!";
            case "SIGN_UP_FAILED":
                return "Sign up failed! Please try again.";
            case "LOGIN_FAILED":
                return "Login failed! Check your credentials.";
            case "NO_HEADER_TOKEN":
            case "NO_ACCESS_TOKEN":
                return "Authentication error! Please log in again.";
            case "ERROR":
            default:
                return "Something went wrong! Please try again later.";
        }
    }

    private ArrayList<LatLng> getDemoLocation() {
        ArrayList<LatLng> points = new ArrayList<>();
        points.add(new LatLng(23.81159,90.35720));
        points.add(new LatLng(23.81152,90.35717));
        points.add(new LatLng(23.81143,90.35685));
        points.add(new LatLng(23.81253,90.35655));
        points.add(new LatLng(23.81350,90.35632));
        points.add(new LatLng(23.81448,90.35610));
        points.add(new LatLng(23.81551,90.35600));
        points.add(new LatLng(23.81654,90.35600));
        points.add(new LatLng(23.81758,90.35599));
        points.add(new LatLng(23.81846,90.35617));
        points.add(new LatLng(23.81935,90.35634));
        points.add(new LatLng(23.82024,90.35664));
        points.add(new LatLng(23.82113,90.35694));
        points.add(new LatLng(23.82218,90.35700));
        points.add(new LatLng(23.82322,90.35700));
        points.add(new LatLng(23.82426,90.35700));
        points.add(new LatLng(23.82431,90.35881));
        points.add(new LatLng(23.82435,90.36062));
        points.add(new LatLng(23.82440,90.36244));
        points.add(new LatLng(23.82445,90.36425));
        points.add(new LatLng(23.82600,90.36422));
        points.add(new LatLng(23.82756,90.36419));
        points.add(new LatLng(23.82930,90.36400));
        points.add(new LatLng(23.82933,90.36576));
        points.add(new LatLng(23.82936,90.36752));
        points.add(new LatLng(23.82940,90.36928));
        points.add(new LatLng(23.82943,90.37104));
        points.add(new LatLng(23.82946,90.37280));
        points.add(new LatLng(23.82971,90.37411));
        points.add(new LatLng(23.82996,90.37542));
        points.add(new LatLng(23.83021,90.37673));
        points.add(new LatLng(23.82876,90.37694));
        points.add(new LatLng(23.82731,90.37714));
        points.add(new LatLng(23.82586,90.37735));
        points.add(new LatLng(23.82441,90.37755));
        points.add(new LatLng(23.82296,90.37776));
        points.add(new LatLng(23.82320,90.37891));
        points.add(new LatLng(23.82344,90.38006));
        points.add(new LatLng(23.82290,90.38111));
        points.add(new LatLng(23.82235,90.38217));
        points.add(new LatLng(23.82153,90.38334));
        points.add(new LatLng(23.82070,90.38451));
        points.add(new LatLng(23.82032,90.38626));
        points.add(new LatLng(23.81995,90.38801));
        points.add(new LatLng(23.82006,90.38904));
        points.add(new LatLng(23.82017,90.39008));
        points.add(new LatLng(23.82099,90.39100));
        points.add(new LatLng(23.82182,90.39192));
        points.add(new LatLng(23.82260,90.39343));
        return points;
    }
}
