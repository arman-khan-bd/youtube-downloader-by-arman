package com.tubepipe.app.data.downloader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tubepipe.app.data.model.DownloadItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton managing the download tasks, persistence, and live updates to UI listeners.
 */
public class DownloadManager {

    private static final String PREF_NAME = "tubepipe_downloads";
    private static final String KEY_ITEMS = "saved_downloads";

    private static volatile DownloadManager instance;
    private final Context context;
    private final List<DownloadItem> downloadList = new CopyOnWriteArrayList<>();
    private final List<DownloadListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    public interface DownloadListener {
        void onDownloadUpdated(DownloadItem item);
        void onDownloadCompleted(DownloadItem item);
        void onDownloadFailed(DownloadItem item);
        void onListChanged();
    }

    private DownloadManager(Context context) {
        this.context = context.getApplicationContext();
        loadSavedDownloads();
    }

    public static DownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager(context);
                }
            }
        }
        return instance;
    }

    public void addListener(DownloadListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    public List<DownloadItem> getDownloads() {
        return Collections.unmodifiableList(downloadList);
    }

    /**
     * Enqueues a new download and starts the foreground download service.
     */
    public synchronized void enqueueDownload(DownloadItem item) {
        downloadList.add(0, item);
        saveDownloads();
        notifyListChanged();

        Intent serviceIntent = new Intent(context, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_START_DOWNLOAD);
        serviceIntent.putExtra(DownloadService.EXTRA_DOWNLOAD_ITEM, item);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public synchronized void updateProgress(String id, int progress, String speed) {
        for (DownloadItem item : downloadList) {
            if (item.getId().equals(id)) {
                item.setStatus(DownloadItem.Status.DOWNLOADING);
                item.setProgress(progress);
                item.setDownloadSpeed(speed);
                notifyUpdated(item);
                break;
            }
        }
    }

    public synchronized void markCompleted(String id, String filePath) {
        for (DownloadItem item : downloadList) {
            if (item.getId().equals(id)) {
                item.setStatus(DownloadItem.Status.COMPLETED);
                item.setProgress(100);
                item.setTargetFilePath(filePath);
                item.setDownloadSpeed("Completed");
                saveDownloads();
                notifyCompleted(item);
                break;
            }
        }
    }

    public synchronized void markFailed(String id, String errorMessage) {
        for (DownloadItem item : downloadList) {
            if (item.getId().equals(id)) {
                item.setStatus(DownloadItem.Status.FAILED);
                item.setErrorMessage(errorMessage);
                saveDownloads();
                notifyFailed(item);
                break;
            }
        }
    }

    public synchronized void removeDownload(DownloadItem item) {
        downloadList.remove(item);
        saveDownloads();
        notifyListChanged();
    }

    private void notifyUpdated(DownloadItem item) {
        mainHandler.post(() -> {
            for (DownloadListener l : listeners) {
                l.onDownloadUpdated(item);
            }
        });
    }

    private void notifyCompleted(DownloadItem item) {
        mainHandler.post(() -> {
            for (DownloadListener l : listeners) {
                l.onDownloadCompleted(item);
            }
        });
    }

    private void notifyFailed(DownloadItem item) {
        mainHandler.post(() -> {
            for (DownloadListener l : listeners) {
                l.onDownloadFailed(item);
            }
        });
    }

    private void notifyListChanged() {
        mainHandler.post(() -> {
            for (DownloadListener l : listeners) {
                l.onListChanged();
            }
        });
    }

    private void saveDownloads() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = gson.toJson(downloadList);
            prefs.edit().putString(KEY_ITEMS, json).apply();
        } catch (Throwable ignored) {}
    }

    private void loadSavedDownloads() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_ITEMS, null);
            if (json != null) {
                Type type = new TypeToken<ArrayList<DownloadItem>>() {}.getType();
                List<DownloadItem> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    downloadList.addAll(loaded);
                }
            }
        } catch (Throwable ignored) {}
    }
}
