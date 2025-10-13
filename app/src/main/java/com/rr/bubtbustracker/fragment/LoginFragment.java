package com.rr.bubtbustracker.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.view.CustomInputView;
import com.rr.bubtbustracker.view.CustomView;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.activity.DashboardActivity;
import com.rr.bubtbustracker.api.API;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CustomView loginBtn = view.findViewById(R.id.login);
        CustomView signUpBtn = view.findViewById(R.id.sign_up);
        CustomView signUp2Btn = view.findViewById(R.id.sign_up_2);
        Button forgetBtn = view.findViewById(R.id.forget_password);
        CustomInputView emailInput = view.findViewById(R.id.email_input);
        CustomInputView passInput = view.findViewById(R.id.pass_input);

        API api = API.getAPI();

        loginBtn.setOnClickListener(v -> {
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

            String passText = passInput.getText();

            if (passText.isEmpty()) {
                Toast.makeText(getActivity(), "Password Filed Empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (passText.length() < 6) {
                Toast.makeText(getActivity(), "Password are Short!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (passText.length() > 50) {
                Toast.makeText(getActivity(), "Password are Long!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog loading = new ProgressDialog(requireActivity());
            loading.setMessage("Login account...");
            loading.setCancelable(false);
            loading.show();

            api.login(emailText, passText, json -> {
                if (loading.isShowing()) loading.dismiss();

                try {
                    if (json != null) {
                        Log.d("BusTrackerLog", "onViewCreated: "+json);
                        String status = json.optString("status");
                        if (!status.isEmpty()) {
                            if (status.equals("SUCCESS")) {
                                App.saveString("email", emailText);
                                App.saveString("id", json.getString("id"));
                                App.saveString("name", json.optString("name", ""));
                                App.saveString("rule", json.optString("rule", "").toUpperCase());
                                App.saveString("bus", json.optString("bus", "").toUpperCase());
                                App.saveBoolean("verified", json.optBoolean("verified", false));
                                App.saveString("accessToken", json.optString("accessToken", ""));
                                App.saveString("refreshToken", json.optString("refreshToken", ""));
                                App.saveString("requestToken", json.optString("requestToken", ""));
                                App.saveString("passwordUpdatedAt", json.optString("passwordUpdatedAt", ""));
                                App.saveString("lastLoginAt", json.optString("lastLoginAt", ""));
                                App.saveString("createdAt", json.optString("createdAt", ""));
                                App.saveLong("token_time", System.currentTimeMillis()+3000000);

                                if (json.optBoolean("verified")) {
                                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                                    loading.setMessage("Subscribing to Notifications...");
                                    loading.setCancelable(false);
                                    loading.show();

                                    api.subscribeNotification(requireContext(), subscribe -> {
                                        if (loading.isShowing()) loading.dismiss();
                                        if (subscribe) {
                                            Toast.makeText(requireContext(), "Notification subscribed successfully!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "Subscription failed, continuing without notifications.", Toast.LENGTH_SHORT).show();
                                        }
                                        goToDashboard();
                                    });
                                } else {
                                    Toast.makeText(requireContext(), "Please verify your email first!", Toast.LENGTH_SHORT).show();
                                    goToVerificationPage();
                                }
                            } else {
                                passInput.clearText();
                                Toast.makeText(requireContext(), api.getMessage(status), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            passInput.clearText();
                            Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        passInput.clearText();
                        Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    passInput.clearText();
                    Toast.makeText(requireContext(), "Exception Error! Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        signUpBtn.setOnClickListener(v -> goToSignUpPage());

        signUp2Btn.setOnClickListener(v -> goToSignUpPage());

        forgetBtn.setOnClickListener(v -> goToResetPage());
    }

    private void goToDashboard() {
        Intent intent = new Intent(requireActivity(), DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(App.LOGIN_SUCCESS);
        startActivity(intent);
        requireActivity().overridePendingTransition(0, 0);
    }

    private void goToSignUpPage() {
        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                .replace(R.id.container, new SignUpFragment())
                .commit();
    }

    private void goToResetPage() {
        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                .replace(R.id.container, new ResetFragment())
                .commit();
    }

    private void goToVerificationPage() {
        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                .replace(R.id.container, new VerificationFragment())
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
}
