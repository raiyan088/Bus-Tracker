package com.rr.bubtbustracker.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocationFragment extends Fragment {

    private GoogleMap mMap;
    private float lastZoom = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;
//
                LatLng bubtLocation = new LatLng(23.81189,90.35711);
//                mMap.addMarker(new MarkerOptions().position(bubtLocation).title("Bangladesh University of Business and Technology (BUBT)"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bubtLocation, 15));

                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                mMap.getUiSettings().setZoomControlsEnabled(false);

                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                LatLng start = new LatLng(23.81174, 90.35676);
                LatLng end = new LatLng(23.79858, 90.35314);
                List<LatLng> points = new ArrayList<>();
                points.add(start);
                points.add(new LatLng(23.80812, 90.35792));
                points.add(new LatLng(23.806, 90.35151));
                points.add(new LatLng(23.8, 90.35513));
                points.add(new LatLng(23.79982, 90.35503));
                points.add(end);

                PolylineOptions outer = new PolylineOptions()
                        .addAll(points)
                        .width(18)
                        .color(Color.parseColor("#0A12D9"))
                        .geodesic(true);
                Polyline polylineOuter = mMap.addPolyline(outer);

                PolylineOptions inner = new PolylineOptions()
                        .addAll(points)
                        .width(12)
                        .color(Color.parseColor("#0F53FE"))
                        .geodesic(true);

                Polyline polylineInner = mMap.addPolyline(inner);

                mMap.addMarker(new MarkerOptions()
                        .position(start)
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                mMap.addMarker(new MarkerOptions()
                        .position(end)
                        .title("End")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                mMap.addCircle(new CircleOptions()
                        .center(start)
                        .radius(40)
                        .strokeColor(Color.TRANSPARENT)
                        .fillColor(Color.parseColor("#20000000"))
                        .strokeWidth(0)
                        .zIndex(1));

                mMap.addCircle(new CircleOptions()
                        .center(end)
                        .radius(40)
                        .strokeColor(Color.TRANSPARENT)
                        .fillColor(Color.parseColor("#20000000"))
                        .strokeWidth(0)
                        .zIndex(1));

                Circle startCircle = mMap.addCircle(new CircleOptions()
                        .center(start)
                        .radius(20)
                        .strokeColor(Color.parseColor("#FF757575"))
                        .fillColor(Color.WHITE)
                        .strokeWidth(6)
                        .zIndex(2));

                Circle endCircle = mMap.addCircle(new CircleOptions()
                        .center(end)
                        .radius(20)
                        .strokeColor(Color.parseColor("#FF757575"))
                        .fillColor(Color.WHITE)
                        .strokeWidth(6)
                        .zIndex(2));

                mMap.setOnCameraMoveListener(() -> {
                    float zoom = mMap.getCameraPosition().zoom;
                    float currentZoom = (float) (Math.round(zoom * 100.0) / 100.0);

                    if (currentZoom != lastZoom) {
                        lastZoom = currentZoom;

                        float baseWidthAt17 = 30.0f;
                        float scalingFactor = 10.0f;

                        float innerWidth = baseWidthAt17 + (zoom - 17f) * scalingFactor;

                        float minimumWidth = 15.0f;

                        if (innerWidth < minimumWidth) {
                            innerWidth = minimumWidth;
                        }

                        float outerWidth = innerWidth + 6f;

                        polylineInner.setWidth(innerWidth);
                        polylineOuter.setWidth(outerWidth);

                        double circlePixelSize = innerWidth + 6f;

                        double startRadius = getRadiusInMeters(mMap, start, (float)circlePixelSize);
                        double endRadius = getRadiusInMeters(mMap, end, (float)circlePixelSize);

                        startCircle.setRadius(startRadius);
                        endCircle.setRadius(endRadius);
                    }
                });

                mMap.setOnCameraIdleListener(() -> {
                    float zoom = mMap.getCameraPosition().zoom;
                    float baseWidthAt17 = 30.0f;
                    float scalingFactor = 10.0f;

                    float innerWidth = baseWidthAt17 + (zoom - 17f) * scalingFactor;

                    float minimumWidth = 15.0f;

                    if (innerWidth < minimumWidth) {
                        innerWidth = minimumWidth;
                    }

                    float outerWidth = innerWidth + 6f;

                    polylineInner.setWidth(innerWidth);
                    polylineOuter.setWidth(outerWidth);

                    double circlePixelSize = innerWidth + 6f;

                    double startRadius = getRadiusInMeters(mMap, start, (float)circlePixelSize);
                    double endRadius = getRadiusInMeters(mMap, end, (float)circlePixelSize);

                    startCircle.setRadius(startRadius);
                    endCircle.setRadius(endRadius);
                });
            });
        }
    }

    private double getRadiusInMeters(GoogleMap map, LatLng center, float radiusInPx) {
        Projection projection = map.getProjection();
        Point screenPos = projection.toScreenLocation(center);

        Point screenPosOffset = new Point(screenPos.x + (int)radiusInPx, screenPos.y);
        LatLng offsetLatLng = projection.fromScreenLocation(screenPosOffset);

        float[] results = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                offsetLatLng.latitude, offsetLatLng.longitude, results);

        return results[0];
    }

    public void updateRoute(JSONObject route, String id) {

    }
}
