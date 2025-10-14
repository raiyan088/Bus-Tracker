package com.rr.bubtbustracker.interfaces;

import org.json.JSONObject;

public interface OnBusClickListener {
    void onSelected(JSONObject route, String id, float zoom);
}
