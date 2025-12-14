package com.rr.bubtbustracker.fragment;

import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.view.BusListBottomView;
import com.rr.bubtbustracker.view.CustomInputView;
import com.rr.bubtbustracker.view.CustomView;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.api.API;


public class SignUpFragment extends Fragment {

    private String selectedBus = "Padma";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CustomView signUpBtn = view.findViewById(R.id.sign_up);
        CustomView loginBtn = view.findViewById(R.id.login);
        Button login2Btn = view.findViewById(R.id.login2);
        CustomInputView nameInput = view.findViewById(R.id.name_input);
        CustomInputView emailInput = view.findViewById(R.id.email_input);
        CustomInputView passInput = view.findViewById(R.id.pass_input);
        CustomInputView confirmInput = view.findViewById(R.id.confirm_pass_input);
        LinearLayout busClick = view.findViewById(R.id.busClick);
        TextView busName = view.findViewById(R.id.busName);

        API api = API.getAPI(requireContext());

        busClick.setOnClickListener(v -> {
            hideKeyboard();
            new BusListBottomView(requireContext(), null, busName, bus -> selectedBus = bus);
        });

        signUpBtn.setOnClickListener(v -> {
            String nameText = nameInput.getText();

            hideKeyboard();

            if (nameText.isEmpty()) {
                Toast.makeText(getActivity(), "Name Filed Empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            String emailText = emailInput.getText();

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

            String confirmText = confirmInput.getText();

            if (confirmText.isEmpty()) {
                Toast.makeText(getActivity(), "Confirm Password Filed Empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!passText.equals(confirmText)) {
                Toast.makeText(getActivity(), "Password not Matched!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog loading = new ProgressDialog(requireActivity());
            loading.setMessage("Creating account...");
            loading.setCancelable(false);
            loading.show();

            api.signUp(nameText, emailText, passText, selectedBus.toUpperCase(), json -> {
                if (loading.isShowing()) loading.dismiss();

                try {
                    if (json != null) {
                        String status = json.optString("status");
                        if (!status.isEmpty()) {
                            if (status.equals("SUCCESS")) {
                                App.saveString("name", nameText);
                                App.saveString("bus", selectedBus.toUpperCase());
                                App.saveString("email", emailText);
                                App.saveString("role", "STUDENT");
                                App.saveBoolean("verified", false);
                                App.saveString("id", json.getString("id"));
                                App.saveString("accessToken", json.optString("accessToken", ""));
                                App.saveString("refreshToken", json.optString("refreshToken", ""));
                                App.saveString("requestToken", json.optString("requestToken", ""));
                                App.saveLong("resend_time", System.currentTimeMillis()+60000);
                                App.saveLong("token_time", System.currentTimeMillis()+3000000);
                                if (!json.isNull("schedule")) {
                                    App.saveString("schedule", json.optString("schedule", ""));
                                    App.saveInt("schedule_v", json.optInt("schedule_v", 0));
                                    App.saveLong("schedule_update", System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR);
                                }
                                Toast.makeText(requireContext(), "Account Creating Success.", Toast.LENGTH_SHORT).show();

                                goToVerificationPage();
                            } else {
                                if (status.equals("EMAIL_EXISTS")) {
                                    emailInput.clearText();
                                } else {
                                    confirmInput.clearText();
                                }
                                Toast.makeText(requireContext(), api.getMessage(status), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            passInput.clearText();
                            Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        confirmInput.clearText();
                        Toast.makeText(requireContext(), "Request Error! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    confirmInput.clearText();
                    Toast.makeText(requireContext(), "Exception Error! Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        loginBtn.setOnClickListener(v -> goToLoginPage());

        login2Btn.setOnClickListener(v -> goToLoginPage());
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private void goToLoginPage() {
        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                .replace(R.id.container, new LoginFragment())
                .commit();
    }

    private void goToVerificationPage() {
        VerificationFragment verificationFragment = new VerificationFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("signUp", true);
        verificationFragment.setArguments(bundle);

        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short, R.anim.fade_in_short, R.anim.fade_out_short)
                .replace(R.id.container, verificationFragment)
                .commit();
    }
}
