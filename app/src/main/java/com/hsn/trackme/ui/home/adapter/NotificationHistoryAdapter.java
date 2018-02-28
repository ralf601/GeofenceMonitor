package com.hsn.trackme.ui.home.adapter;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hsn.trackme.R;
import com.hsn.trackme.model.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class NotificationHistoryAdapter extends RecyclerView.Adapter<NotificationHistoryAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onClick(Notification notification);
    }


    private List<Notification> notifications = new ArrayList<>();
    private OnItemClickListener listener;

    public void update(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    @Override
    public NotificationHistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationHistoryAdapter.ViewHolder holder, int position) {
        final Notification notification = notifications.get(position);
        holder.bind(notifications.get(position));
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(listener!=null)
                    listener.onClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public View root;
        public TextView notificationText;
        public TextView dateTime;


        public ViewHolder(View root) {
            super(root);
            this.root = root;
            this.notificationText =  root.findViewById(R.id.notificationText);
            this.dateTime =  root.findViewById(R.id.dateTime);

        }

        public void bind(Notification notification) {
            notificationText.setText(notification.getMessage());
            dateTime.setText(notification.getDateTime());
        }
    }

}
