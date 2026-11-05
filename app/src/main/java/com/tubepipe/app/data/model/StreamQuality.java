package com.tubepipe.app.data.model;

import java.io.Serializable;

/**
 * Represents a video resolution or audio format available for playback or download.
 */
public class StreamQuality implements Serializable {
    private String formatId;
    private String resolution; // e.g. "1080p", "720p", "480p", "Audio Only (MP3)"
    private String extension;  // e.g. "mp4", "webm", "m4a", "mp3"
    private boolean isAudioOnly;
    private String streamUrl;
    private long approxSizeBytes;
    private int bitrate;

    public StreamQuality() {}

    public StreamQuality(String formatId, String resolution, String extension,
                         boolean isAudioOnly, String streamUrl, long approxSizeBytes, int bitrate) {
        this.formatId = formatId;
        this.resolution = resolution;
        this.extension = extension;
        this.isAudioOnly = isAudioOnly;
        this.streamUrl = streamUrl;
        this.approxSizeBytes = approxSizeBytes;
        this.bitrate = bitrate;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getResolution() {
        return resolution != null ? resolution : "Unknown Quality";
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getExtension() {
        return extension != null ? extension : "mp4";
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean isAudioOnly() {
        return isAudioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        isAudioOnly = audioOnly;
    }

    public String getStreamUrl() {
        return streamUrl != null ? streamUrl : "";
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public long getApproxSizeBytes() {
        return approxSizeBytes;
    }

    public void setApproxSizeBytes(long approxSizeBytes) {
        this.approxSizeBytes = approxSizeBytes;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
}
