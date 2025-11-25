package com.rr.bubtbustracker.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.model.RoutePoint;
import com.rr.bubtbustracker.interfaces.OnBusClickListener;
import com.rr.bubtbustracker.view.RouteMapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScheduleAdapter extends BaseAdapter {

    private final Context context;
    private final JSONArray busList;
    private boolean isFriday;
    private final OnBusClickListener listener;

    public ScheduleAdapter(Context context, JSONArray busList, boolean isFriday, OnBusClickListener listener) {
        this.context = context;
        this.busList = busList;
        this.isFriday = isFriday;
        this.listener = listener;
    }

    public void setFriday(boolean friday) {
        isFriday = friday;
    }


    @Override
    public int getCount() {
        return busList.length();
    }

    @Override
    public Object getItem(int position) {
        try {
            return busList.getJSONObject(position);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View _view, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.bus_schedule_list, parent, false);

        LinearLayout horizontalList = view.findViewById(R.id.horizontalList);
        RouteMapView routeMapView = view.findViewById(R.id.routeMapView);
        LinearLayout busClick = view.findViewById(R.id.busClick);
        TextView busName = view.findViewById(R.id.busName);

        try {
            JSONObject jsonObject = busList.getJSONObject(position);
            String name = jsonObject.getString("n");
            busName.setText(name);

            double zoom = jsonObject.optDouble("z", 16);

            JSONObject route = jsonObject.getJSONObject("r");
            Iterator<String> keys = route.keys();

            JSONArray timeList = jsonObject.getJSONObject("t").getJSONArray(isFriday?"f":"o");

            for (int i = 0; i < timeList.length(); i++) {
                JSONObject timeObj = timeList.getJSONObject(i);
                View timeView = LayoutInflater.from(context).inflate(R.layout.bus_schedule_time, parent, false);
                TextView startTime = timeView.findViewById(R.id.startTime);
                TextView endTime = timeView.findViewById(R.id.endTime);
                startTime.setText(timeObj.getString("s"));
                endTime.setText(timeObj.getString("e"));
                horizontalList.addView(timeView);
            }

            List<RoutePoint> routeList = new ArrayList<>();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject value = route.getJSONObject(key);
                routeList.add(new RoutePoint(key, value.getString("n")));
            }
            routeMapView.setRoutePoints(routeList);

            busClick.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSelected(route, "", (float) zoom);
                }
            });

            routeMapView.setOnPointClickListener(pointId -> {
                if (listener != null) {
                    listener.onSelected(route, pointId, (float) zoom);
                }
            });
        } catch (Exception e) {}

        return view;
    }
}
