package com.rr.bubtbustracker.fragment;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LocationFragment extends Fragment {

    private GoogleMap mMap;
    private float lastZoom = 0;
    private Marker startMarker;
    private Marker endMarker;
    private Polyline polylineOuter;
    private Polyline polylineInner;
    private HashMap<String, Circle> outerCircle;
    private HashMap<String, Circle> innerCircle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        Button copyBtn = view.findViewById(R.id.copyLocation);
        copyBtn.setOnClickListener(v -> {
            if (mMap != null) {
                LatLng center = mMap.getCameraPosition().target; // Map center
                if (startMarker != null) {
                    startMarker.remove();
                }

                if (endMarker != null) {
                    endMarker.remove();
                }

                // নতুন marker add করা
                startMarker = mMap.addMarker(new MarkerOptions()
                        .position(center)
                        .title("Selected Location"));

                String lat = String.format(Locale.US, "%.5f", center.latitude);
                String lng = String.format(Locale.US, "%.5f", center.longitude);

                // Clipboard এ copy করা
                ClipData clip = ClipData.newPlainText("LatLng", lat + "," + lng);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(requireContext(), "Copied: " + lat + "," + lng, Toast.LENGTH_SHORT).show();
            }
        });

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;

                LatLng bubtLocation = new LatLng(23.81189,90.35711);
                startMarker = mMap.addMarker(new MarkerOptions().position(bubtLocation).title("Bangladesh University of Business and Technology (BUBT)"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bubtLocation, 15));

                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                mMap.getUiSettings().setZoomControlsEnabled(false);

                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {

                        if (startMarker != null) {
                            startMarker.remove();
                        }

                        if (endMarker != null) {
                            endMarker.remove();
                        }

                        startMarker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Selected Location"));

                        String lat = String.format(Locale.US, "%.5f", latLng.latitude);
                        String lng = String.format(Locale.US, "%.5f", latLng.longitude);

                        ClipData clip = ClipData.newPlainText("LatLng", lat + "," + lng);
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(requireContext(), "Copied: " + lat + "," + lng, Toast.LENGTH_SHORT).show();
                    }
                });

                mMap.setOnCameraMoveListener(() -> {
                    if (polylineInner == null || polylineOuter == null) return;

                    float zoom = mMap.getCameraPosition().zoom;
                    float currentZoom = (float) (Math.round(zoom * 100.0) / 100.0);

                    if (currentZoom != lastZoom) {
                        lastZoom = currentZoom;

                        updateMapZoom();
                    }
                });

                mMap.setOnCameraIdleListener(() -> {
                    Log.d("BusTrackerLog", "onViewCreated: "+mMap.getCameraPosition().zoom);
                    if (polylineInner == null || polylineOuter == null) return;

                    updateMapZoom();
                });
            });
        }
    }

    private void updateMapZoom() {
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

        double circlePixelSize = innerWidth + 10f;

        if (innerCircle != null) {
            for (String key: innerCircle.keySet()) {
                Circle circle = innerCircle.get(key);
                if (circle != null) {
                    LatLng center = circle.getCenter();
                    double radius = getRadiusInMeters(mMap, center, (float)circlePixelSize);
                    circle.setRadius(radius);
                }
            }
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

    public void updateRoute(JSONObject route, String id, float zoom) {
        if (mMap == null || route == null) return;

        try {
            LatLng start = null;
            LatLng selected = null;
            LatLng end = null;
            String startName = "";
            String selectedName = "";
            String endName = "";

            List<LatLng> points = new ArrayList<>();

            removeAllView();

            float mapZoom = zoom;
            innerCircle = new HashMap<>();
            outerCircle = new HashMap<>();

            Iterator<String> keys = route.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject value = route.getJSONObject(key);
                    boolean isSelect = id != null && !id.isEmpty() && key.equals(id);
                    if (isSelect) {
                        mapZoom = (float) value.optDouble("x", zoom);
                    }
                    JSONArray list = value.optJSONArray("l");
                    if (list != null) {
                        for (int i = 0; i < list.length(); i++) {
                            try {
                                String[] split = list.getString(i).split(",");
                                if (split.length >= 2) {
                                    LatLng latLng = new LatLng(Float.parseFloat(split[0].trim()), Float.parseFloat(split[1].trim()));
                                    if (split.length >= 3) {
                                        end = latLng;
                                        endName = value.optString("n", "");
                                        if (start == null) {
                                            start = latLng;
                                            startName = endName;
                                        }
                                        if (isSelect) {
                                            selected = latLng;
                                            selectedName = endName;
                                        }
                                        Circle circle = mMap.addCircle(new CircleOptions()
                                                .center(latLng)
                                                .radius(40)
                                                .strokeColor(Color.TRANSPARENT)
                                                .fillColor(Color.parseColor("#20000000"))
                                                .strokeWidth(0)
                                                .zIndex(1));
                                        circle.setTag(key);

                                        outerCircle.put(key, circle);

                                        circle = mMap.addCircle(new CircleOptions()
                                                .center(latLng)
                                                .radius(20)
                                                .strokeColor(Color.parseColor("#FF757575"))
                                                .fillColor(Color.WHITE)
                                                .strokeWidth(6)
                                                .clickable(true)
                                                .zIndex(2));
                                        circle.setTag(key);

                                        innerCircle.put(key, circle);
                                    }
                                    points.add(latLng);
                                }
                            } catch (Exception e) {}
                        }
                    }


                } catch (Exception e) {}
            }

            if (selected == null) {
                if (start != null && end != null) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(start);
                    builder.include(end);
                    LatLngBounds bounds = builder.build();

                    startMarker = mMap.addMarker(new MarkerOptions()
                        .position(start)
                        .title(startName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    endMarker = mMap.addMarker(new MarkerOptions()
                            .position(end)
                            .title(endName)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
                }
            } else {
                startMarker = mMap.addMarker(new MarkerOptions()
                        .position(selected)
                        .title(selectedName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selected, mapZoom));
            }

            if (!points.isEmpty()) {

                PolylineOptions outer = new PolylineOptions()
                        .addAll(points)
                        .width(18)
                        .color(Color.parseColor("#0A12D9"))
                        .geodesic(true);
                polylineOuter = mMap.addPolyline(outer);

                PolylineOptions inner = new PolylineOptions()
                        .addAll(points)
                        .width(12)
                        .color(Color.parseColor("#0F53FE"))
                        .geodesic(true);

                polylineInner = mMap.addPolyline(inner);
            }
        } catch (Exception e) {}
    }

    private void removeAllView() {
        if (startMarker != null) {
            startMarker.remove();
            startMarker = null;
        }

        if (endMarker != null) {
            endMarker.remove();
            endMarker = null;
        }

        if (polylineOuter != null) {
            polylineOuter.remove();
            polylineOuter = null;
        }

        if (polylineInner != null) {
            polylineInner.remove();
            polylineInner = null;
        }

        if (innerCircle != null) {
            for (String key: innerCircle.keySet()) {
                Circle circle = innerCircle.get(key);
                if (circle != null) {
                    circle.remove();
                }
            }
            innerCircle = null;
        }

        if (outerCircle != null) {
            for (String key: outerCircle.keySet()) {
                Circle circle = outerCircle.get(key);
                if (circle != null) {
                    circle.remove();
                }
            }
            outerCircle = null;
        }
    }
}
