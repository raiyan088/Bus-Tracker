package com.rr.bubtbustracker.activity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.api.API;
import com.rr.bubtbustracker.fragment.LocationFragment;
import com.rr.bubtbustracker.fragment.NotificationFragment;
import com.rr.bubtbustracker.fragment.ScheduleFragment;
import com.rr.bubtbustracker.interfaces.OnBusClickListener;
import com.rr.bubtbustracker.services.MyNotificationService;
import com.rr.bubtbustracker.view.BusListBottomView;
import com.rr.bubtbustracker.view.ScheduleBottomView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class DashboardActivity extends AppCompatActivity implements OnBusClickListener {

    private String mToday = "Friday";
    private String mBusName = "Padma";
    private int tabClick = 1;
    private int blue;
    private int gray;
    boolean isFridayClicked = false;
    boolean isOtherDayClicked = false;
    private LinearLayout titleClick;
    private TextView titleName;
    private ImageView titleIcon;
    private ImageView actionSchedule;
    private ImageView actionNotification;
    private TextView scheduleText;
    private TextView notificationText;
    private FragmentManager fragmentManager;
    private ScheduleFragment scheduleFragment;
    private LocationFragment locationFragment;
    private NotificationFragment notificationFragment;

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

        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }

        toolbar.setTitle("Location");

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,  drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        titleClick = main.findViewById(R.id.titleClick);
        titleName = main.findViewById(R.id.titleName);
        titleIcon = main.findViewById(R.id.titleIcon);

        mBusName = capitalizeFirst(App.getString("bus", "Padma"));
        titleName.setText(mBusName);

        scheduleFragment = new ScheduleFragment();
        locationFragment = new LocationFragment();
        notificationFragment = new NotificationFragment();

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.add(R.id.container, locationFragment, "location");
        ft.add(R.id.container, scheduleFragment, "schedule").hide(scheduleFragment);
        ft.add(R.id.container, notificationFragment, "notification").hide(notificationFragment);
        ft.commit();

        API api = new API();

        titleClick.setOnClickListener(v -> {
            if (tabClick == 1) {
                new BusListBottomView(this, null, null, newBus -> {
                    String currentBus = App.getString("bus", "Padma");
                    if (!Objects.equals(currentBus.toUpperCase(), newBus.toUpperCase())) {
                        new AlertDialog.Builder(this)
                                .setTitle("Bus Change Warning")
                                .setMessage("Changing from " + capitalizeFirst(currentBus) + " to " + newBus + ".\n\n" +
                                        "• Your notification will be updated.\n" +
                                        "• App data may be refreshed.\n" +
                                        "• Make sure you want to continue.")
                                .setCancelable(false)
                                .setPositiveButton("Continue", (dialog, which) -> {
                                    busChange(api, titleName, newBus);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .show();
                    }
                });
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

        LinearLayout scheduleLayout = main.findViewById(R.id.schedule_layout);
        LinearLayout notificationLayout = main.findViewById(R.id.notification_layout);
        FloatingActionButton locationFab = main.findViewById(R.id.location);

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

                changeTitle(titleClick, titleName, titleIcon, "Schedule", mToday, R.drawable.ic_schedule, true);

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

                changeTitle(titleClick, titleName, titleIcon, "Notification","", 0, false);

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
            }
        });
    }

    private void locationTabClick() {
        tabClick = 1;
        actionNotification.setColorFilter(gray);
        notificationText.setTextColor(gray);

        actionSchedule.setColorFilter(gray);
        scheduleText.setTextColor(gray);

        changeTitle(titleClick, titleName, titleIcon, "Location", mBusName, R.drawable.ic_bus, true);

        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short)
                .hide(scheduleFragment)
                .hide(notificationFragment)
                .show(locationFragment)
                .commit();
    }

    private void busChange(API api, TextView busName, String newBus) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Changing bus...");
        loading.setCancelable(false);
        loading.show();

        api.busChange(App.getString("id", ""), newBus.toUpperCase(), json -> {
            if (loading.isShowing()) loading.dismiss();
            String message = "Request Error! Please try again.";
            try {
                if (json != null) {
                    String status = json.optString("status");
                    if (!status.isEmpty()) {
                        if (status.equals("SUCCESS")) {
                            message = "";
                            subscriptionChange(api, busName, newBus);
                        } else {
                            message = api.getMessage(status);
                        }
                    }
                }
            } catch (Exception e) {
                message = "Exception Error! Please try again.";
            }
            if (!message.isEmpty()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void subscriptionChange(API api, TextView busName, String newBus) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Unsubscribing from old bus...");
        loading.setCancelable(false);
        loading.show();

        api.unSubscribeNotification(getApplicationContext(), unSubscribe -> {
            if (unSubscribe) {
                mBusName = newBus;
                loading.setMessage("Subscribing to new bus...");
                App.saveString("bus", newBus.toUpperCase());
                busName.setText(newBus);
                api.subscribeNotification(getApplicationContext(), subscribe -> {
                    if (loading.isShowing()) loading.dismiss();
                    loadBusLocation(newBus);
                    if (subscribe) {
                        Toast.makeText(this, "Bus changed to " + newBus, Toast.LENGTH_SHORT).show();
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

    private void loadBusLocation(String busName) {

    }

    private void changeTitle(View titleClick, TextView titleName, ImageView titleIcon, String title, String newText, int newIcon, boolean visible) {
        titleName.animate().alpha(0f).setDuration(125).withEndAction(() -> {
            titleName.setText(newText);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.setTitle(title);
            titleName.animate().alpha(1f).setDuration(125).start();
        }).start();

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
                    .withEndAction(() -> titleClick.setVisibility(View.INVISIBLE))
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
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            App.saveString("schedule", "[]");
            App.saveLong("schedule_update", 0);
            Toast.makeText(this, "schedule clear", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}