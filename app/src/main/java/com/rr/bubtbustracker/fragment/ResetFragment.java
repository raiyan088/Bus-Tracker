package com.rr.bubtbustracker.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.view.CustomInputView;
import com.rr.bubtbustracker.view.CustomView;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.api.API;

public class ResetFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private CustomView resetBtn;
    private LinearLayout timerView;
    private TextView timerText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button goBack = view.findViewById(R.id.go_back);
        CustomInputView emailInput = view.findViewById(R.id.email_input);
        resetBtn = view.findViewById(R.id.reset_password);
        timerText = view.findViewById(R.id.reset_time);
        timerView = view.findViewById(R.id.timer_view);

        long time = App.getLong("reset_time", 0) - System.currentTimeMillis();
        if (time > 0) {
            startTimer(time);
        }

        API api = API.getAPI(requireContext());

        resetBtn.setOnClickListener(v -> {
            String emailText = emailInput.getText();

            hideKeyboard();

            if (emailText.isEmpty()) {
                Toast.makeText(getActivity(), "Email Filed Empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!emailText.contains("@") || emailText.lastIndexOf('@') > emailText.lastIndexOf('.') || emailText.length() > 50) {
                Toast.makeText(getActivity(), "Invalid Email!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog loading = new ProgressDialog(requireActivity());
            loading.setMessage("Sending reset password email...");
            loading.setCancelable(false);
            loading.show();

            api.reset(emailText, json -> {
                if (loading.isShowing()) loading.dismiss();
                String message = "Request Error! Please try again.";
                try {
                    if (json != null) {
                        String status = json.optString("status");
                        if (!status.isEmpty()) {
                            if (status.equals("SUCCESS")) {
                                emailInput.clearText();
                                message = "Reset password email sent successfully!";
                                App.saveLong("reset_time", System.currentTimeMillis()+60000);
                                startTimer(60000);
                            } else {
                                message = api.getMessage(status);
                            }
                        }
                    }
                } catch (Exception e) {
                    message = "Exception Error! Please try again.";
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        });

        goBack.setOnClickListener(v -> goToLoginPage());
    }

    @SuppressLint("SetTextI18n")
    private void startTimer(long time) {
        int seconds = Math.toIntExact(time / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        resetBtn.disable();
        timerView.setVisibility(View.VISIBLE);

        timerText.setText("Resend in\n"+minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));

        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long time = App.getLong("reset_time", 0) - System.currentTimeMillis();
                if (time > 0) {
                    int seconds = Math.toIntExact(time / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    timerText.setText("Resend in\n"+minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));

                    handler.postDelayed(this, 1000);
                } else {
                    resetBtn.enable();
                    timerView.setVisibility(View.INVISIBLE);
                }
            }
        };

        handler.post(timerRunnable);
    }

    private void goToLoginPage() {
        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                .replace(R.id.container, new LoginFragment())
                .commit();
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }
}
