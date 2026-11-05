package com.tubepipe.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tubepipe.app.R;
import com.tubepipe.app.data.model.DownloadItem;

import java.util.ArrayList;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

    public interface OnDownloadActionListener {
        void onPlayOffline(DownloadItem item);
        void onDeleteDownload(DownloadItem item);
    }

    private final List<DownloadItem> items = new ArrayList<>();
    private final OnDownloadActionListener listener;

    public DownloadAdapter(OnDownloadActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<DownloadItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void updateItem(DownloadItem updated) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(updated.getId())) {
                items.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumb;
        private final TextView tvTitle;
        private final TextView tvQuality;
        private final ProgressBar progressBar;
        private final TextView tvStatus;
        private final MaterialButton btnPlay;
        private final ImageButton btnDelete;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_download_thumb);
            tvTitle = itemView.findViewById(R.id.tv_download_title);
            tvQuality = itemView.findViewById(R.id.tv_download_quality);
            progressBar = itemView.findViewById(R.id.download_progress_bar);
            tvStatus = itemView.findViewById(R.id.tv_download_status);
            btnPlay = itemView.findViewById(R.id.btn_play_offline);
            btnDelete = itemView.findViewById(R.id.btn_action_delete);
        }

        public void bind(DownloadItem item, OnDownloadActionListener listener) {
            tvTitle.setText(item.getTitle());
            tvQuality.setText(item.getQualityLabel() + (item.isAudioOnly() ? " • Audio" : " • Video"));

            Glide.with(itemView.getContext())
                    .load(item.getThumbnailUrl())
                    .placeholder(R.drawable.bg_thumbnail_placeholder)
                    .into(ivThumb);

            switch (item.getStatus()) {
                case DOWNLOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(item.getProgress());
                    tvStatus.setText("Downloading: " + item.getProgress() + "% (" + item.getDownloadSpeed() + ")");
                    btnPlay.setVisibility(View.GONE);
                    break;
                case COMPLETED:
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Downloaded successfully");
                    btnPlay.setVisibility(View.VISIBLE);
                    break;
                case FAILED:
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Failed: " + (item.getErrorMessage().isEmpty() ? "Unknown error" : item.getErrorMessage()));
                    btnPlay.setVisibility(View.GONE);
                    break;
                case QUEUED:
                default:
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                    tvStatus.setText("Queued in download manager…");
                    btnPlay.setVisibility(View.GONE);
                    break;
            }

            btnPlay.setOnClickListener(v -> {
                if (listener != null) listener.onPlayOffline(item);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteDownload(item);
            });
        }
    }
}
