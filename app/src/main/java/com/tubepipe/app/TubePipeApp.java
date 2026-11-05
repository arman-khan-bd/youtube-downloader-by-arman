package com.tubepipe.app;

import android.app.Application;
import android.util.Log;

import com.tubepipe.app.data.downloader.DownloadManager;
import com.tubepipe.app.data.extractor.NewPipeDownloader;
import com.tubepipe.app.data.extractor.YtDlpDownloader;

import org.schabi.newpipe.extractor.NewPipe;

/**
 * Global Application class for TubePipe.
 * Initializes NewPipeExtractor streaming engine and yt-dlp native media downloader.
 */
public class TubePipeApp extends Application {

    private static final String TAG = "TubePipeApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize NewPipeExtractor with our custom OkHttp Downloader
        try {
            NewPipe.init(new NewPipeDownloader());
            Log.d(TAG, "NewPipeExtractor initialized successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize NewPipeExtractor", t);
        }

        // 2. Initialize yt-dlp wrapper and FFmpeg engine for multi-platform downloading
        try {
            YtDlpDownloader.getInstance().init(this);
            Log.d(TAG, "yt-dlp engine initialization dispatched");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize yt-dlp engine", t);
        }

        // 3. Initialize download manager cache
        DownloadManager.getInstance(this);
    }
}
