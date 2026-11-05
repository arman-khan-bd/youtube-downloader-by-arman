package com.tubepipe.app.ui.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
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

    // Gesture Overlay Views
    private View layoutRewindOverlay;
    private View layoutForwardOverlay;
    private View layoutGestureIndicator;
    private ImageView ivGestureIcon;
    private TextView tvGestureText;
    private final Handler gestureHandler = new Handler(Looper.getMainLooper());

    private AudioManager audioManager;
    private int maxVolume = 15;

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

        layoutRewindOverlay = findViewById(R.id.layout_rewind_overlay);
        layoutForwardOverlay = findViewById(R.id.layout_forward_overlay);
        layoutGestureIndicator = findViewById(R.id.layout_gesture_indicator);
        ivGestureIcon = findViewById(R.id.iv_gesture_icon);
        tvGestureText = findViewById(R.id.tv_gesture_text);

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

        // Setup minimize button in player controller
        View btnPlayerMinimize = playerView.findViewById(R.id.btn_player_minimize);
        if (btnPlayerMinimize != null) {
            btnPlayerMinimize.setOnClickListener(v -> minimizeToMiniPlayer());
        }

        setupPlayerGestures();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPlayerGestures() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (playerView != null) {
                    if (playerView.isControllerFullyVisible()) {
                        playerView.hideController();
                    } else {
                        playerView.showController();
                    }
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (playerView == null) return false;
                float width = playerView.getWidth();
                if (e.getX() < width / 2f) {
                    seekRewind10();
                } else {
                    seekForward10();
                }
                return true;
            }

            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null) {
                    float deltaY = e2.getY() - e1.getY();
                    float deltaX = e2.getX() - e1.getX();
                    // Swipe down gesture to mini-player: vertical swipe down
                    if (deltaY > 100 && Math.abs(deltaY) > Math.abs(deltaX) * 1.3f && velocityY > 200) {
                        minimizeToMiniPlayer();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || playerView == null) return false;
                float totalDeltaY = e1.getY() - e2.getY(); // positive when dragging up
                float totalDeltaX = e2.getX() - e1.getX();

                if (Math.abs(totalDeltaY) > Math.abs(totalDeltaX)) {
                    // Check if dragging down towards mini-player
                    if (-totalDeltaY > 160 && e1.getY() < playerView.getHeight() * 0.7f) {
                        minimizeToMiniPlayer();
                        return true;
                    }

                    float height = playerView.getHeight();
                    if (height > 0) {
                        float deltaPercent = (distanceY / height) * 0.6f;
                        if (e1.getX() < playerView.getWidth() / 2f) {
                            adjustBrightness(deltaPercent);
                        } else {
                            adjustVolume(deltaPercent);
                        }
                    }
                    return true;
                }
                return false;
            }
        };

        GestureDetector detector = new GestureDetector(this, gestureListener);
        playerView.setOnTouchListener((v, event) -> {
            boolean handled = detector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                gestureHandler.postDelayed(hideGestureIndicatorRunnable, 600);
            }
            return handled;
        });
    }

    private final Runnable hideForwardOverlayRunnable = () -> {
        if (layoutForwardOverlay != null) layoutForwardOverlay.setVisibility(View.GONE);
    };

    private final Runnable hideRewindOverlayRunnable = () -> {
        if (layoutRewindOverlay != null) layoutRewindOverlay.setVisibility(View.GONE);
    };

    private final Runnable hideGestureIndicatorRunnable = () -> {
        if (layoutGestureIndicator != null) layoutGestureIndicator.setVisibility(View.GONE);
    };

    private void seekForward10() {
        if (player == null) return;
        long current = player.getCurrentPosition();
        long dur = player.getDuration();
        long target = dur > 0 ? Math.min(dur, current + 10000) : current + 10000;
        player.seekTo(target);

        if (layoutForwardOverlay != null) {
            layoutForwardOverlay.setVisibility(View.VISIBLE);
            gestureHandler.removeCallbacks(hideForwardOverlayRunnable);
            gestureHandler.postDelayed(hideForwardOverlayRunnable, 600);
        }
    }

    private void seekRewind10() {
        if (player == null) return;
        long current = player.getCurrentPosition();
        long target = Math.max(0, current - 10000);
        player.seekTo(target);

        if (layoutRewindOverlay != null) {
            layoutRewindOverlay.setVisibility(View.VISIBLE);
            gestureHandler.removeCallbacks(hideRewindOverlayRunnable);
            gestureHandler.postDelayed(hideRewindOverlayRunnable, 600);
        }
    }

    private void adjustBrightness(float deltaPercent) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness;
        if (current < 0) current = 0.5f;
        float newBrightness = Math.max(0.01f, Math.min(1.0f, current + deltaPercent));
        lp.screenBrightness = newBrightness;
        getWindow().setAttributes(lp);

        int percent = Math.round(newBrightness * 100);
        showGestureIndicator(R.drawable.ic_brightness, "Brightness: " + percent + "%");
    }

    private void adjustVolume(float deltaPercent) {
        if (audioManager == null) return;
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int step = Math.round(deltaPercent * maxVolume);
        if (step == 0) step = deltaPercent > 0 ? 1 : -1;
        int newVolume = Math.max(0, Math.min(maxVolume, current + step));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);

        int percent = maxVolume > 0 ? Math.round((newVolume / (float) maxVolume) * 100) : 0;
        showGestureIndicator(R.drawable.ic_volume, "Volume: " + percent + "%");
    }

    private void showGestureIndicator(int iconRes, String text) {
        if (layoutGestureIndicator == null) return;
        ivGestureIcon.setImageResource(iconRes);
        tvGestureText.setText(text);
        layoutGestureIndicator.setVisibility(View.VISIBLE);
        gestureHandler.removeCallbacks(hideGestureIndicatorRunnable);
        gestureHandler.postDelayed(hideGestureIndicatorRunnable, 800);
    }

    private void minimizeToMiniPlayer() {
        if (isFullscreen) {
            toggleFullscreen();
            return;
        }
        startBackgroundPlayback();
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen();
        } else {
            minimizeToMiniPlayer();
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
            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(20000);

            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

            player = new ExoPlayer.Builder(this)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        playerLoading.setVisibility(View.VISIBLE);
                    } else {
                        playerLoading.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    playerLoading.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "Playback error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

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
