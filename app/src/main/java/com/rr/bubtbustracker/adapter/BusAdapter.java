package com.rr.bubtbustracker.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.rr.bubtbustracker.R;

import java.util.List;

public class BusAdapter extends BaseAdapter {

    private Context context;
    private List<String> busNames;

    public BusAdapter(Context context, List<String> busNames) {
        this.context = context;
        this.busNames = busNames;
    }

    @Override
    public int getCount() {
        return busNames.size();
    }

    @Override
    public Object getItem(int position) {
        return busNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_bus, parent, false);
        }

        TextView txtBusName = convertView.findViewById(R.id.txtBusName);

        txtBusName.setText(busNames.get(position));

        return convertView;
    }
}
