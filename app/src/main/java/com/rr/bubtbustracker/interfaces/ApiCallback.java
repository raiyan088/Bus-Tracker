package com.rr.bubtbustracker.interfaces;

import org.json.JSONObject;

public interface ApiCallback<T> {
    void onResult(T json);
}
