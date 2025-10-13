package com.rr.bubtbustracker.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.interfaces.ApiCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class API {

    public interface Callback {
        void onStatus(boolean status);
    }

    private static API mAPI;
    private final OkHttpClient client;
    private final Handler handler;
    private JSONObject serverData = null;

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static synchronized API getAPI() {
        if (mAPI == null) {
            mAPI = new API();
        }
        return mAPI;
    }

    public static synchronized API initialize() {
        mAPI = new API();
        return mAPI;
    }

    public API() {
        handler = new Handler(Looper.getMainLooper());
        client = new OkHttpClient.Builder().build();
    }

    public void serverData(ApiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                readServerData();
                handler.post(() -> {
                    if (callback != null) {
                        callback.onResult(serverData);
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    public void login(String email, String password, ApiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                readServerData();

                if (serverData != null) {
                    String token = App.getToken();
                    if (!token.isEmpty()) {
                        JSONObject json = new JSONObject();
                        json.put("email", email);
                        json.put("password", App.encryption(password));
                        json.put("token", token);

                        Request request = new Request.Builder()
                                .url(serverData.optString("host")+"login")
                                .post(RequestBody.create(json.toString(), JSON))
                                .build();

                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            result = new JSONObject(response.body().string());
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

    public void signUp(String name, String email, String password, String bus, ApiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                readServerData();
                if (serverData != null) {
                    String token = App.getToken();
                    if (!token.isEmpty()) {
                        JSONObject json = new JSONObject();
                        json.put("name", name);
                        json.put("email", email);
                        json.put("password", App.encryption(password));
                        json.put("bus", bus);
                        json.put("token", token);

                        Request request = new Request.Builder()
                                .url(serverData.optString("host")+"sign_up")
                                .post(RequestBody.create(json.toString(), JSON))
                                .build();

                        Response response = client.newCall(request).execute();

                        if (response.isSuccessful()) {
                            result = new JSONObject(response.body().string());
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

    public void reset(String email, ApiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                readServerData();
                if (serverData != null) {
                    String token = App.getToken();
                    if (!token.isEmpty()) {
                        JSONObject json = new JSONObject();
                        json.put("email", email);
                        json.put("token", token);

                        Request request = new Request.Builder()
                                .url(serverData.optString("host")+"reset")
                                .post(RequestBody.create(json.toString(), JSON))
                                .build();

                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            result = new JSONObject(response.body().string());
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

    public void busChange(String id, String bus, ApiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                readServerData();
                if (serverData != null) {
                    String token = App.getToken();
                    if (!token.isEmpty()) {
                        JSONObject json = new JSONObject();
                        json.put("id", id);
                        json.put("bus", bus);
                        json.put("token", token);

                        Request request = new Request.Builder()
                                .url(serverData.optString("host")+"bus_change")
                                .post(RequestBody.create(json.toString(), JSON))
                                .build();

                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            result = new JSONObject(response.body().string());
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

    public void verification(String refreshToken, String accessToken, ApiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            JSONObject result = null;
            try {
                readServerData();
                if (serverData != null) {
                    String token = App.getToken();
                    if (!token.isEmpty()) {
                        JSONObject json = new JSONObject();
                        json.put("accessToken", accessToken);
                        json.put("token", token);

                        Request request = new Request.Builder()
                                .url(serverData.optString("host")+"verification")
                                .addHeader("Authorization", "Bearer "+refreshToken)
                                .post(RequestBody.create(json.toString(), JSON))
                                .build();

                        Response response = client.newCall(request).execute();

                        if (response.isSuccessful()) {
                            result = new JSONObject(response.body().string());
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

    public void checkStatus(String refreshToken, String token, String requestToken, long refreshTime, ApiCallback callback) {
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

                if (isConfig) {
                    String bus = App.getString("bus", "Padma").toUpperCase();
                    FirebaseMessaging.getInstance().subscribeToTopic(bus).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            App.saveString("subscribe", bus);
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

    private void readServerData() throws IOException, JSONException {
        if (serverData == null) {
            Request request = new Request.Builder().url(App.getPublicUrl()).build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                serverData = new JSONObject(response.body().string());
            }
        }
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
}
