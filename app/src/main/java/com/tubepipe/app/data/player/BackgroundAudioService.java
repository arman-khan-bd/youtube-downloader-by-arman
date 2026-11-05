package com.tubepipe.app.data.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.tubepipe.app.R;
import com.tubepipe.app.ui.main.MainActivity;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Robust Foreground Audio Player Service providing lock screen controls,
 * status bar media notifications, and uninterrupted background playback.
 */
public class BackgroundAudioService extends Service {

    public static final String ACTION_PLAY_ITEM = "com.tubepipe.action.PLAY_AUDIO_ITEM";
    public static final String ACTION_PLAY = "com.tubepipe.action.AUDIO_PLAY";
    public static final String ACTION_PAUSE = "com.tubepipe.action.AUDIO_PAUSE";
    public static final String ACTION_TOGGLE = "com.tubepipe.action.AUDIO_TOGGLE";
    public static final String ACTION_REWIND = "com.tubepipe.action.AUDIO_REWIND";
    public static final String ACTION_FORWARD = "com.tubepipe.action.AUDIO_FORWARD";
    public static final String ACTION_STOP = "com.tubepipe.action.AUDIO_STOP";

    public static final String EXTRA_TITLE = "extra_audio_title";
    public static final String EXTRA_ARTIST = "extra_audio_artist";
    public static final String EXTRA_URI = "extra_audio_uri";
    public static final String EXTRA_ART_URL = "extra_audio_art_url";
    public static final String EXTRA_START_POSITION = "extra_start_position";

    private static final String CHANNEL_ID = "tubepipe_audio_channel";
    private static final int NOTIFICATION_ID = 3001;

    public interface AudioPlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onTrackChanged(String title, String artist, String artUrl);
        void onProgress(long positionMs, long durationMs);
        void onStopped();
    }

    private static final CopyOnWriteArrayList<AudioPlaybackListener> listeners = new CopyOnWriteArrayList<>();
    private static BackgroundAudioService instance;

    private ExoPlayer player;
    private NotificationManager notificationManager;
    private String currentTitle = "";
    private String currentArtist = "";
    private String currentArtUrl = "";
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                for (AudioPlaybackListener l : listeners) {
                    l.onProgress(pos, dur > 0 ? dur : 0);
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    public static void addListener(AudioPlaybackListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(AudioPlaybackListener listener) {
        listeners.remove(listener);
    }

    public static BackgroundAudioService getInstance() {
        return instance;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

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
            public void onIsPlayingChanged(boolean isPlaying) {
                updateNotification();
                for (AudioPlaybackListener l : listeners) {
                    l.onPlaybackStateChanged(isPlaying);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    for (AudioPlaybackListener l : listeners) {
                        l.onPlaybackStateChanged(false);
                    }
                }
            }
        });

        progressHandler.post(progressRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY_ITEM:
                currentTitle = intent.getStringExtra(EXTRA_TITLE);
                currentArtist = intent.getStringExtra(EXTRA_ARTIST);
                currentArtUrl = intent.getStringExtra(EXTRA_ART_URL);
                String uriStr = intent.getStringExtra(EXTRA_URI);
                long startPos = intent.getLongExtra(EXTRA_START_POSITION, 0);

                if (uriStr != null) {
                    playUri(uriStr, startPos);
                }
                break;
            case ACTION_PLAY:
                if (player != null) player.play();
                break;
            case ACTION_PAUSE:
                if (player != null) player.pause();
                break;
            case ACTION_TOGGLE:
                if (player != null) {
                    if (player.isPlaying()) player.pause();
                    else player.play();
                }
                break;
            case ACTION_REWIND:
                if (player != null) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                }
                break;
            case ACTION_FORWARD:
                if (player != null) {
                    player.seekTo(player.getCurrentPosition() + 10000);
                }
                break;
            case ACTION_STOP:
                stopSelf();
                break;
        }

        updateNotification();
        return START_STICKY;
    }

    private void playUri(String uriStr, long startPositionMs) {
        Uri uri;
        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
            uri = Uri.parse(uriStr);
        } else {
            uri = Uri.fromFile(new File(uriStr));
        }

        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs);
        }
        player.play();

        startForeground(NOTIFICATION_ID, buildNotification());

        for (AudioPlaybackListener l : listeners) {
            l.onTrackChanged(currentTitle, currentArtist, currentArtUrl);
            l.onPlaybackStateChanged(true);
        }
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public String getCurrentTitle() {
        return currentTitle != null ? currentTitle : "";
    }

    public String getCurrentArtist() {
        return currentArtist != null ? currentArtist : "";
    }

    public String getCurrentArtUrl() {
        return currentArtUrl != null ? currentArtUrl : "";
    }

    private void updateNotification() {
        if (player != null) {
            Notification notification = buildNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        boolean isPlaying = player != null && player.isPlaying();

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        PendingIntent prevIntent = getServiceActionIntent(ACTION_REWIND, 1);
        PendingIntent toggleIntent = getServiceActionIntent(ACTION_TOGGLE, 2);
        PendingIntent fwdIntent = getServiceActionIntent(ACTION_FORWARD, 3);
        PendingIntent stopIntent = getServiceActionIntent(ACTION_STOP, 4);

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentTitle.isEmpty() ? "TubePipe Player" : currentTitle)
                .setContentText(currentArtist.isEmpty() ? "Background Playback" : currentArtist)
                .setSmallIcon(R.drawable.ic_audio)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_arrow_back, "Rewind 10s", prevIntent)
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", toggleIntent)
                .addAction(R.drawable.ic_arrow_back, "Forward 10s", fwdIntent)
                .addAction(R.drawable.ic_pause, "Close", stopIntent)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private PendingIntent getServiceActionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, BackgroundAudioService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this, requestCode, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TubePipe Audio Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background audio player notification controls");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
        for (AudioPlaybackListener l : listeners) {
            l.onStopped();
        }
        instance = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
