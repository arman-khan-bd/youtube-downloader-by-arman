package com.tubepipe.app.data.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import androidx.core.content.ContextCompat;

import com.tubepipe.app.util.AppUtils;

import java.io.File;

/**
 * Manages app configuration, particularly storage locations (Phone Internal vs SD Card).
 */
public class SettingsManager {

    private static final String PREF_NAME = "tubepipe_settings";
    private static final String KEY_STORAGE_TYPE = "storage_type";
    private static final String KEY_CUSTOM_STORAGE_PATH = "custom_storage_path";
    private static final String KEY_DEFAULT_VIDEO_QUALITY = "default_video_quality";

    public enum StorageLocation {
        PHONE_INTERNAL,
        SD_CARD
    }

    private static volatile SettingsManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    private SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SettingsManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SettingsManager.class) {
                if (instance == null) {
                    instance = new SettingsManager(context);
                }
            }
        }
        return instance;
    }

    public StorageLocation getStorageLocation() {
        String val = prefs.getString(KEY_STORAGE_TYPE, StorageLocation.PHONE_INTERNAL.name());
        try {
            return StorageLocation.valueOf(val);
        } catch (Exception e) {
            return StorageLocation.PHONE_INTERNAL;
        }
    }

    public void setStorageLocation(StorageLocation location) {
        prefs.edit().putString(KEY_STORAGE_TYPE, location.name()).apply();
    }

    public String getCustomStoragePath() {
        return prefs.getString(KEY_CUSTOM_STORAGE_PATH, null);
    }

    public void setCustomStoragePath(String path) {
        prefs.edit().putString(KEY_CUSTOM_STORAGE_PATH, path).apply();
    }

    /**
     * Resolves and returns the effective download directory based on current settings.
     */
    public File getEffectiveDownloadDirectory() {
        if (getStorageLocation() == StorageLocation.SD_CARD) {
            File sdDir = getSdCardDirectory();
            if (sdDir != null && (sdDir.exists() || sdDir.mkdirs()) && sdDir.canWrite()) {
                return sdDir;
            }
        }

        // Default: Phone Internal Storage Download directory
        File publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(publicDownloads, "TubePipe");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        if (!appDir.canWrite()) {
            // Fallback to app-specific external files dir
            File fallback = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (fallback != null) return fallback;
        }
        return appDir;
    }

    /**
     * Detects external removable SD card directory.
     */
    public File getSdCardDirectory() {
        String custom = getCustomStoragePath();
        if (custom != null) {
            File customFile = new File(custom);
            if (customFile.exists() && customFile.canWrite()) {
                return customFile;
            }
        }

        File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        if (dirs != null && dirs.length > 1) {
            // Index 1 is typically removable SD card
            File sd = dirs[1];
            if (sd != null) {
                File tubePipeSd = new File(sd, "TubePipe");
                if (!tubePipeSd.exists()) tubePipeSd.mkdirs();
                return tubePipeSd;
            }
        }
        return null;
    }

    public boolean isSdCardAvailable() {
        File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        return dirs != null && dirs.length > 1 && dirs[1] != null;
    }

    public long getInternalFreeSpaceBytes() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            return stat.getAvailableBytes();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getInternalTotalSpaceBytes() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            return stat.getTotalBytes();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getSdCardFreeSpaceBytes() {
        try {
            File sd = getSdCardDirectory();
            if (sd != null) {
                StatFs stat = new StatFs(sd.getPath());
                return stat.getAvailableBytes();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public long getSdCardTotalSpaceBytes() {
        try {
            File sd = getSdCardDirectory();
            if (sd != null) {
                StatFs stat = new StatFs(sd.getPath());
                return stat.getTotalBytes();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
