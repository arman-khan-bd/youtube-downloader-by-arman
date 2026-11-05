package com.tubepipe.app.data.model;

import java.io.Serializable;

/**
 * Model representing a video card in the YouTube-style feed or search results.
 */
public class VideoItem implements Serializable {
    private String id;
    private String title;
    private String url;
    private String uploaderName;
    private String uploaderAvatarUrl;
    private String thumbnailUrl;
    private long durationSeconds;
    private long viewCount;
    private String uploadDate;

    public VideoItem() {
    }

    public VideoItem(String id, String title, String url, String uploaderName,
                     String uploaderAvatarUrl, String thumbnailUrl,
                     long durationSeconds, long viewCount, String uploadDate) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.uploaderName = uploaderName;
        this.uploaderAvatarUrl = uploaderAvatarUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.viewCount = viewCount;
        this.uploadDate = uploadDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUploaderName() {
        return uploaderName != null ? uploaderName : "";
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getUploaderAvatarUrl() {
        return uploaderAvatarUrl != null ? uploaderAvatarUrl : "";
    }

    public void setUploaderAvatarUrl(String uploaderAvatarUrl) {
        this.uploaderAvatarUrl = uploaderAvatarUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl != null ? thumbnailUrl : "";
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public String getUploadDate() {
        return uploadDate != null ? uploadDate : "";
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }
}
