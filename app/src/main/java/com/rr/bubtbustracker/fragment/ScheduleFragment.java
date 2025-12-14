package com.rr.bubtbustracker.fragment;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.adapter.ScheduleAdapter;
import com.rr.bubtbustracker.api.API;
import com.rr.bubtbustracker.interfaces.OnBusClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScheduleFragment extends Fragment {

    private ScheduleAdapter adapter;
    private ListView listView;
    private boolean dataUpdated = false;
    private boolean isVisibleToUser = false;
    private boolean isFriday = false;
    private OnBusClickListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnBusClickListener) {
            listener = (OnBusClickListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.listView);

        reloadAdapter();
        scheduleUpdateCheck();
    }

    public void scheduleUpdateCheck() {
        long now = System.currentTimeMillis();
        if (0 < now) {
            App.saveLong("schedule_update", now + 60000);
            API.getAPI(requireContext()).scheduleData(schedule -> {
                if (schedule != null) {
                    try {
                        JSONArray data = schedule.getJSONArray("data");
                        App.saveString("schedule", data.toString());
                        App.saveInt("schedule_v", schedule.getInt("version"));
                        App.saveLong("schedule_update", now + AlarmManager.INTERVAL_HOUR);

                        scheduleUpdate();
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    public void scheduleUpdate() {
        dataUpdated = true;

        if (isVisibleToUser && isAdded()) {
            reloadAdapter();
            dataUpdated = false;
        }
    }

    private void reloadAdapter() {
        try {
            JSONArray jsonArray = new JSONArray(App.getString("schedule", "[]"));
            adapter = new ScheduleAdapter(requireContext(), jsonArray, isFriday, (route, id, zoom) -> {
                if (listener != null) {
                    listener.onSelected(route, id, zoom);
                }
            });
            listView.setAdapter(adapter);
        } catch (JSONException e) {}
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

    public void setFriday(boolean friday) {
        isFriday = friday;

        if (isVisibleToUser && isAdded()) {
            adapter.setFriday(friday);
            adapter.notifyDataSetChanged();
        }
    }
}
