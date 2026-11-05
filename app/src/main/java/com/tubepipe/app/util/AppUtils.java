package com.tubepipe.app.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\.-]+(?:\\.[\\w\\.-]+)+[/\\w\\._~:?#\\[\\]@!$&'()*+,;=–-]*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extracts the first HTTP/HTTPS URL found in a shared text string.
     */
    public static String extractUrl(String text) {
        if (text == null) return null;
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Identifies the social media or video platform from a URL.
     */
    public static String detectPlatform(String url) {
        if (url == null) return "Universal Web";
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return "YouTube";
        } else if (lower.contains("tiktok.com")) {
            return "TikTok";
        } else if (lower.contains("facebook.com") || lower.contains("fb.watch")) {
            return "Facebook";
        } else if (lower.contains("instagram.com")) {
            return "Instagram";
        } else if (lower.contains("twitter.com") || lower.contains("x.com")) {
            return "Twitter / X";
        }
        return "Web Media";
    }

    /**
     * Formats video duration in seconds into mm:ss or hh:mm:ss.
     */
    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) return "00:00";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats view count into readable short format like 1.2M, 450K.
     */
    public static String formatViews(long views) {
        if (views < 1000) {
            return String.valueOf(views);
        } else if (views < 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fK", views / 1000.0);
        } else if (views < 1_000_000_000) {
            return String.format(Locale.getDefault(), "%.1fM", views / 1_000_000.0);
        } else {
            return String.format(Locale.getDefault(), "%.1fB", views / 1_000_000_000.0);
        }
    }

    /**
     * Formats bytes into human-readable size string.
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.getDefault(), "%.1f %s",
                bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Sanitizes a string so it can be safely used as a filename.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "video_" + System.currentTimeMillis();
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
