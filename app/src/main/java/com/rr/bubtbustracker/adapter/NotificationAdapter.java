package com.rr.bubtbustracker.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.interfaces.OnItemClickListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final JSONArray mList;
    private OnItemClickListener mClickListener;

    public NotificationAdapter(JSONArray list) {
        mList = list;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        try {
            JSONObject object = mList.getJSONObject(position);
            holder.title.setText(object.optString("title", ""));
            holder.body.setText(object.optString("body", ""));
            holder.ago.setText(getTimeAgo(object.optLong("time", System.currentTimeMillis())));
        } catch (Exception ignored) {}
    }


    @Override
    public int getItemCount() {
        return mList.length();
    }


    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mClickListener = listener;
    }

    public static String getTimeAgo(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        long diffMinutes = diff / (1000 * 60);
        long diffHours   = diff / (1000 * 60 * 60);
        long diffDays    = diff / (1000 * 60 * 60 * 24);

        String dot = " \u2022 ";

        if (diffMinutes == 0) {
            return dot+"now";
        } else if (diffMinutes < 60) {
            return dot+diffMinutes + "m ago";
        } else if (diffHours < 24) {
            return dot+diffHours + "m ago";
        } else {
            return dot+diffDays + "m ago";
        }
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {

        public TextView title, body, ago;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            body = itemView.findViewById(R.id.body);
            ago = itemView.findViewById(R.id.ago);

            itemView.setOnClickListener(v -> {
                if (mClickListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        mClickListener.onItemClick(pos);
                    }
                }
            });
        }
    }
}
