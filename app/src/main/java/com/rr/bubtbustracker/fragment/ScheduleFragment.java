package com.rr.bubtbustracker.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.file.RoutePoint;
import com.rr.bubtbustracker.view.RouteMapView;

import java.util.Arrays;
import java.util.List;

public class ScheduleFragment  extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RouteMapView routeMapView = view.findViewById(R.id.route_map_view);

        List<RoutePoint> stationData = Arrays.asList(
                new RoutePoint("1", "ECB Chattar"),
                new RoutePoint("2", "Kalshi Bridge"),
                new RoutePoint("3", "Mirpur-12"),
                new RoutePoint("4", "Duaripara"),
                new RoutePoint("5", "BUBT3"),
                new RoutePoint("6", "BUBT4"),
                new RoutePoint("7", "BUBT")
        );

        routeMapView.setRoutePoints(stationData);

        routeMapView.setOnPointClickListener(new RouteMapView.OnPointClickListener() {
            @Override
            public void onPointClicked(String pointId) {
                Toast.makeText(requireContext(), "Clicked Point ID: " + pointId, Toast.LENGTH_SHORT).show();
            }
        });
    }
}