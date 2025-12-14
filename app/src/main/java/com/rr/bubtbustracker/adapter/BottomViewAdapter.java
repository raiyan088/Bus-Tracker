package com.rr.bubtbustracker.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.rr.bubtbustracker.R;

import java.util.List;

public class BottomViewAdapter extends BaseAdapter {

    private final Context context;
    private final int resName;
    private final int padding;
    private final List<String> listNames;

    public BottomViewAdapter(Context context, List<String> listNames, int resName, int padding) {
        this.context = context;
        this.resName = resName;
        this.padding = padding;
        this.listNames = listNames;
    }

    @Override
    public int getCount() {
        return listNames.size();
    }

    @Override
    public Object getItem(int position) {
        return listNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View _view, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_view_list_item, parent, false);


        ImageView logo = view.findViewById(R.id.logo);
        TextView name = view.findViewById(R.id.name);

        name.setText(listNames.get(position));
        logo.setPadding(padding, padding, padding, padding);
        logo.setImageResource(resName);

        return view;
    }
}
