package com.rr.bubtbustracker.fragment;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.view.CustomView;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.activity.DashboardActivity;
import com.rr.bubtbustracker.api.API;

public class VerificationFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private CustomView verificationBtn;
    private CustomView statusCheckBtn;
    private LinearLayout timerView;
    private TextView timerText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        verificationBtn = view.findViewById(R.id.resend_email);
        statusCheckBtn = view.findViewById(R.id.check_status);
        timerText = view.findViewById(R.id.resend_time);
        timerView = view.findViewById(R.id.timer_view);
        TextView details = view.findViewById(R.id.details);
        Button goBack = view.findViewById(R.id.go_back);

        long now = System.currentTimeMillis();
        long resendTime = App.getLong("resend_time", 0) - now;
        long refreshTime = App.getLong("refresh_time", 0) - now;
        if (resendTime > 0 || refreshTime > 0) {
            startTimer(resendTime, refreshTime);
        }

        API api = API.getAPI(requireContext());

        String part1 = "Please check your ";
        String email = App.getString("email","");
        String part3 = " inbox. We have sent a a verification link to your email address. If you didn't receive it, can resend the the email.";

        String fullText = part1 + email + part3;

        SpannableString spannableString = new SpannableString(fullText);

        int startEmail = part1.length();
        int endEmail = startEmail + email.length();

        ForegroundColorSpan blueColor = new ForegroundColorSpan(Color.parseColor("#1E88E5"));
        spannableString.setSpan(blueColor, startEmail, endEmail, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        UnderlineSpan underline = new UnderlineSpan();
        spannableString.setSpan(underline, startEmail, endEmail, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        details.setText(spannableString);

        verificationBtn.setOnClickListener(v -> {
            ProgressDialog loading = new ProgressDialog(requireActivity());
            loading.setMessage("Sending verification email...");
            loading.setCancelable(false);
            loading.show();

            api.verification(App.getString("refreshToken", ""), App.getString("accessToken", ""), json -> {
                if (loading.isShowing()) loading.dismiss();

                try {
                    if (json != null) {
                        Log.d("BusTrackerLog", "onViewCreated: "+json);
                        String status = json.optString("status");
                        if (!status.isEmpty()) {
                            if (status.equals("SUCCESS")) {
                                App.saveString("id", json.getString("id"));
                                App.saveBoolean("verified", json.optBoolean("verified", false));
                                App.saveString("passwordUpdatedAt", json.optString("passwordUpdatedAt", ""));
                                App.saveString("lastLoginAt", json.optString("lastLoginAt", ""));
                                App.saveString("createdAt", json.optString("createdAt", ""));
                                if (!json.isNull("latestToken")) {
                                    App.saveString("accessToken", json.optString("latestToken", ""));
                                    App.saveLong("token_time", System.currentTimeMillis()+3000000);
                                }
                                if (!json.isNull("schedule")) {
                                    App.saveString("schedule", json.optString("schedule", ""));
                                    App.saveInt("schedule_v", json.optInt("schedule_v", 0));
                                    App.saveLong("schedule_update", System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR);
                                }
                                App.saveLong("resend_time", System.currentTimeMillis()+60000);

                                if (json.optBoolean("verified")) {
                                    api.subscribeNotification(requireContext(), null);
                                    Toast.makeText(requireContext(), "Login success. Go To Dashboard!", Toast.LENGTH_SHORT).show();
                                    goToDashboard();
                                } else {
                                    Toast.makeText(requireContext(), "Please check your inbox!", Toast.LENGTH_SHORT).show();
                                    startTimer(60000, App.getLong("refresh_time", 0));
                                }
                            } else {
                                Toast.makeText(requireContext(), api.getMessage(status), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Exception Error! Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        statusCheckBtn.setOnClickListener(v -> {
            ProgressDialog loading = new ProgressDialog(requireActivity());
            loading.setMessage("Verification status checking...");
            loading.setCancelable(false);
            loading.show();

            api.checkStatus(App.getString("refreshToken", ""), App.getString("accessToken", ""), App.getString("requestToken", ""), App.getLong("token_time", 0), json -> {
                if (loading.isShowing()) loading.dismiss();

                try {
                    if (json != null) {
                        String status = json.optString("status");
                        if (!status.isEmpty()) {
                            if (status.equals("SUCCESS")) {
                                App.saveString("id", json.getString("id"));
                                App.saveBoolean("verified", json.optBoolean("verified", false));
                                App.saveString("passwordUpdatedAt", json.optString("passwordUpdatedAt", ""));
                                App.saveString("lastLoginAt", json.optString("lastLoginAt", ""));
                                App.saveString("createdAt", json.optString("createdAt", ""));
                                if (!json.isNull("latestToken")) {
                                    App.saveString("accessToken", json.optString("latestToken", ""));
                                    App.saveLong("token_time", System.currentTimeMillis()+3300000);
                                }
                                App.saveLong("refresh_time", System.currentTimeMillis()+15000);

                                if (json.optBoolean("verified")) {
                                    api.subscribeNotification(requireContext(), null);
                                    Toast.makeText(requireContext(), "Login success. Go To Dashboard!", Toast.LENGTH_SHORT).show();
                                    goToDashboard();
                                } else {
                                    Toast.makeText(requireContext(), "Your account is not verified. Please check your email to verify.", Toast.LENGTH_SHORT).show();
                                    startTimer(App.getLong("resend_time", 0), 30000);
                                }
                            } else {
                                Toast.makeText(requireContext(), api.getMessage(status), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Exception Error! Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        goBack.setOnClickListener(v -> goToLoginPage());
    }

    @SuppressLint("SetTextI18n")
    private void startTimer(long resendTime, long refreshTime) {
        int seconds = Math.toIntExact(Math.max(resendTime, refreshTime) / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        if (resendTime > 0) {
            verificationBtn.disable();
        }
        if (refreshTime > 0) {
            statusCheckBtn.disable();
        }
        timerView.setVisibility(View.VISIBLE);

        timerText.setText("Resend in\n"+minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));

        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long resendTime = App.getLong("resend_time", 0) - now;
                long refreshTime = App.getLong("refresh_time", 0) - now;

                if (resendTime <= 0) {
                    verificationBtn.enable();
                }
                if (refreshTime <= 0) {
                    statusCheckBtn.enable();
                }

                if (resendTime > 0 || refreshTime > 0) {
                    int seconds = Math.toIntExact(Math.max(resendTime, refreshTime) / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    timerText.setText("Resend in\n"+minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));

                    handler.postDelayed(this, 1000);
                } else {
                    timerView.setVisibility(View.INVISIBLE);
                }
            }
        };

        handler.post(timerRunnable);
    }

    private void goToDashboard() {
        Intent intent = new Intent(requireActivity(), DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(App.LOGIN_SUCCESS);
        startActivity(intent);
    }

    private void goToLoginPage() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Wrong Email?")
                .setMessage("It seems you entered the wrong email. Do you want to go back and correct it?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();

                    App.clearAll();

                    FragmentManager fm = getParentFragmentManager();
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                            .replace(R.id.container, new LoginFragment())
                            .commit();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }
}
