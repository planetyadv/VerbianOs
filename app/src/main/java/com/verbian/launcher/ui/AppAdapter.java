package com.verbian.launcher.ui;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.verbian.launcher.R;
import com.verbian.launcher.model.AppModel;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(AppModel app);
        void onItemLongClick(AppModel app);
    }

    private final Context context;
    private final List<AppModel> items;
    private final OnItemClickListener listener;

    public AppAdapter(Context context, List<AppModel> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_card, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppModel app = items.get(position);
        holder.tvTitle.setText(app.getTitle());

        if (app.getIcon() != null) {
            holder.ivIcon.setImageDrawable(app.getIcon());
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_apps);
        }

        if (app.isWebShortcut()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(app);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(app);
            }
            return true;
        });

        // TV Remote Menu key handler (KEYCODE_MENU)
        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_MENU) {
                if (listener != null) {
                    listener.onItemLongClick(app);
                    return true;
                }
            }
            return false;
        });

        // Smooth hardware-accelerated TV focus animation (matches user screenshot highlight)
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ViewCompat.animate(v)
                        .scaleX(1.06f)
                        .scaleY(1.06f)
                        .setDuration(140)
                        .start();
                ViewCompat.setElevation(v, 16f);
            } else {
                ViewCompat.animate(v)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(140)
                        .start();
                ViewCompat.setElevation(v, 0f);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvBadge;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvBadge = itemView.findViewById(R.id.tv_badge);
        }
    }
}
