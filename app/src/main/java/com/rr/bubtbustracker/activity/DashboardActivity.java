package com.rr.bubtbustracker.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.api.API;
import com.rr.bubtbustracker.db.TripDatabaseHelper;
import com.rr.bubtbustracker.fragment.LocationFragment;
import com.rr.bubtbustracker.fragment.NotificationFragment;
import com.rr.bubtbustracker.fragment.ScheduleFragment;
import com.rr.bubtbustracker.interfaces.FragmentReadyListener;
import com.rr.bubtbustracker.interfaces.LocationCallback;
import com.rr.bubtbustracker.interfaces.NotificationCallback;
import com.rr.bubtbustracker.interfaces.OnBusClickListener;
import com.rr.bubtbustracker.interfaces.ScheduleCallback;
import com.rr.bubtbustracker.interfaces.TripStatusListener;
import com.rr.bubtbustracker.view.BusListBottomView;
import com.rr.bubtbustracker.view.ScheduleBottomView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class DashboardActivity extends AppCompatActivity implements OnBusClickListener, LocationCallback, FragmentReadyListener, TripStatusListener, ScheduleCallback, NotificationCallback {

    private String mToday = "Friday";
    private String mBusName = "";
    private int tabClick = 1;
    private int blue;
    private int gray;
    private boolean isFridayClicked = false;
    private boolean isOtherDayClicked = false;
    private boolean isDriver = false;
    private boolean isStartTrip = false;
    private boolean isStartTripView = false;
    private LinearLayout titleClick;
    private TextView titleName;
    private TextView tripTitle;
    private ImageView titleIcon;
    private ImageView actionSchedule;
    private ImageView actionNotification;
    private TextView scheduleText;
    private TextView notificationText;
    private LinearLayout notificationLayout;
    private LinearLayout scheduleLayout;
    private FloatingActionButton locationFab;

    private FragmentManager fragmentManager;
    private ScheduleFragment scheduleFragment;
    private LocationFragment locationFragment;
    private NotificationFragment notificationFragment;
    private TripDatabaseHelper dbHelper;
    private LatLng pendingLatLng;
    private String pendingId = "";
    private String pendingFrom = "";
    private String pendingTo = "";
    private boolean isPending = false;

    private API mAPI;

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (App.isLogin() && App.getBoolean("verified", false)) {
            Intent intent = getIntent();
            if (intent != null && Objects.equals(intent.getAction(), App.LOGIN_SUCCESS)) {
                initViews(false);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(() -> initViews(true), 1500);
            }
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    @SuppressLint("InflateParams")
    private void initViews(boolean wait) {
        setTheme(R.style.NoStatusBar);
        EdgeToEdge.enable(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View dashboard = inflater.inflate(R.layout.activity_dashboard, null);

        if (wait) {
            dashboard.setAlpha(0f);
        }
        setContentView(dashboard);

        if (wait) {
            dashboard.animate()
                    .alpha(1f)
                    .setDuration(420)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        setupWindow(dashboard);
    }

    @SuppressLint("SetTextI18n")
    private void setupWindow(View main) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.parseColor("#FFFFFFFF"));

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }

        ViewCompat.setOnApplyWindowInsetsListener(main.findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DrawerLayout drawerLayout = main.findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = main.findViewById(R.id.toolbar);
        NavigationView navigationView = main.findViewById(R.id.navigation_view);
        View headerView = navigationView.getHeaderView(0);
        TextView navTitle = headerView.findViewById(R.id.nav_title);
        TextView navEmail = headerView.findViewById(R.id.nav_email);

        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }

        toolbar.setTitle("Location");

        setSupportActionBar(toolbar);

        navTitle.setText(App.getString("name", ""));
        navEmail.setText(App.getString("email", ""));

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,  drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);

        titleClick = main.findViewById(R.id.titleClick);
        titleName = main.findViewById(R.id.titleName);
        tripTitle = main.findViewById(R.id.tripTitle);
        titleIcon = main.findViewById(R.id.titleIcon);

        scheduleLayout = main.findViewById(R.id.schedule_layout);
        notificationLayout = main.findViewById(R.id.notification_layout);
        locationFab = main.findViewById(R.id.location);

        mBusName = capitalizeFirst(App.getString("bus", ""));

        scheduleFragment = new ScheduleFragment();
        locationFragment = new LocationFragment();
        notificationFragment = new NotificationFragment();

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.add(R.id.container, locationFragment, "location");
        ft.add(R.id.container, scheduleFragment, "schedule").hide(scheduleFragment);
        ft.add(R.id.container, notificationFragment, "notification").hide(notificationFragment);
        ft.commit();

        mAPI = API.getAPI(this);
        dbHelper = new TripDatabaseHelper(this);
        mAPI.setLocationCallback(this);
        mAPI.setTripStatusCallback(this);
        mAPI.setScheduleCallback(this);
        mAPI.setNotificationCallback(this);
        mAPI.setDatabaseHelper(dbHelper);
        mAPI.connectWebsocket();

        isStartTripView = false;
        isDriver = App.isDriver();
        isStartTrip = App.isStartTrip();

        if (isDriver) {
            titleName.setVisibility(View.GONE);
            tripTitle.setVisibility(View.VISIBLE);
            if (isStartTrip) {
                tripTitle.setText("End Trip");
                titleClick.setBackgroundResource(R.drawable.rounded_red_bg);
            } else {
                tripTitle.setText("Start Trip");
                titleClick.setBackgroundResource(R.drawable.rounded_green_bg);
            }
        } else {
            titleName.setText(mBusName);
            titleClick.setBackgroundResource(R.drawable.rounded_bg_effect);
        }


        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                locationFab.performClick();
            } else if (id == R.id.nav_notification) {
                notificationLayout.performClick();
            } else if (id == R.id.nav_schedule) {
                scheduleLayout.performClick();
            } else if (id == R.id.nav_setting) {
                Toast.makeText(getApplicationContext(),"Feature will be available in upcoming version", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_logout) {
                logoutAccount();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        titleClick.setOnClickListener(v -> {
            if (tabClick == 1) {
                if (isDriver) {
                    if (isStartTrip) {
                        stopBusTrip();
                    } else {
                        checkLocationPermission();
                    }
                } else {
                    new BusListBottomView(this, null, null, newBus -> {
                        String currentBus = App.getString("bus", "");
                        if (!Objects.equals(currentBus.toUpperCase(), newBus.toUpperCase())) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Bus Change Warning")
                                    .setMessage("Changing from " + capitalizeFirst(currentBus) + " to " + newBus + ".\n\n" +
                                            "• Your notification will be updated.\n" +
                                            "• App data may be refreshed.\n" +
                                            "• Make sure you want to continue.")
                                    .setCancelable(false)
                                    .setPositiveButton("Continue", (dialog, which) -> {
                                        busChange(titleName, newBus);
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .show();
                        }
                    });
                }
            } else {
                new ScheduleBottomView(this, titleName, selectDay -> {
                    if (selectDay.equals("Friday")) {
                        if (!isFridayClicked) {
                            isFridayClicked = true;
                            isOtherDayClicked = false;

                            scheduleFragment.setFriday(true);
                        }
                    } else {
                        if (!isOtherDayClicked) {
                            isOtherDayClicked = true;
                            isFridayClicked = false;
                            scheduleFragment.setFriday(false);
                        }
                    }

                });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 3122);
            }
        }

        actionSchedule = main.findViewById(R.id.action_schedule);
        actionNotification = main.findViewById(R.id.action_notification);
        scheduleText = main.findViewById(R.id.schedule_text);
        notificationText = main.findViewById(R.id.notification_text);

        blue = ContextCompat.getColor(this, R.color.blue);
        gray = ContextCompat.getColor(this, R.color.gray);

        tabClick = 1;
        mToday = getToDay();
        isFridayClicked = mToday.equals("Friday");
        isOtherDayClicked = !isFridayClicked;

        scheduleLayout.setOnClickListener(v -> {
            if (tabClick != 0) {
                tabClick = 0;
                actionSchedule.setColorFilter(blue);
                scheduleText.setTextColor(blue);

                actionNotification.setColorFilter(gray);
                notificationText.setTextColor(gray);

                changeTitle(titleClick, titleName, tripTitle, titleIcon, "Schedule", mToday, R.drawable.ic_schedule, true);

                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short)
                        .hide(locationFragment)
                        .hide(notificationFragment)
                        .show(scheduleFragment)
                        .commit();
            }
        });

        notificationLayout.setOnClickListener(v -> {
            if (tabClick != 2) {
                tabClick = 2;
                actionNotification.setColorFilter(blue);
                notificationText.setTextColor(blue);

                actionSchedule.setColorFilter(gray);
                scheduleText.setTextColor(gray);

                changeTitle(titleClick, titleName, tripTitle, titleIcon, "Notification","", 0, false);

                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short)
                        .hide(scheduleFragment)
                        .hide(locationFragment)
                        .show(notificationFragment)
                        .commit();
            }
        });

        locationFab.setOnClickListener(v -> {
            if (tabClick != 1) {
                locationTabClick();
            } else {
                locationFragment.viewCurrentPosition(true);
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 9099);
        } else {
            checkGpsEnabled();
        }
    }

    private void checkGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                LocationRequest locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                LocationSettingsRequest.Builder builder =  new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true);

                SettingsClient settingsClient = LocationServices.getSettingsClient(getApplicationContext());
                Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

                task.addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(DashboardActivity.this, 999);
                        } catch (IntentSender.SendIntentException ex) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    }
                });
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            showStartTripDialog();
        }
    }

    private void showStartTripDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Start Bus Trip")
                .setMessage("Are you sure you want to start this bus trip?")
                .setPositiveButton("Start", (dialog, which) -> {
                    if (mAPI.isConnected()) {
                        getCurrentLocation();
                    } else {
                        Toast.makeText(this, "Websocket are not connected", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void getCurrentLocation() {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Read Current Location...");
        loading.setCancelable(false);
        loading.show();

        mAPI.getCurrentLocation((lat, lng) -> {
            if (lat > 0 && lng > 0) {
                loading.setMessage("Starting bus trip...");
                String bus = App.getString("bus", "");
                mAPI.startTrip(bus, lat, lng, json -> {
                    if (loading.isShowing()) loading.dismiss();
                    String message = "Request Error! Please try again.";
                    try {
                        if (json != null) {
                            String status = json.optString("status");
                            if (!status.isEmpty()) {
                                if (status.equals("SUCCESS")) {
                                    isStartTrip = true;
                                    App.saveBoolean("start_trip", true);
                                    message = "Trip Start Success!";
                                    tripTitle.setText("End Trip");
                                    dbHelper.clearTrip();
                                    App.saveInt("demo_load", 0);
                                    locationFragment.startBusTrip(dbHelper, false);
                                    titleClick.setBackgroundResource(R.drawable.rounded_red_bg);
                                    mAPI.startLocationShare(bus, new LatLng(lat, lng));
                                } else {
                                    message = mAPI.getMessage(status);
                                }
                            }
                        }
                    } catch (Exception e) {
                        message = "Exception Error! Please try again.";
                    }
                    if (!message.isEmpty()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
            } else {
                if (loading.isShowing()) loading.dismiss();
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void stopBusTrip() {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Stop bus trip...");
        loading.setCancelable(false);
        loading.show();

        String bus = App.getString("bus", "");
        mAPI.stopTrip(bus,  json -> {
            if (loading.isShowing()) loading.dismiss();
            String message = "Request Error! Please try again.";
            try {
                if (json != null) {
                    String status = json.optString("status");
                    if (!status.isEmpty()) {
                        if (status.equals("SUCCESS")) {
                            isStartTrip = false;
                            message = "Trip Stop Success!";
                            tripTitle.setText("Start Trip");
                            App.saveBoolean("start_trip", false);
                            titleClick.setBackgroundResource(R.drawable.rounded_green_bg);
                            locationFragment.stopBusTrip();
                            mAPI.stopLocationShare();
                        } else {
                            message = mAPI.getMessage(status);
                        }
                    }
                }
            } catch (Exception e) {
                message = "Exception Error! Please try again.";
            }
            if (!message.isEmpty()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void locationTabClick() {
        tabClick = 1;
        actionNotification.setColorFilter(gray);
        notificationText.setTextColor(gray);

        actionSchedule.setColorFilter(gray);
        scheduleText.setTextColor(gray);

        changeTitle(titleClick, titleName, tripTitle, titleIcon, "Location", mBusName, R.drawable.ic_bus, true);

        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short)
                .hide(scheduleFragment)
                .hide(notificationFragment)
                .show(locationFragment)
                .commit();
    }

    private void busChange(TextView busName, String newBus) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Changing bus...");
        loading.setCancelable(false);
        loading.show();

        mAPI.busChange(App.getString("id", ""), newBus.toUpperCase(), json -> {
            if (loading.isShowing()) loading.dismiss();
            String message = "Request Error! Please try again.";
            try {
                if (json != null) {
                    String status = json.optString("status");
                    if (!status.isEmpty()) {
                        if (status.equals("SUCCESS")) {
                            message = "";
                            subscriptionChange(busName, newBus);
                        } else {
                            message = mAPI.getMessage(status);
                        }
                    }
                }
            } catch (Exception e) {
                message = "Exception Error! Please try again.";
            }
            if (!message.isEmpty()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void subscriptionChange(TextView busName, String newBus) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Unsubscribing from old bus...");
        loading.setCancelable(false);
        loading.show();

        mAPI.unSubscribeNotification(getApplicationContext(), unSubscribe -> {
            if (unSubscribe) {
                mBusName = newBus;
                loading.setMessage("Subscribing to new bus...");
                App.saveString("bus", newBus.toUpperCase());
                busName.setText(newBus);
                mAPI.subscribeNotification(getApplicationContext(), subscribe -> {
                    if (loading.isShowing()) loading.dismiss();
                    if (subscribe) {
                        Toast.makeText(this, "Bus changed to " + newBus, Toast.LENGTH_SHORT).show();
                        new AlertDialog.Builder(this)
                                .setTitle("Application Restart")
                                .setMessage("Application need restart for bus change to take effect")
                                .setCancelable(false)
                                .setPositiveButton("Restart", (dialog, which) -> {
                                    Process.killProcess(Process.myPid());
                                    System.exit(1);
                                })
                                .show();
                    } else {
                        Toast.makeText(this, "Bus changed but subscription failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                if (loading.isShowing()) loading.dismiss();
                Toast.makeText(this, "Failed to unsubscribe from previous bus notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changeTitle(View titleClick, TextView titleName, TextView tripTitle, ImageView titleIcon, String title, String newText, int newIcon, boolean visible) {
        if (isDriver && title.equals("Location")) {
            if (isStartTrip) {
                titleClick.setBackgroundResource(R.drawable.rounded_red_bg);
            } else {
                titleClick.setBackgroundResource(R.drawable.rounded_green_bg);
            }
            tripTitle.animate().alpha(0f).setDuration(125).withEndAction(() -> {
                tripTitle.setVisibility(View.VISIBLE);
                titleName.setVisibility(View.GONE);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) actionBar.setTitle(title);
                tripTitle.animate().alpha(1f).setDuration(125).start();
            }).start();
        } else {
            titleClick.setBackgroundResource(R.drawable.rounded_bg_effect);
            titleName.animate().alpha(0f).setDuration(125).withEndAction(() -> {
                tripTitle.setVisibility(View.GONE);
                titleName.setVisibility(View.VISIBLE);
                titleName.setText(newText);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) actionBar.setTitle(title);
                titleName.animate().alpha(1f).setDuration(125).start();
            }).start();
        }

        titleIcon.animate().alpha(0f).setDuration(125).withEndAction(() -> {
            titleIcon.setImageResource(newIcon);
            titleIcon.animate().alpha(1f).setDuration(125).start();
        }).start();

        if (visible) {
            titleClick.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .withStartAction(() -> titleClick.setVisibility(View.VISIBLE))
                    .start();
        } else {
            titleClick.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> titleClick.setVisibility(View.GONE))
                    .start();
        }
    }


    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return "";
        input = input.toLowerCase();
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private String getToDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    @Override
    public void onSelected(JSONObject route, String id, float zoom) {
        locationFragment.updateRoute(route, id, zoom);
        locationTabClick();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Toast.makeText(getApplicationContext(),"Feature will be available in upcoming version", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            logoutAccount();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Logout account")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();

                    ProgressDialog loading = new ProgressDialog(this);
                    loading.setMessage("Logout account...");
                    loading.setCancelable(false);
                    loading.show();

                    mAPI.unSubscribeNotification(getApplicationContext(), unSubscribe -> {
                        if (loading.isShowing()) loading.dismiss();
                        if (unSubscribe) {
                            Toast.makeText(this, "Logout Success", Toast.LENGTH_SHORT).show();
                            App.clearAll();
                            mAPI.closeConnection();

                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Logout Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 9099) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGpsEnabled();
            } else {
                Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 999) {
            if (resultCode == Activity.RESULT_OK) {
                showStartTripDialog();
            } else {
                Toast.makeText(this, "GPS is required to start trip", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onLocationReceived(double lat, double lng) {
        if (lat <= 0 || lng <= 0) return;

        if (locationFragment == null || !locationFragment.isAdded() || locationFragment.getView() == null)  {
            pendingLatLng = new LatLng(lat, lng);
            return;
        }

        pendingLatLng = null;
        runOnUiThread(() -> locationFragment.setBusLocation(new LatLng(lat, lng), null, isDriver));
    }

    @Override
    public void onLocationReady() {
        if (isDriver && isStartTrip && locationFragment != null) {
            locationFragment.startBusTrip(dbHelper, true);
            mAPI.startLocationShare(App.getString("bus", ""), null);
        } else if (isStartTrip) {
            if (pendingLatLng != null) runOnUiThread(() -> locationFragment.setBusLocation(pendingLatLng, null, isDriver));
            if (isPending) {
                if (!App.getString("trip_id", "").equals(pendingId)) {
                    dbHelper.clearTrip();
                    App.saveString("trip_id", pendingId);
                }
                runOnUiThread(() -> locationFragment.viewTripStart(dbHelper, pendingFrom, pendingTo));
            }
        }
    }

    @Override
    public void readLocationRoute(long time) {
        mAPI.readLocationRoute(time, App.getString("bus", ""));
    }

    @Override
    public void tripStatus(long id, boolean start, String from, String to) {
        if (!isDriver) {
            isStartTrip = start;
        }

        if (locationFragment == null || !locationFragment.isAdded() || locationFragment.getView() == null) {
            isPending = true;
            pendingId = App.getString("bus", "")+"_"+id;
            pendingFrom = from;
            pendingTo = to;
            return;
        }

        if (start) {
            if (!isStartTripView) {
                isPending = false;
                isStartTripView = true;
                String bus = App.getString("bus", "");
                if (!App.getString("trip_id", "").equals(bus+"_"+id)) {
                    dbHelper.clearTrip();
                    App.saveString("trip_id", bus+"_"+id);
                }
                runOnUiThread(() -> locationFragment.viewTripStart(dbHelper, from, to));
            }
        } else if (isStartTripView) {
            isStartTripView = false;
            dbHelper.clearTrip();
            runOnUiThread(() -> locationFragment.stopBusTrip());
        }
    }

    @Override
    public void readLocationRoute(ArrayList<LatLng> list) {
        if (locationFragment == null || !locationFragment.isAdded() || locationFragment.getView() == null)  {
            return;
        }

        runOnUiThread(() -> locationFragment.updateBusRoute(list));
    }

    @Override
    public void scheduleUpdate() {
        if (scheduleFragment == null || !scheduleFragment.isAdded() || scheduleFragment.getView() == null)  {
            return;
        }

        runOnUiThread(() -> scheduleFragment.scheduleUpdate());
    }

    @Override
    public void notificationUpdate() {
        if (notificationFragment == null || !notificationFragment.isAdded() || notificationFragment.getView() == null)  {
            return;
        }

        runOnUiThread(() -> notificationFragment.notificationUpdate());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        locationFragment = null;
        scheduleFragment = null;
        notificationFragment = null;
    }
}