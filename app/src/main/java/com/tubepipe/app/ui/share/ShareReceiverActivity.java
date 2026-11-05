package com.tubepipe.app.ui.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tubepipe.app.R;
import com.tubepipe.app.data.downloader.DownloadManager;
import com.tubepipe.app.data.extractor.VideoExtractionCallback;
import com.tubepipe.app.data.extractor.VideoExtractorService;
import com.tubepipe.app.data.model.DownloadItem;
import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.data.model.VideoDetail;
import com.tubepipe.app.ui.adapter.QualityOptionAdapter;
import com.tubepipe.app.ui.player.PlayerActivity;
import com.tubepipe.app.util.AppUtils;
import com.tubepipe.app.util.PermissionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShareReceiverActivity extends AppCompatActivity {

    private TextView tvPlatform;
    private TextView tvSharedUrl;
    private View layoutExtracting;
    private TextView tvExtractionStatus;
    private View layoutExtractedContent;
    private ImageView ivThumb;
    private TextView tvVideoTitle;
    private TextView tvAuthor;
    private RecyclerView recyclerQualities;
    private MaterialButton btnStreamNow;
    private MaterialButton btnDownload;
    private ImageButton btnClose;

    private QualityOptionAdapter qualityAdapter;
    private StreamQuality selectedQuality;
    private VideoDetail extractedDetail;
    private String targetUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_receiver);

        initViews();
        handleShareIntent();
    }

    private void initViews() {
        tvPlatform = findViewById(R.id.tv_share_platform);
        tvSharedUrl = findViewById(R.id.tv_shared_url);
        layoutExtracting = findViewById(R.id.layout_extracting);
        tvExtractionStatus = findViewById(R.id.tv_extraction_status);
        layoutExtractedContent = findViewById(R.id.layout_extracted_content);
        ivThumb = findViewById(R.id.iv_share_thumb);
        tvVideoTitle = findViewById(R.id.tv_share_video_title);
        tvAuthor = findViewById(R.id.tv_share_author);
        recyclerQualities = findViewById(R.id.recycler_share_qualities);
        btnStreamNow = findViewById(R.id.btn_stream_now);
        btnDownload = findViewById(R.id.btn_share_download);
        btnClose = findViewById(R.id.btn_share_close);

        recyclerQualities.setLayoutManager(new LinearLayoutManager(this));
        qualityAdapter = new QualityOptionAdapter(quality -> selectedQuality = quality);
        recyclerQualities.setAdapter(qualityAdapter);

        btnClose.setOnClickListener(v -> finish());

        btnDownload.setOnClickListener(v -> onDownloadClicked());
        btnStreamNow.setOnClickListener(v -> onStreamClicked());
    }

    private void handleShareIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            targetUrl = AppUtils.extractUrl(sharedText);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                targetUrl = data.toString();
            }
        }

        if (targetUrl == null || targetUrl.isEmpty()) {
            Toast.makeText(this, "No valid video URL detected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String platform = AppUtils.detectPlatform(targetUrl);
        tvPlatform.setText("Download from " + platform);
        tvSharedUrl.setText(targetUrl);

        extractMediaInfo(targetUrl);
    }

    private void extractMediaInfo(String url) {
        layoutExtracting.setVisibility(View.VISIBLE);
        layoutExtractedContent.setVisibility(View.GONE);

        VideoExtractorService.getInstance().extractVideoInfo(url, new VideoExtractionCallback() {
            @Override
            public void onSuccess(VideoDetail detail) {
                extractedDetail = detail;
                tvVideoTitle.setText(detail.getTitle());
                tvAuthor.setText(detail.getUploaderName());

                Glide.with(ShareReceiverActivity.this)
                        .load(detail.getThumbnailUrl())
                        .placeholder(R.drawable.bg_thumbnail_placeholder)
                        .into(ivThumb);

                List<StreamQuality> options = new ArrayList<>();
                options.addAll(detail.getVideoStreams());
                options.addAll(detail.getAudioStreams());

                qualityAdapter.setQualities(options);
                if (!options.isEmpty()) {
                    selectedQuality = options.get(0);
                }

                layoutExtracting.setVisibility(View.GONE);
                layoutExtractedContent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Throwable throwable) {
                layoutExtracting.setVisibility(View.GONE);
                Toast.makeText(ShareReceiverActivity.this, "Extraction failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void onDownloadClicked() {
        if (selectedQuality == null) {
            Toast.makeText(this, "Select a format first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PermissionUtils.hasStoragePermission(this)) {
            PermissionUtils.requestStoragePermission(this);
            return;
        }

        String title = extractedDetail != null ? extractedDetail.getTitle() : "Shared Video";
        String thumb = extractedDetail != null ? extractedDetail.getThumbnailUrl() : "";
        String streamUrl = selectedQuality.getStreamUrl();

        DownloadItem downloadItem = new DownloadItem(
                UUID.randomUUID().toString(),
                targetUrl,
                title,
                thumb,
                targetUrl,
                streamUrl,
                selectedQuality.getResolution(),
                selectedQuality.isAudioOnly()
        );

        DownloadManager.getInstance(this).enqueueDownload(downloadItem);
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void onStreamClicked() {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, targetUrl);
        if (extractedDetail != null) {
            intent.putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, extractedDetail.getTitle());
        }
        startActivity(intent);
        finish();
    }
}
