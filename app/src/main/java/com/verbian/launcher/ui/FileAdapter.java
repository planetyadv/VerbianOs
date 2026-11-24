package com.verbian.launcher.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.verbian.launcher.R;
import com.verbian.launcher.model.FileModel;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(FileModel file);
        void onFileLongClick(FileModel file);
    }

    private final Context context;
    private final List<FileModel> files;
    private final OnFileClickListener listener;

    public FileAdapter(Context context, List<FileModel> files, OnFileClickListener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileModel item = files.get(position);
        holder.tvName.setText(item.getName());
        holder.tvSize.setText(item.getSizeFormatted());

        if (item.isDirectory()) {
            holder.ivIcon.setImageResource(R.drawable.ic_folder);
            holder.tvAction.setText("OPEN");
        } else if (item.isApk()) {
            holder.ivIcon.setImageResource(R.drawable.ic_apk);
            holder.tvAction.setText("INSTALL APK");
        } else if (item.isMedia()) {
            holder.ivIcon.setImageResource(R.drawable.ic_video);
            holder.tvAction.setText("PLAY");
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_file);
            holder.tvAction.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFileClick(item);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onFileLongClick(item);
            return true;
        });

        // Smooth TV D-Pad Focus animation
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ViewCompat.animate(v).scaleX(1.02f).scaleY(1.02f).setDuration(120).start();
                ViewCompat.setElevation(v, 8f);
            } else {
                ViewCompat.animate(v).scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                ViewCompat.setElevation(v, 0f);
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvName;
        final TextView tvSize;
        final TextView tvAction;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            tvName = itemView.findViewById(R.id.tv_file_name);
            tvSize = itemView.findViewById(R.id.tv_file_size);
            tvAction = itemView.findViewById(R.id.tv_file_action);
        }
    }
}
