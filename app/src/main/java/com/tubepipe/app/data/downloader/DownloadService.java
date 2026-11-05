package com.tubepipe.app.data.downloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.tubepipe.app.R;
import com.tubepipe.app.data.extractor.YtDlpDownloader;
import com.tubepipe.app.data.model.DownloadItem;
import com.tubepipe.app.ui.main.MainActivity;

import java.util.LinkedList;
import java.util.Queue;

public class DownloadService extends Service {

    public static final String ACTION_START_DOWNLOAD = "com.tubepipe.action.START_DOWNLOAD";
    public static final String EXTRA_DOWNLOAD_ITEM = "extra_download_item";
    public static final String CHANNEL_ID = "tubepipe_downloads_channel";
    private static final int NOTIFICATION_ID = 2001;

    private NotificationManager notificationManager;
    private final Queue<DownloadItem> taskQueue = new LinkedList<>();
    private boolean isDownloading = false;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START_DOWNLOAD.equals(intent.getAction())) {
            DownloadItem item = (DownloadItem) intent.getSerializableExtra(EXTRA_DOWNLOAD_ITEM);
            if (item != null) {
                taskQueue.offer(item);
                if (!isDownloading) {
                    startForeground(NOTIFICATION_ID, buildProgressNotification(item, 0, "Starting download…"));
                    processNextDownload();
                }
            }
        }
        return START_NOT_STICKY;
    }

    private void processNextDownload() {
        if (taskQueue.isEmpty()) {
            isDownloading = false;
            stopForeground(true);
            stopSelf();
            return;
        }

        isDownloading = true;
        DownloadItem currentItem = taskQueue.poll();
        if (currentItem == null) {
            processNextDownload();
            return;
        }

        updateNotification(currentItem, 0, "Connecting to media stream…");

        YtDlpDownloader.getInstance().executeDownload(currentItem, new YtDlpDownloader.DownloadCallback() {
            @Override
            public void onProgress(int progress, String speed) {
                DownloadManager.getInstance(DownloadService.this).updateProgress(currentItem.getId(), progress, speed);
                updateNotification(currentItem, progress, speed);
            }

            @Override
            public void onCompleted(String filePath) {
                DownloadManager.getInstance(DownloadService.this).markCompleted(currentItem.getId(), filePath);
                showCompletionNotification(currentItem);
                processNextDownload();
            }

            @Override
            public void onError(String errorMessage) {
                DownloadManager.getInstance(DownloadService.this).markFailed(currentItem.getId(), errorMessage);
                processNextDownload();
            }
        });
    }

    private void updateNotification(DownloadItem item, int progress, String speed) {
        Notification notification = buildProgressNotification(item, progress, speed);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification buildProgressNotification(DownloadItem item, int progress, String speed) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(item.getTitle())
                .setContentText(speed != null && !speed.isEmpty() ? speed : "Downloading…")
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(100, progress, progress == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void showCompletionNotification(DownloadItem item) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.download_completed))
                .setContentText(item.getTitle())
                .setSmallIcon(R.drawable.ic_download)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TubePipe Downloads",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Progress and completion of media downloads");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
