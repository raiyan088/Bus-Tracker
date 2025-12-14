package com.rr.bubtbustracker.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.adapter.BottomViewAdapter;
import com.rr.bubtbustracker.interfaces.OnSelected;

import java.util.ArrayList;
import java.util.Arrays;

public class BusListBottomView {

    @SuppressLint("InflateParams")
    public BusListBottomView(Context context, ArrayList<String> list, TextView textView, OnSelected callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_list, null);
        ListView listView = view.findViewById(R.id.listViewOptions);

        if (list == null || list.isEmpty()) {
            list = new ArrayList<>(Arrays.asList( "Padma", "Meghna", "Jamuna", "Buriganga", "Brahmaputra" ));
        }

        BottomViewAdapter adapter = new BottomViewAdapter(context, list, R.drawable.ic_bus, 8);
        listView.setAdapter(adapter);

        ArrayList<String> finalList = list;
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selected = finalList.get(position);
            if (textView != null)  textView.setText(selected);
            dialog.dismiss();
            if (callback != null) callback.onSelected(selected);
        });

        adapter.notifyDataSetChanged();
        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }
        totalHeight += listView.getDividerHeight() * (adapter.getCount() - 1);

        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int maxHeight = screenHeight / 2;

        int finalHeight = Math.min(totalHeight, maxHeight);

        listView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, finalHeight));

        dialog.setContentView(view);

        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bottom_white_bg);
            }
        });

        dialog.show();
    }
}
