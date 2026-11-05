package com.tubepipe.app.data.model;

import java.io.Serializable;

public class DownloadItem implements Serializable {

    public enum Status {
        QUEUED,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        PAUSED
    }

    private String id;
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private String sourceUrl;
    private String streamUrl;
    private String targetFilePath;
    private String qualityLabel;
    private boolean isAudioOnly;
    private Status status = Status.QUEUED;
    private int progress = 0;
    private long downloadedBytes = 0;
    private long totalBytes = 0;
    private String downloadSpeed = "";
    private long timestamp = System.currentTimeMillis();
    private String errorMessage = "";

    public DownloadItem() {}

    public DownloadItem(String id, String videoId, String title, String thumbnailUrl,
                        String sourceUrl, String streamUrl, String qualityLabel, boolean isAudioOnly) {
        this.id = id;
        this.videoId = videoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.sourceUrl = sourceUrl;
        this.streamUrl = streamUrl;
        this.qualityLabel = qualityLabel;
        this.isAudioOnly = isAudioOnly;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl != null ? thumbnailUrl : "";
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getSourceUrl() {
        return sourceUrl != null ? sourceUrl : "";
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getStreamUrl() {
        return streamUrl != null ? streamUrl : "";
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getTargetFilePath() {
        return targetFilePath != null ? targetFilePath : "";
    }

    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

    public String getQualityLabel() {
        return qualityLabel != null ? qualityLabel : "";
    }

    public void setQualityLabel(String qualityLabel) {
        this.qualityLabel = qualityLabel;
    }

    public boolean isAudioOnly() {
        return isAudioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        isAudioOnly = audioOnly;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getDownloadSpeed() {
        return downloadSpeed != null ? downloadSpeed : "";
    }

    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage != null ? errorMessage : "";
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
