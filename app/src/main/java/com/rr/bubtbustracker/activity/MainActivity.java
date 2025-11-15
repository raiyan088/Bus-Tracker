package com.rr.bubtbustracker.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.adapter.ViewPagerAdapter;
import com.rr.bubtbustracker.fragment.LoginFragment;
import com.rr.bubtbustracker.fragment.VerificationFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler(Looper.getMainLooper()).postDelayed(this::initViews, 1500);
    }

    @SuppressLint("InflateParams")
    private void initViews() {
        setTheme(R.style.NoStatusBar);
        EdgeToEdge.enable(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View main = inflater.inflate(R.layout.activity_main, null);

        main.setAlpha(0f);
        setContentView(main);

        main.animate()
                .alpha(1f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        setupWindow(main);
    }

    private void setupWindow(View main) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.parseColor("#2A9DB8"));

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }

        ViewCompat.setOnApplyWindowInsetsListener(main.findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (App.isLogin() && !App.getBoolean("verified", false)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new VerificationFragment()).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new LoginFragment()).commit();
        }
    }
}