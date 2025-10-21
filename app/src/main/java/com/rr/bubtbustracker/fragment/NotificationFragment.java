package com.rr.bubtbustracker.fragment;

import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.api.API;

public class NotificationFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText title = view.findViewById(R.id.title);
        EditText body = view.findViewById(R.id.body);
        Button send = view.findViewById(R.id.send);

        API api =  new API();

        send.setOnClickListener(v -> {
            String titleText = title.getText().toString();

            if (titleText.isEmpty()) {
                Toast.makeText(getActivity(), "Email Filed Empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            String bodyText = body.getText().toString();

            if (bodyText.isEmpty()) {
                Toast.makeText(getActivity(), "Password Filed Empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog loading = new ProgressDialog(requireActivity());
            loading.setMessage("Send Notification...");
            loading.setCancelable(false);
            loading.show();

            api.notification(titleText, bodyText, App.getString("bus", "padma").toUpperCase(), json -> {
                if (loading.isShowing()) loading.dismiss();
                try {
                    if (json != null) {
                        String status = json.optString("status");
                        if (!status.isEmpty()) {
                            if (status.equals("SUCCESS")) {
                                Toast.makeText(requireContext(), "Notification Send successfully!", Toast.LENGTH_SHORT).show();
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
    }
}
