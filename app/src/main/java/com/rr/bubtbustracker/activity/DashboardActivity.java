package com.rr.bubtbustracker.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.fragment.LocationFragment;
import com.rr.bubtbustracker.fragment.NotificationFragment;
import com.rr.bubtbustracker.fragment.ScheduleFragment;
import com.rr.bubtbustracker.view.BusListBottomView;

import java.util.Objects;


public class DashboardActivity extends AppCompatActivity {

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (App.isLogin() && App.getBoolean("verified", false)) {
            Intent intent = getIntent();
            if (intent != null && Objects.equals(intent.getAction(), App.LOGIN_SUCCESS)) {
                initViews(false);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(() -> initViews(true), 720);
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


        int toolbarHeight;

        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            toolbarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        } else {
            toolbarHeight = (int) (56 * getResources().getDisplayMetrics().density);
        }

        View topBar = main.findViewById(R.id.top_bar);
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = systemBars.top + toolbarHeight;
            v.setLayoutParams(params);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            View content = main.findViewById(R.id.main);
            content.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
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

        LinearLayout busClick = main.findViewById(R.id.busClick);
        TextView busName = main.findViewById(R.id.busName);

        busName.setText(capitalizeFirst(App.getString("bus", "Padma")));

        busClick.setOnClickListener(v -> {
            new BusListBottomView(this, null, busName, null);
        });

        LinearLayout scheduleLayout = main.findViewById(R.id.schedule_layout);
        LinearLayout notificationLayout = main.findViewById(R.id.notification_layout);
        FloatingActionButton locationFab = main.findViewById(R.id.location);

        ImageView actionSchedule = main.findViewById(R.id.action_schedule);
        ImageView actionNotification = main.findViewById(R.id.action_notification);
        TextView scheduleText = main.findViewById(R.id.schedule_text);
        TextView notificationText = main.findViewById(R.id.notification_text);

        int blue = ContextCompat.getColor(this, R.color.blue);
        int gray = ContextCompat.getColor(this, R.color.gray);

        ScheduleFragment scheduleFragment = new ScheduleFragment();
        LocationFragment locationFragment = new LocationFragment();
        NotificationFragment notificationFragment = new NotificationFragment();

        getSupportFragmentManager().beginTransaction().replace(R.id.container, locationFragment).commit();

        scheduleLayout.setOnClickListener(v -> {
            actionSchedule.setColorFilter(blue);
            scheduleText.setTextColor(blue);

            actionNotification.setColorFilter(gray);
            notificationText.setTextColor(gray);

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                    .replace(R.id.container, scheduleFragment)
                    .commit();
        });

        notificationLayout.setOnClickListener(v -> {
            actionNotification.setColorFilter(blue);
            notificationText.setTextColor(blue);

            actionSchedule.setColorFilter(gray);
            scheduleText.setTextColor(gray);

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                    .replace(R.id.container, notificationFragment)
                    .commit();
        });

        locationFab.setOnClickListener(v -> {
            actionNotification.setColorFilter(gray);
            notificationText.setTextColor(gray);

            actionSchedule.setColorFilter(gray);
            scheduleText.setTextColor(gray);

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                    .replace(R.id.container, locationFragment)
                    .commit();
        });
    }

    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return "";
        input = input.toLowerCase();
        return input.substring(0, 1).toUpperCase() + input.substring(1);
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
            Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}