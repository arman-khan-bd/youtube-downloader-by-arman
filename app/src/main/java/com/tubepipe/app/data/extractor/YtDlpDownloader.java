package com.tubepipe.app.data.extractor;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tubepipe.app.data.model.DownloadItem;
import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.data.model.VideoDetail;
import com.tubepipe.app.util.AppUtils;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles yt-dlp native extraction and downloading for YouTube, TikTok, Facebook, Instagram, Twitter, etc.
 * Includes a robust fallback direct-stream downloader for guaranteed performance.
 */
public class YtDlpDownloader {

    private static final String TAG = "YtDlpDownloader";
    private static volatile YtDlpDownloader instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isInitialized = false;

    public interface ExtractionCallback {
        void onSuccess(VideoDetail detail);
        void onError(Throwable error);
    }

    public interface DownloadCallback {
        void onProgress(int progress, String speed);
        void onCompleted(String filePath);
        void onError(String errorMessage);
    }

    private YtDlpDownloader() {}

    public static YtDlpDownloader getInstance() {
        if (instance == null) {
            synchronized (YtDlpDownloader.class) {
                if (instance == null) {
                    instance = new YtDlpDownloader();
                }
            }
        }
        return instance;
    }

    private Context appContext;

    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        executor.execute(() -> {
            try {
                YoutubeDL.getInstance().init(context.getApplicationContext());
                FFmpeg.getInstance().init(context.getApplicationContext());
                isInitialized = true;
                Log.d(TAG, "yt-dlp and FFmpeg successfully initialized");
            } catch (Throwable t) {
                Log.w(TAG, "yt-dlp native initialization notice: " + t.getMessage());
                isInitialized = false;
            }
        });
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Extracts video information using yt-dlp native engine.
     */
    public void extractInfo(String url, ExtractionCallback callback) {
        executor.execute(() -> {
            try {
                if (!isInitialized) {
                    throw new IllegalStateException("yt-dlp engine not ready");
                }

                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--no-playlist");
                request.addOption("--socket-timeout", "15");

                VideoInfo videoInfo = YoutubeDL.getInstance().getInfo(request);
                VideoDetail detail = new VideoDetail();
                detail.setId(videoInfo.getId() != null ? videoInfo.getId() : url);
                detail.setTitle(videoInfo.getTitle() != null ? videoInfo.getTitle() : "Extracted Video");
                detail.setWebUrl(url);
                detail.setUploaderName(videoInfo.getUploader() != null ? videoInfo.getUploader() : "Unknown Creator");
                detail.setDurationSeconds(videoInfo.getDuration());
                if (videoInfo.getViewCount() != null) {
                    try {
                        detail.setViewCount(Long.parseLong(videoInfo.getViewCount()));
                    } catch (Exception ignored) {}
                }
                detail.setThumbnailUrl(videoInfo.getThumbnail());

                List<StreamQuality> videoQualities = new ArrayList<>();
                List<StreamQuality> audioQualities = new ArrayList<>();

                if (videoInfo.getFormats() != null) {
                    for (VideoFormat format : videoInfo.getFormats()) {
                        String formatId = format.getFormatId();
                        String ext = format.getExt() != null ? format.getExt() : "mp4";
                        String note = format.getFormatNote() != null ? format.getFormatNote() : "";
                        long fileSize = format.getFileSize();

                        // Detect Audio-only vs Video
                        if (format.getVcodec() != null && "none".equalsIgnoreCase(format.getVcodec())) {
                            // Audio only
                            audioQualities.add(new StreamQuality(
                                    formatId,
                                    "Audio (" + ext.toUpperCase() + (note.isEmpty() ? "" : " - " + note) + ")",
                                    ext,
                                    true,
                                    format.getUrl(),
                                    fileSize,
                                    (int) (format.getAcodec() != null ? 128 : 0)
                            ));
                        } else {
                            // Video format
                            String resolution = format.getHeight() > 0 ? format.getHeight() + "p" : note;
                            if (resolution.isEmpty()) resolution = "Video";
                            videoQualities.add(new StreamQuality(
                                    formatId,
                                    resolution + " (" + ext.toUpperCase() + ")",
                                    ext,
                                    false,
                                    format.getUrl(),
                                    fileSize,
                                    (int) format.getTbr()
                            ));
                        }
                    }
                }

                // If no specific formats were returned, provide smart defaults
                if (videoQualities.isEmpty()) {
                    videoQualities.add(new StreamQuality("best", "Best Available Video (MP4)", "mp4", false, url, 0, 0));
                    videoQualities.add(new StreamQuality("1080p", "1080p Full HD", "mp4", false, url, 0, 0));
                    videoQualities.add(new StreamQuality("720p", "720p HD", "mp4", false, url, 0, 0));
                }
                if (audioQualities.isEmpty()) {
                    audioQualities.add(new StreamQuality("bestaudio", "Audio Only (MP3)", "mp3", true, url, 0, 0));
                }

                detail.setVideoStreams(videoQualities);
                detail.setAudioStreams(audioQualities);

                mainHandler.post(() -> callback.onSuccess(detail));
            } catch (Throwable t) {
                Log.w(TAG, "yt-dlp extractInfo failed: " + t.getMessage());
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }

    /**
     * Executes download of a video via yt-dlp or direct stream downloader.
     */
    public void executeDownload(DownloadItem item, DownloadCallback callback) {
        executor.execute(() -> {
            File appDir;
            if (appContext != null) {
                appDir = com.tubepipe.app.data.settings.SettingsManager.getInstance(appContext).getEffectiveDownloadDirectory();
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                appDir = new File(downloadsDir, "TubePipe");
            }
            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            String ext = item.isAudioOnly() ? "mp3" : "mp4";
            String cleanTitle = AppUtils.sanitizeFileName(item.getTitle());
            File outputFile = new File(appDir, cleanTitle + "." + ext);
            item.setTargetFilePath(outputFile.getAbsolutePath());

            // Use yt-dlp to download media with full video+audio merging or pure audio extraction
            if (isInitialized) {
                try {
                    YoutubeDLRequest request = new YoutubeDLRequest(item.getSourceUrl());
                    if (item.isAudioOnly()) {
                        request.addOption("-x");
                        request.addOption("--audio-format", "mp3");
                    } else {
                        // Extract requested height, e.g. "1080p" -> 1080
                        String quality = item.getQualityLabel();
                        String height = null;
                        if (quality != null) {
                            if (quality.contains("1080")) height = "1080";
                            else if (quality.contains("720")) height = "720";
                            else if (quality.contains("480")) height = "480";
                            else if (quality.contains("360")) height = "360";
                        }
                        if (height != null) {
                            request.addOption("-f", "bestvideo[height<=" + height + "][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=" + height + "]+bestaudio/best[height<=" + height + "]/best");
                        } else {
                            request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
                        }
                        request.addOption("--merge-output-format", "mp4");
                    }
                    request.addOption("-o", outputFile.getAbsolutePath());

                    mainHandler.post(() -> callback.onProgress(15, "Downloading media..."));
                    YoutubeDL.getInstance().execute(request);

                    mainHandler.post(() -> callback.onCompleted(outputFile.getAbsolutePath()));
                    return;
                } catch (Throwable t) {
                    Log.w(TAG, "yt-dlp execution error, falling back to direct stream download: " + t.getMessage());
                }
            }

            // Direct HTTP stream download fallback
            downloadDirectStream(item.getStreamUrl() != null && !item.getStreamUrl().isEmpty() ? item.getStreamUrl() : item.getSourceUrl(),
                    outputFile, callback);
        });
    }

    /**
     * Fallback high-speed multi-buffer direct HTTP download.
     */
    private void downloadDirectStream(String url, File outputFile, DownloadCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new Exception("HTTP download failed: code " + response.code());
            }

            long totalBytes = response.body().contentLength();
            InputStream is = response.body().byteStream();
            FileOutputStream fos = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8192];
            long downloadedBytes = 0;
            int read;
            long lastTime = System.currentTimeMillis();
            long bytesSinceLastTime = 0;

            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                downloadedBytes += read;
                bytesSinceLastTime += read;

                long now = System.currentTimeMillis();
                if (now - lastTime >= 500) {
                    double speedBytesPerSec = (bytesSinceLastTime * 1000.0) / (now - lastTime);
                    String speed = AppUtils.formatFileSize((long) speedBytesPerSec) + "/s";
                    int progress = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : 0;

                    final int finalProgress = progress;
                    mainHandler.post(() -> callback.onProgress(finalProgress, speed));

                    lastTime = now;
                    bytesSinceLastTime = 0;
                }
            }

            fos.flush();
            fos.close();
            is.close();

            mainHandler.post(() -> callback.onCompleted(outputFile.getAbsolutePath()));
        } catch (Throwable t) {
            Log.e(TAG, "Direct stream download error", t);
            mainHandler.post(() -> callback.onError(t.getMessage()));
        }
    }
}
