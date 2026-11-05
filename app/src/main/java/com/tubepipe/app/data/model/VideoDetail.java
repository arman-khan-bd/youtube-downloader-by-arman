package com.tubepipe.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed metadata of a video (title, channel info, stream formats, related videos).
 */
public class VideoDetail implements Serializable {
    private String id;
    private String title;
    private String description;
    private String webUrl;
    private String uploaderName;
    private String uploaderAvatarUrl;
    private String uploaderSubscriberCount;
    private long viewCount;
    private long durationSeconds;
    private String uploadDate;
    private String thumbnailUrl;
    private List<StreamQuality> videoStreams = new ArrayList<>();
    private List<StreamQuality> audioStreams = new ArrayList<>();
    private List<VideoItem> relatedVideos = new ArrayList<>();

    public VideoDetail() {}

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

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebUrl() {
        return webUrl != null ? webUrl : "";
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
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

    public String getUploaderSubscriberCount() {
        return uploaderSubscriberCount != null ? uploaderSubscriberCount : "";
    }

    public void setUploaderSubscriberCount(String uploaderSubscriberCount) {
        this.uploaderSubscriberCount = uploaderSubscriberCount;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getUploadDate() {
        return uploadDate != null ? uploadDate : "";
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl != null ? thumbnailUrl : "";
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public List<StreamQuality> getVideoStreams() {
        return videoStreams;
    }

    public void setVideoStreams(List<StreamQuality> videoStreams) {
        this.videoStreams = videoStreams;
    }

    public List<StreamQuality> getAudioStreams() {
        return audioStreams;
    }

    public void setAudioStreams(List<StreamQuality> audioStreams) {
        this.audioStreams = audioStreams;
    }

    public List<VideoItem> getRelatedVideos() {
        return relatedVideos;
    }

    public void setRelatedVideos(List<VideoItem> relatedVideos) {
        this.relatedVideos = relatedVideos;
    }
}
