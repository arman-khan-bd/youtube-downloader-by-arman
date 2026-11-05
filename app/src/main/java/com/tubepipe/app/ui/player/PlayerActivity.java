package com.tubepipe.app.ui.player;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tubepipe.app.R;
import com.tubepipe.app.data.extractor.VideoExtractionCallback;
import com.tubepipe.app.data.extractor.VideoExtractorService;
import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.data.model.VideoDetail;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.ui.adapter.VideoAdapter;
import com.tubepipe.app.ui.dialog.DownloadDialog;
import com.tubepipe.app.util.AppUtils;

import java.io.File;

public class PlayerActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    public static final String EXTRA_VIDEO_URL = "extra_video_url";
    public static final String EXTRA_VIDEO_TITLE = "extra_video_title";
    public static final String EXTRA_OFFLINE_PATH = "extra_offline_path";

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar playerLoading;

    private TextView tvTitle;
    private TextView tvMetadata;
    private ImageView ivChannelAvatar;
    private TextView tvChannelName;
    private TextView tvSubscribers;
    private MaterialButton btnSubscribe;
    private View btnDownload;
    private View btnShare;
    private View btnBgAudio;
    private RecyclerView recyclerRelated;
    private VideoAdapter relatedAdapter;

    private String videoUrl;
    private String offlinePath;
    private String currentPlayingStreamUri;
    private VideoDetail currentVideoDetail;
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        handleIntent();
    }

    private void initViews() {
        playerView = findViewById(R.id.player_view);
        playerLoading = findViewById(R.id.player_loading);
        tvTitle = findViewById(R.id.tv_player_title);
        tvMetadata = findViewById(R.id.tv_player_metadata);
        ivChannelAvatar = findViewById(R.id.iv_player_channel_avatar);
        tvChannelName = findViewById(R.id.tv_player_channel_name);
        tvSubscribers = findViewById(R.id.tv_player_subscribers);
        btnSubscribe = findViewById(R.id.btn_subscribe);
        btnDownload = findViewById(R.id.btn_player_download);
        btnShare = findViewById(R.id.btn_player_share);
        btnBgAudio = findViewById(R.id.btn_player_bg_audio);
        recyclerRelated = findViewById(R.id.recycler_related_videos);

        recyclerRelated.setLayoutManager(new LinearLayoutManager(this));
        relatedAdapter = new VideoAdapter(this);
        recyclerRelated.setAdapter(relatedAdapter);

        btnBgAudio.setOnClickListener(v -> startBackgroundPlayback());

        btnDownload.setOnClickListener(v -> {
            if (videoUrl != null && !videoUrl.isEmpty()) {
                DownloadDialog.newInstance(videoUrl).show(getSupportFragmentManager(), "player_download");
            }
        });

        btnShare.setOnClickListener(v -> {
            if (videoUrl != null && !videoUrl.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, videoUrl);
                startActivity(Intent.createChooser(shareIntent, "Share Video"));
            }
        });

        btnSubscribe.setOnClickListener(v -> {
            boolean isSub = getString(R.string.subscribed_button).equals(btnSubscribe.getText().toString());
            btnSubscribe.setText(isSub ? R.string.subscribe_button : R.string.subscribed_button);
            btnSubscribe.setBackgroundTintList(getColorStateList(isSub ? R.color.white : R.color.yt_chip_dark));
            btnSubscribe.setTextColor(getColor(isSub ? R.color.black : R.color.yt_text_primary));
        });

        // Setup custom fullscreen toggle in player controller
        ImageButton btnFullscreenControl = playerView.findViewById(R.id.btn_fullscreen);
        if (btnFullscreenControl != null) {
            btnFullscreenControl.setOnClickListener(v -> toggleFullscreen());
        }
    }

    private void handleIntent() {
        videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        offlinePath = getIntent().getStringExtra(EXTRA_OFFLINE_PATH);
        String initialTitle = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);

        if (initialTitle != null) {
            tvTitle.setText(initialTitle);
        }

        if (offlinePath != null) {
            playLocalFile(offlinePath);
        } else if (videoUrl != null) {
            extractAndPlay(videoUrl);
        } else {
            Toast.makeText(this, "No video provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void extractAndPlay(String url) {
        playerLoading.setVisibility(View.VISIBLE);

        VideoExtractorService.getInstance().extractVideoInfo(url, new VideoExtractionCallback() {
            @Override
            public void onSuccess(VideoDetail detail) {
                currentVideoDetail = detail;
                tvTitle.setText(detail.getTitle());

                StringBuilder meta = new StringBuilder();
                if (detail.getViewCount() > 0) {
                    meta.append(AppUtils.formatViews(detail.getViewCount())).append(" views");
                }
                if (!detail.getUploadDate().isEmpty()) {
                    if (meta.length() > 0) meta.append(" • ");
                    meta.append(detail.getUploadDate());
                }
                tvMetadata.setText(meta.toString());

                tvChannelName.setText(detail.getUploaderName());
                tvSubscribers.setText(detail.getUploaderSubscriberCount());

                Glide.with(PlayerActivity.this)
                        .load(detail.getUploaderAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.bg_thumbnail_placeholder)
                        .into(ivChannelAvatar);

                relatedAdapter.setVideos(detail.getRelatedVideos());

                // Find best playback stream
                String streamUrl = null;
                for (StreamQuality quality : detail.getVideoStreams()) {
                    if (quality.getStreamUrl() != null && !quality.getStreamUrl().isEmpty()) {
                        streamUrl = quality.getStreamUrl();
                        break;
                    }
                }

                if (streamUrl != null) {
                    initializePlayer(Uri.parse(streamUrl));
                } else {
                    playerLoading.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "No playable stream found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                playerLoading.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Extraction failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void playLocalFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            tvMetadata.setText("Offline Playback • " + AppUtils.formatFileSize(file.length()));
            tvChannelName.setText("Local Download");
            btnDownload.setVisibility(View.GONE);
            initializePlayer(Uri.fromFile(file));
        } else {
            Toast.makeText(this, "Local file not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializePlayer(Uri mediaUri) {
        currentPlayingStreamUri = mediaUri.toString();
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
        }

        MediaItem mediaItem = MediaItem.fromUri(mediaUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        playerLoading.setVisibility(View.GONE);
    }

    private void startBackgroundPlayback() {
        if (currentPlayingStreamUri == null && videoUrl == null && offlinePath == null) {
            Toast.makeText(this, "Video not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        long pos = player != null ? player.getCurrentPosition() : 0;
        if (player != null) {
            player.pause();
        }

        String title = currentVideoDetail != null ? currentVideoDetail.getTitle() : tvTitle.getText().toString();
        String artist = currentVideoDetail != null ? currentVideoDetail.getUploaderName() : tvChannelName.getText().toString();
        String thumb = currentVideoDetail != null ? currentVideoDetail.getThumbnailUrl() : "";
        String playUri = offlinePath != null ? offlinePath : (currentPlayingStreamUri != null ? currentPlayingStreamUri : videoUrl);

        Intent serviceIntent = new Intent(this, com.tubepipe.app.data.player.BackgroundAudioService.class);
        serviceIntent.setAction(com.tubepipe.app.data.player.BackgroundAudioService.ACTION_PLAY_ITEM);
        serviceIntent.putExtra(com.tubepipe.app.data.player.BackgroundAudioService.EXTRA_URI, playUri);
        serviceIntent.putExtra(com.tubepipe.app.data.player.BackgroundAudioService.EXTRA_TITLE, title);
        serviceIntent.putExtra(com.tubepipe.app.data.player.BackgroundAudioService.EXTRA_ARTIST, artist);
        serviceIntent.putExtra(com.tubepipe.app.data.player.BackgroundAudioService.EXTRA_ART_URL, thumb);
        serviceIntent.putExtra(com.tubepipe.app.data.player.BackgroundAudioService.EXTRA_START_POSITION, pos);
        startService(serviceIntent);

        Toast.makeText(this, "Playing in background (swipe down for lock screen controls)", Toast.LENGTH_SHORT).show();
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            findViewById(R.id.player_scroll_view).setVisibility(View.VISIBLE);
            isFullscreen = false;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            findViewById(R.id.player_scroll_view).setVisibility(View.GONE);
            isFullscreen = true;
        }
    }

    @Override
    public void onVideoClick(VideoItem video) {
        if (player != null) {
            player.stop();
        }
        videoUrl = video.getUrl();
        tvTitle.setText(video.getTitle());
        extractAndPlay(videoUrl);
    }

    @Override
    public void onQuickDownloadClick(VideoItem video) {
        DownloadDialog.newInstance(video).show(getSupportFragmentManager(), "related_download");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
