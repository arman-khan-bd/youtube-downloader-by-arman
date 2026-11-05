package com.tubepipe.app.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.tubepipe.app.R;
import com.tubepipe.app.data.downloader.DownloadManager;
import com.tubepipe.app.data.extractor.VideoExtractionCallback;
import com.tubepipe.app.data.extractor.VideoExtractorService;
import com.tubepipe.app.data.model.DownloadItem;
import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.data.model.VideoDetail;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.data.settings.SettingsManager;
import com.tubepipe.app.ui.adapter.QualityOptionAdapter;
import com.tubepipe.app.util.AppUtils;
import com.tubepipe.app.util.PermissionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DownloadDialog extends BottomSheetDialogFragment {

    private static final String ARG_VIDEO_ITEM = "arg_video_item";
    private static final String ARG_VIDEO_URL = "arg_video_url";

    private VideoItem videoItem;
    private String videoUrl;
    private VideoDetail videoDetail;

    private ImageView ivThumb;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private RecyclerView recyclerQualities;
    private TextView tvStorageInfo;
    private MaterialButton btnDownload;
    private MaterialButton btnCancel;

    private QualityOptionAdapter adapter;
    private StreamQuality selectedQuality;
    private final List<StreamQuality> videoQualities = new ArrayList<>();
    private final List<StreamQuality> audioQualities = new ArrayList<>();

    public static DownloadDialog newInstance(VideoItem videoItem) {
        DownloadDialog dialog = new DownloadDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_VIDEO_ITEM, videoItem);
        dialog.setArguments(args);
        return dialog;
    }

    public static DownloadDialog newInstance(String url) {
        DownloadDialog dialog = new DownloadDialog();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_URL, url);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_TubePipe_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_download_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivThumb = view.findViewById(R.id.iv_dialog_thumb);
        tvTitle = view.findViewById(R.id.tv_dialog_title);
        tvSubtitle = view.findViewById(R.id.tv_dialog_subtitle);
        tabLayout = view.findViewById(R.id.tab_download_type);
        progressBar = view.findViewById(R.id.dialog_loading);
        recyclerQualities = view.findViewById(R.id.recycler_qualities);
        tvStorageInfo = view.findViewById(R.id.tv_dialog_storage_info);
        btnDownload = view.findViewById(R.id.btn_dialog_download);
        btnCancel = view.findViewById(R.id.btn_dialog_cancel);

        if (getArguments() != null) {
            videoItem = (VideoItem) getArguments().getSerializable(ARG_VIDEO_ITEM);
            videoUrl = getArguments().getString(ARG_VIDEO_URL);
            if (videoItem != null) {
                videoUrl = videoItem.getUrl();
                tvTitle.setText(videoItem.getTitle());
                tvSubtitle.setText(videoItem.getUploaderName() + (videoItem.getDurationSeconds() > 0 ? " • " + AppUtils.formatDuration(videoItem.getDurationSeconds()) : ""));
                Glide.with(this).load(videoItem.getThumbnailUrl()).placeholder(R.drawable.bg_thumbnail_placeholder).into(ivThumb);
            }
        }

        displayStorageInfo();

        recyclerQualities.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QualityOptionAdapter(quality -> {
            selectedQuality = quality;
            updateDownloadButtonText();
        });
        recyclerQualities.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showVideoQualities();
                } else {
                    showAudioQualities();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnCancel.setOnClickListener(v -> dismiss());
        btnDownload.setOnClickListener(v -> handleDownloadClick());

        loadStreams();
    }

    private void displayStorageInfo() {
        if (getContext() == null) return;
        SettingsManager sm = SettingsManager.getInstance(getContext());
        File targetDir = sm.getEffectiveDownloadDirectory();
        boolean isSd = sm.getStorageLocation() == SettingsManager.StorageLocation.SD_CARD;
        long free = isSd ? sm.getSdCardFreeSpaceBytes() : sm.getInternalFreeSpaceBytes();

        String label = (isSd ? "SD Card" : "Phone Storage") + " (" + targetDir.getName() + ") • " + AppUtils.formatFileSize(free) + " free";
        tvStorageInfo.setText("Saving to: " + label);
    }

    private void loadStreams() {
        if (videoUrl == null || videoUrl.isEmpty()) {
            dismiss();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerQualities.setVisibility(View.GONE);
        btnDownload.setEnabled(false);

        VideoExtractorService.getInstance().extractVideoInfo(videoUrl, new VideoExtractionCallback() {
            @Override
            public void onSuccess(VideoDetail detail) {
                if (!isAdded()) return;
                videoDetail = detail;
                tvTitle.setText(detail.getTitle());
                tvSubtitle.setText(detail.getUploaderName() + (detail.getDurationSeconds() > 0 ? " • " + AppUtils.formatDuration(detail.getDurationSeconds()) : ""));

                Glide.with(DownloadDialog.this)
                        .load(detail.getThumbnailUrl())
                        .placeholder(R.drawable.bg_thumbnail_placeholder)
                        .into(ivThumb);

                videoQualities.clear();
                videoQualities.addAll(detail.getVideoStreams());

                audioQualities.clear();
                audioQualities.addAll(detail.getAudioStreams());

                // If audio list empty, provide synthesized MP3/M4A choices based on video
                if (audioQualities.isEmpty() && !videoQualities.isEmpty()) {
                    long duration = detail.getDurationSeconds();
                    audioQualities.add(new StreamQuality("audio_hq", "High Quality MP3 (320 kbps)", "mp3", true, detail.getWebUrl(), duration * 320 * 1000 / 8, 320));
                    audioQualities.add(new StreamQuality("audio_mq", "Standard MP3 (128 kbps)", "mp3", true, detail.getWebUrl(), duration * 128 * 1000 / 8, 128));
                }

                progressBar.setVisibility(View.GONE);
                recyclerQualities.setVisibility(View.VISIBLE);

                if (tabLayout.getSelectedTabPosition() == 1) {
                    showAudioQualities();
                } else {
                    showVideoQualities();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to extract streams: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private void showVideoQualities() {
        adapter.setQualities(videoQualities);
        selectedQuality = adapter.getSelectedQuality();
        btnDownload.setEnabled(selectedQuality != null);
        updateDownloadButtonText();
    }

    private void showAudioQualities() {
        adapter.setQualities(audioQualities);
        selectedQuality = adapter.getSelectedQuality();
        btnDownload.setEnabled(selectedQuality != null);
        updateDownloadButtonText();
    }

    private void updateDownloadButtonText() {
        if (selectedQuality != null) {
            String sizeStr = selectedQuality.getApproxSizeBytes() > 0 ? " (" + AppUtils.formatFileSize(selectedQuality.getApproxSizeBytes()) + ")" : "";
            btnDownload.setText("Download " + (selectedQuality.isAudioOnly() ? "Audio" : selectedQuality.getResolution()) + sizeStr);
            btnDownload.setEnabled(true);
        } else {
            btnDownload.setText(R.string.download);
        }
    }

    private void handleDownloadClick() {
        if (selectedQuality == null) {
            Toast.makeText(getContext(), "Please select a quality", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() != null && !PermissionUtils.hasStoragePermission(getActivity())) {
            PermissionUtils.requestStoragePermission(getActivity());
            return;
        }

        String title = videoDetail != null ? videoDetail.getTitle() : (videoItem != null ? videoItem.getTitle() : "Video");
        String thumb = videoDetail != null ? videoDetail.getThumbnailUrl() : (videoItem != null ? videoItem.getThumbnailUrl() : "");
        String streamUrl = selectedQuality.getStreamUrl();

        DownloadItem downloadItem = new DownloadItem(
                UUID.randomUUID().toString(),
                videoUrl,
                title,
                thumb,
                videoUrl,
                streamUrl,
                selectedQuality.getResolution(),
                selectedQuality.isAudioOnly()
        );

        DownloadManager.getInstance(requireContext()).enqueueDownload(downloadItem);
        Toast.makeText(getContext(), R.string.download_started, Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
