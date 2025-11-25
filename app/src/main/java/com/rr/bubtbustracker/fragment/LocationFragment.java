package com.rr.bubtbustracker.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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
import com.rr.bubtbustracker.db.TripDatabaseHelper;
import com.rr.bubtbustracker.interfaces.FragmentReadyListener;
import com.rr.bubtbustracker.model.TripLocation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class LocationFragment extends Fragment {

    private GoogleMap mMap;
    private ImageView busRoute;
    private ProgressBar busRouteLoading;
    private TextView fromName;
    private TextView toName;
    private LinearLayout distView;
    private float lastZoom = 0;
    private Marker startMarker;
    private Marker endMarker;
    private Polyline polylineOuter;
    private Polyline polylineInner;
    private List<Circle> outerCircle;
    private List<Circle> innerCircle;
    private Circle busOuterCircle = null;
    private Circle busInnerCircle = null;
    private double busOuterRadius = 40;
    private double busInnerRadius = 20;
    private LatLng previousBusLocation = null;
    private ValueAnimator busAnimator;
    private LatLng currentAnimatedLocation = null;
    private long lastUpdateTime = 0;
    private boolean isShowBusRute = false;
    private boolean isAutoMove = false;
    private ArrayList<LatLng> polylinePoints;
    private TripDatabaseHelper dbHelper;
    private ValueAnimator radarAnimator = null;
    private boolean isZooming = false;

    private FragmentReadyListener callback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof FragmentReadyListener) {
            callback = (FragmentReadyListener) context;
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        distView = view.findViewById(R.id.distView);
        fromName = view.findViewById(R.id.fromName);
        toName = view.findViewById(R.id.toName);
        busRoute = view.findViewById(R.id.bus_route);
        busRouteLoading = view.findViewById(R.id.bus_route_loading);

        boolean isDriver = App.isDriver();

        busRoute.setOnClickListener(v -> {
            isAutoMove = true;
            if (!isShowBusRute) {
                if (isDriver) {
                    ArrayList<LatLng> list = new ArrayList<>();
                    if (dbHelper != null) {
                        list = dbHelper.getAllLocationsAsLatLng();
                    }
                    showBusRute(list);
                } else {
                    ArrayList<TripLocation> list = new ArrayList<>();
                    if (dbHelper != null) {
                        list = dbHelper.getAllLocationsAsList();
                    }

                    callback.readLocationRoute(getLastTime(list));
                    busRoute.setEnabled(false);
                    busRoute.setClickable(false);
                    busRouteLoading.setVisibility(View.VISIBLE);
                }
            } else if (busInnerCircle != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busInnerCircle.getCenter(), 16.2f));
            }
        });

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;

                if (callback != null) callback.onLocationReady();

                viewCurrentPosition(true);

                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                mMap.getUiSettings().setZoomControlsEnabled(false);

                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                mMap.setOnCameraMoveListener(() -> {
                    if ((polylineInner != null && polylineOuter != null) || busInnerCircle != null) {
                        float zoom = mMap.getCameraPosition().zoom;
                        float currentZoom = (float) (Math.round(zoom * 100.0) / 100.0);

                        if (currentZoom != lastZoom) {
                            lastZoom = currentZoom;
                            updateMapZoom(zoom);
                        }
                    }
                });

                mMap.setOnCameraIdleListener(() -> {
                    isZooming = false;
                    if ((polylineInner != null && polylineOuter != null) || busInnerCircle != null) {
                        updateMapZoom(mMap.getCameraPosition().zoom);
                    }
                });

                mMap.setOnCameraMoveStartedListener(reason -> {
                    isZooming = true;
                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        isAutoMove = false;
                    }
                });

                mMap.setOnCircleClickListener(circle -> {
                    String name = (String) circle.getTag();
                    LatLng center = circle.getCenter();

                    if (startMarker != null) {
                        startMarker.remove();
                        startMarker = null;
                    }

                    if (endMarker != null) {
                        endMarker.remove();
                        endMarker = null;
                    }

                    startMarker = mMap.addMarker(new MarkerOptions()
                            .position(center)
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    if (startMarker != null) {
                        startMarker.showInfoWindow();
                    }
                });
            });
        }
    }

    private long getLastTime(ArrayList<TripLocation> list) {
        try {
            int size = list.size();
            if (size > 0) {
                TripLocation tripLocation = list.get(size-1);
                return tripLocation.time;
            }
        } catch (Exception e) {}

        return 0;
    }

    private void updateMapZoom(float zoom) {
        float baseWidthAt17 = 30.0f;
        float scalingFactor = 10.0f;

        float innerWidth = baseWidthAt17 + (zoom - 17f) * scalingFactor;

        float minimumWidth = 15.0f;

        if (innerWidth < minimumWidth) {
            innerWidth = minimumWidth;
        }

        float outerWidth = innerWidth + 6f;

        if (polylineInner != null && polylineOuter != null) {
            polylineInner.setWidth(innerWidth);
            polylineOuter.setWidth(outerWidth);
        }

        double circlePixelSize = innerWidth + 10f;

        if (innerCircle != null) {
            for (int i = 0; i < innerCircle.size(); i++) {
                Circle circle = innerCircle.get(i);
                if (circle != null) {
                    LatLng center = circle.getCenter();
                    double radius = getRadiusInMeters(mMap, center, (float)circlePixelSize);
                    circle.setRadius(radius);
                }
            }
        }

        if (busInnerCircle != null) {
            LatLng center = busInnerCircle.getCenter();
            float dynamicOuterAdd = (17f - zoom) * 10f;
            double outerPixelSize = circlePixelSize + 70f - dynamicOuterAdd;
            busInnerRadius = getRadiusInMeters(mMap, center, (float)circlePixelSize);
            busOuterRadius = getRadiusInMeters(mMap, center, (float)outerPixelSize);
            busInnerCircle.setRadius(busInnerRadius);
            busOuterCircle.setRadius(0);
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

            isShowBusRute = false;
            removeAllView();

            float mapZoom = 15;
            innerCircle = new ArrayList<>();
            outerCircle = new ArrayList<>();

            Iterator<String> keys = route.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject value = route.getJSONObject(key);
                    boolean isSelect = id != null && !id.isEmpty() && key.equals(id);
                    if (isSelect) {
                        mapZoom = (float) value.optDouble("z", 15);
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
                                        circle.setTag(endName);

                                        outerCircle.add(circle);

                                        circle = mMap.addCircle(new CircleOptions()
                                                .center(latLng)
                                                .radius(20)
                                                .strokeColor(Color.parseColor("#FF757575"))
                                                .fillColor(Color.WHITE)
                                                .strokeWidth(6)
                                                .clickable(true)
                                                .zIndex(2));
                                        circle.setTag(endName);

                                        innerCircle.add(circle);
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

                    if (startMarker != null) {
                        startMarker.showInfoWindow();
                    }

                    if (endMarker != null) {
                        endMarker.showInfoWindow();
                    }

                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, (int) zoom));
                }
            } else {
                startMarker = mMap.addMarker(new MarkerOptions()
                        .position(selected)
                        .title(selectedName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                if (startMarker != null) {
                    startMarker.showInfoWindow();
                }

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selected, mapZoom));
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
            for (int i = 0; i < innerCircle.size(); i++) {
                Circle circle = innerCircle.get(i);
                if (circle != null) {
                    circle.remove();
                }
            }
            innerCircle = null;
        }

        if (outerCircle != null) {
            for (int i = 0; i < outerCircle.size(); i++) {
                Circle circle = outerCircle.get(i);
                if (circle != null) {
                    circle.remove();
                }
            }
            outerCircle = null;
        }
    }

    public void viewCurrentPosition(boolean mark) {
        isAutoMove = mark;
        if (busInnerCircle != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busInnerCircle.getCenter(), 15f));
        } else if (mark) {
            LatLng bubtLocation = new LatLng(23.81189,90.35711);
            startMarker = mMap.addMarker(new MarkerOptions().position(bubtLocation).title("Bangladesh University of Business and Technology (BUBT)"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bubtLocation, 15f));
        }
    }

    public void updateBusRoute(ArrayList<LatLng> list) {
        if (busRouteLoading != null) {
            busRoute.setClickable(true);
            busRoute.setEnabled(true);
            busRouteLoading.setVisibility(View.GONE);
        }
        if (list != null) {
            if (list.isEmpty()) {
                Toast.makeText(requireContext(), "Error reading bus route data", Toast.LENGTH_SHORT).show();
            } else {
                showBusRute(list);
            }
        }
    }

    public void setBusLocation(LatLng newLocation, ArrayList<LatLng> list, boolean isDriver) {
        long currentTime = System.currentTimeMillis();

        if (previousBusLocation == null) {
            removeAllView();

            busRoute.setVisibility(View.VISIBLE);

            if (isDriver) {
                if (list == null) {
                    ArrayList<LatLng> listNew = new ArrayList<>();
                    listNew.add(newLocation);
                    showBusRute(listNew);
                } else {
                    showBusRute(list);
                }
            }

            busOuterCircle = mMap.addCircle(new CircleOptions()
                    .center(newLocation)
                    .radius(40)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(Color.parseColor("#350F53FE"))
                    .strokeWidth(0)
                    .zIndex(1000f)
            );

            busInnerCircle = mMap.addCircle(new CircleOptions()
                    .center(newLocation)
                    .radius(20)
                    .strokeColor(Color.WHITE)
                    .fillColor(Color.parseColor("#0F53FE"))
                    .strokeWidth(10)
                    .zIndex(1001f)
                    .clickable(true)
            );

            busInnerRadius = 20;
            busOuterRadius = 40;

            currentAnimatedLocation = newLocation;
            previousBusLocation = newLocation;
            lastUpdateTime = currentTime;


            updateMapZoom(15);
            startRadarAnimation();
            viewCurrentPosition(false);
        } else {
            long timeSinceLastUpdate = currentTime - lastUpdateTime;
            long duration = Math.max(2000, timeSinceLastUpdate);

            LatLng from = (currentAnimatedLocation != null) ? currentAnimatedLocation : previousBusLocation;

            animateBusMovement(from, newLocation, duration);

            previousBusLocation = newLocation;
            lastUpdateTime = currentTime;
        }
    }

    public void startBusTrip(TripDatabaseHelper dbHelper, boolean isReconnect) {
        this.dbHelper = dbHelper;

        if (busRoute != null) {
            busRoute.setVisibility(View.VISIBLE);
        }

        if (isReconnect && dbHelper != null) {
            ArrayList<LatLng> list = dbHelper.getAllLocationsAsLatLng();

            int size = list.size();
            if (size > 0) {
                setBusLocation(list.get(size-1), list, true);
            }
        }
    }

    public void stopBusTrip() {
        if (busRoute != null) {
            isShowBusRute = false;
            busRoute.setVisibility(View.GONE);
        }

        if (distView != null) {
            distView.setVisibility(View.GONE);
        }

        removeAllView();

        if (busOuterCircle != null) {
            busOuterCircle.remove();
            busOuterCircle = null;
        }

        if (busInnerCircle != null) {
            busInnerCircle.remove();
            busInnerCircle = null;
        }

        if (busAnimator != null && busAnimator.isRunning()) {
            busAnimator.cancel();
            busAnimator = null;
        }

        if (radarAnimator != null && radarAnimator.isRunning()) {
            radarAnimator.cancel();
            radarAnimator = null;
        }

        lastUpdateTime = 0;
        previousBusLocation = null;
        currentAnimatedLocation = null;
    }

    public void showBusRute(ArrayList<LatLng> lngArrayList) {
        removeAllView();
        isShowBusRute = true;

        polylinePoints = lngArrayList;
        innerCircle = new ArrayList<>();
        outerCircle = new ArrayList<>();

        int size = polylinePoints.size();
        if (size > 0 && currentAnimatedLocation != null) {
            polylinePoints.remove(size-1);
            polylinePoints.add(currentAnimatedLocation);
        }

        PolylineOptions outer = new PolylineOptions()
                .addAll(polylinePoints)
                .width(18)
                .color(Color.parseColor("#0A12D9"))
                .geodesic(true);
        polylineOuter = mMap.addPolyline(outer);

        PolylineOptions inner = new PolylineOptions()
                .addAll(polylinePoints)
                .width(12)
                .color(Color.parseColor("#0F53FE"))
                .geodesic(true);

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(polylinePoints.get(0))
                .radius(40)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(Color.parseColor("#20000000"))
                .strokeWidth(0)
                .zIndex(1));

        outerCircle.add(circle);

        circle = mMap.addCircle(new CircleOptions()
                .center(polylinePoints.get(0))
                .radius(20)
                .strokeColor(Color.parseColor("#FF757575"))
                .fillColor(Color.WHITE)
                .strokeWidth(6)
                .clickable(true)
                .zIndex(2));

        innerCircle.add(circle);

        polylineInner = mMap.addPolyline(inner);

        updateMapZoom(15);
        viewCurrentPosition(false);
    }

    private void animateBusMovement(LatLng from, LatLng to, long duration) {
        if (busAnimator != null && busAnimator.isRunning()) {
            busAnimator.cancel();
        }

        busAnimator = ValueAnimator.ofFloat(0, 1);
        busAnimator.setDuration(duration);
        busAnimator.setInterpolator(new LinearInterpolator());

        busAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            double lat = from.latitude + (to.latitude - from.latitude) * t;
            double lng = from.longitude + (to.longitude - from.longitude) * t;
            LatLng current = new LatLng(lat, lng);

            currentAnimatedLocation = current;

            if (busInnerCircle != null) busInnerCircle.setCenter(current);
            if (busOuterCircle != null) busOuterCircle.setCenter(current);

            if (isShowBusRute) {
                if (polylineInner != null) {
                    ArrayList<LatLng> tempInner = new ArrayList<>(polylinePoints);
                    tempInner.add(current);
                    polylineInner.setPoints(tempInner);
                }

                if (polylineOuter != null) {
                    ArrayList<LatLng> tempOuter = new ArrayList<>(polylinePoints);
                    tempOuter.add(current);
                    polylineOuter.setPoints(tempOuter);
                }
            }
        });

        busAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isShowBusRute) {
                    polylinePoints.add(to);
                }
                if (isAutoMove) {
                    float currentZoom = mMap.getCameraPosition().zoom;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(to, currentZoom));
                }
            }
        });

        busAnimator.start();
    }

    public void viewTripStart(TripDatabaseHelper dbHelper, String from, String to) {
        this.dbHelper = dbHelper;

        if (distView != null) {
            distView.setVisibility(View.VISIBLE);
            fromName.setText(from);
            toName.setText(to);
        }
    }

    private void startRadarAnimation() {
        if (busOuterCircle == null && busInnerCircle != null) return;

        if (radarAnimator != null && radarAnimator.isRunning()) return;

        final AtomicBoolean[] zoomStart = {new AtomicBoolean(false)};

        radarAnimator = ValueAnimator.ofFloat(0f, 1f);
        radarAnimator.setDuration(750);
        radarAnimator.setRepeatCount(ValueAnimator.INFINITE);
        radarAnimator.setRepeatMode(ValueAnimator.RESTART);

        radarAnimator.addUpdateListener(animation -> {
            if (isZooming) {
                zoomStart[0].set(true);
                return;
            };

            if (zoomStart[0].get()) {
                zoomStart[0].set(false);
                animation.setCurrentFraction(0f);
                return;
            }

            float fraction = (float) animation.getAnimatedValue();
            double newRadius = busInnerRadius + fraction * (busOuterRadius - busInnerRadius);
            busOuterCircle.setRadius(newRadius);
        });

        radarAnimator.start();
    }
}
