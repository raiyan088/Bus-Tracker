package com.rr.bubtbustracker.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.rr.bubtbustracker.App;
import com.rr.bubtbustracker.R;

public class LocationFragment extends Fragment {

    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
//
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(googleMap -> {
//                mMap = googleMap;
//
//                LatLng bubtLocation = new LatLng(23.81189,90.35711);
//                mMap.addMarker(new MarkerOptions().position(bubtLocation).title("Bangladesh University of Business and Technology (BUBT)"));
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bubtLocation, 15));
//
//                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//                mMap.getUiSettings().setZoomControlsEnabled(true);
//            });
//        }
    }
}
