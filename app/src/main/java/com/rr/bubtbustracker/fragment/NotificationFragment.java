package com.rr.bubtbustracker.fragment;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.adapter.NotificationAdapter;
import com.rr.bubtbustracker.adapter.ScheduleAdapter;
import com.rr.bubtbustracker.api.API;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationFragment extends Fragment {

    private TextView tvNotificationEmpty;
    private RecyclerView notificationList;
    private boolean dataUpdated = false;
    private boolean isVisibleToUser = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNotificationEmpty = view.findViewById(R.id.tvNotificationEmpty);
        notificationList = view.findViewById(R.id.recyclerView);

        reloadAdapter();
    }

    public void notificationUpdate() {
        dataUpdated = true;

        if (isVisibleToUser && isAdded()) {
            reloadAdapter();
            dataUpdated = false;
        }
    }

    private void reloadAdapter() {
        try {
            JSONArray jsonArray = new JSONArray(App.getString("notification", "[]"));
            if (jsonArray.length() > 0) {
                notificationList.setVisibility(VISIBLE);
                tvNotificationEmpty.setVisibility(GONE);

                NotificationAdapter mAdapter = new NotificationAdapter(jsonArray);
                notificationList.setAdapter(mAdapter);
                notificationList.setLayoutManager(new LinearLayoutManager(requireContext()));
                notificationList.setHasFixedSize(true);

                mAdapter.setOnItemClickListener(position -> {
                    try {
                        JSONObject object = jsonArray.optJSONObject(position);
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle(object.optString("title"));
                        builder.setMessage(object.optString("body"));
                        builder.setPositiveButton("Cancel", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } catch (Exception ignored) {}
                });
            } else {
                notificationList.setVisibility(GONE);
                tvNotificationEmpty.setVisibility(VISIBLE);
            }
        } catch (JSONException ignored) {}
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        isVisibleToUser = !hidden;

        if (!hidden && dataUpdated) {
            reloadAdapter();
            dataUpdated = false;
        }
    }
}
