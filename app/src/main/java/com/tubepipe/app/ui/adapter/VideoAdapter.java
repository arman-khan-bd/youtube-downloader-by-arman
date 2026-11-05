package com.tubepipe.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.tubepipe.app.R;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video);
        void onQuickDownloadClick(VideoItem video);
    }

    private final List<VideoItem> videos = new ArrayList<>();
    private final OnVideoClickListener listener;
    private boolean isLoadingFooter = false;

    public VideoAdapter(OnVideoClickListener listener) {
        this.listener = listener;
    }

    public void setVideos(List<VideoItem> newVideos) {
        videos.clear();
        if (newVideos != null) {
            videos.addAll(newVideos);
        }
        isLoadingFooter = false;
        notifyDataSetChanged();
    }

    public void addVideos(List<VideoItem> newVideos) {
        if (newVideos != null && !newVideos.isEmpty()) {
            int start = videos.size();
            videos.addAll(newVideos);
            notifyItemRangeInserted(start, newVideos.size());
        }
    }

    public void setLoadingFooter(boolean loading) {
        if (this.isLoadingFooter != loading) {
            this.isLoadingFooter = loading;
            if (loading) {
                notifyItemInserted(videos.size());
            } else {
                notifyItemRemoved(videos.size());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooter && position == videos.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading_footer, parent, false);
            return new LoadingViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_card, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VideoViewHolder) {
            VideoItem video = videos.get(position);
            ((VideoViewHolder) holder).bind(video, listener);
        }
    }

    @Override
    public int getItemCount() {
        return videos.size() + (isLoadingFooter ? 1 : 0);
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvDuration;
        private final ImageView ivAvatar;
        private final TextView tvTitle;
        private final TextView tvMetadata;
        private final ImageButton btnQuickDownload;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            ivAvatar = itemView.findViewById(R.id.iv_channel_avatar);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMetadata = itemView.findViewById(R.id.tv_metadata);
            btnQuickDownload = itemView.findViewById(R.id.btn_quick_download);
        }

        public void bind(VideoItem video, OnVideoClickListener listener) {
            tvTitle.setText(video.getTitle());

            StringBuilder meta = new StringBuilder();
            if (!video.getUploaderName().isEmpty()) {
                meta.append(video.getUploaderName());
            }
            if (video.getViewCount() > 0) {
                if (meta.length() > 0) meta.append(" • ");
                meta.append(AppUtils.formatViews(video.getViewCount())).append(" views");
            }
            if (!video.getUploadDate().isEmpty()) {
                if (meta.length() > 0) meta.append(" • ");
                meta.append(video.getUploadDate());
            }
            tvMetadata.setText(meta.toString());

            if (video.getDurationSeconds() > 0) {
                tvDuration.setVisibility(View.VISIBLE);
                tvDuration.setText(AppUtils.formatDuration(video.getDurationSeconds()));
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            Glide.with(itemView.getContext())
                    .load(video.getThumbnailUrl())
                    .placeholder(R.drawable.bg_thumbnail_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivThumbnail);

            if (!video.getUploaderAvatarUrl().isEmpty()) {
                ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(video.getUploaderAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.bg_thumbnail_placeholder)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_logo);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onVideoClick(video);
            });

            btnQuickDownload.setOnClickListener(v -> {
                if (listener != null) listener.onQuickDownloadClick(video);
            });
        }
    }
}
