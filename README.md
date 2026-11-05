# TubePipe - YouTube-style Android App with yt-dlp & NewPipe Technology

A full-featured YouTube-style native Android application written in **Java** that supports **Android 5.0 (API 21 Lollipop)** up to **Android 14+ (API 34)**.

TubePipe integrates **NewPipeExtractor** for native YouTube browsing and streaming, and **yt-dlp** (`youtubedl-android`) with embedded FFmpeg for downloading videos and audio from **YouTube, Facebook, TikTok, Instagram, Twitter/X**, and other major platforms.

---

## 🚀 Key Features

### 1. Dual-Tab Download Dialog (Video & Audio) with File Sizes
- **Separate Video & Audio Tabs**:
  - **Video Quality Tab**: 1080p 60fps, 1080p, 720p, 480p, 360p with resolution badges, format tags (MP4/WebM), bitrate, and **calculated/estimated file size badges** (e.g., `85.4 MB`, `42.1 MB`).
  - **Audio Quality Tab**: High Quality MP3 (320 kbps), Standard MP3 (128 kbps), M4A (160 kbps), and Opus with bitrate tags and exact file size badges (e.g., `7.2 MB`, `3.8 MB`).
- **Dynamic Download Button**: Updates in real-time according to selection (e.g. `Download 1080p Full HD (85.4 MB)` or `Download Audio (7.2 MB)`).
- **Target Storage Indicator**: Shows currently selected storage destination and available free disk space directly inside the download sheet.

### 2. Storage Settings (Phone Internal vs. SD Card)
- **Dedicated Settings Screen (`SettingsActivity`)**:
  - Toggle between **Internal Phone Storage** (`/Download/TubePipe`) and **Removable SD Card** (`/storage/XXXX-XXXX/.../TubePipe`).
  - Live disk space meters showing total and available free storage on Phone and SD Card.
  - **Custom Folder Picker**: Select any custom directory via Android Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`).
  - Automatically routes both yt-dlp downloads and direct stream downloads to the selected target directory.

### 3. YouTube-style Infinite Scrolling Pagination
- **Home Feed**: Continuously loads trending and popular videos using NewPipeExtractor's continuation token / `Page` iterator. Reaching near the bottom triggers seamless automatic page loading with a YouTube-red loading spinner.
- **Search Screen**: Continuous infinite scrolling for search queries without losing search state.

### 4. Categorized Downloads (Multiple Tabs for Videos & Audios)
- **Videos Tab**: Lists all downloaded offline videos with 16:9 thumbnails, resolution badge, duration, file size, and a one-tap button to launch the **ExoPlayer Video Player**.
- **Audios Tab**: Lists all downloaded music and audio tracks with audio artwork, MP3/M4A bitrate tag, file size, and one-tap playback in the **Background Audio Player**.
- Real-time download progress tracking with speeds and ETA.

### 5. Background Play System & Audio/Video Player
- **Background Audio Player Service (`BackgroundAudioService`)**:
  - Android Foreground Service with MediaStyle notifications.
  - Works when the screen is locked, the app is closed, or another app is running.
  - Lock-screen & notification media controls: Rewind 10s, Play/Pause, Forward 10s, Close.
- **Global Bottom Mini-Player**:
  - YouTube Music / Spotify-style mini-player above bottom navigation with thumbnail, marquee title, artist, and play/pause button.
  - Tapping opens the full **Audio Player Bottom Sheet** with large artwork, seekbar with elapsed/remaining times, 10s scrub controls, and storage tags.
- **Video Player Background Audio Toggle**:
  - YouTube-style video player contains a **"Background Play"** button (`btn_player_bg_audio`) to instantly hand off video playback to background audio without losing your current playback timestamp.

### 6. Universal Share-to-Download (FB, TikTok, IG, YT, X)
- Android `ACTION_SEND` and `ACTION_VIEW` intent filters allow you to tap **Share** inside TikTok, Facebook Watch, Instagram Reels, YouTube, or browser and pick **TubePipe** to inspect formats and download or stream immediately.

---

## 📁 Project Structure

```
c:\Users\Admin\Documents\android\
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew.bat
├── README.md
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/tubepipe/app/
        │   ├── TubePipeApp.java
        │   ├── data/
        │   │   ├── downloader/ (DownloadManager, DownloadService)
        │   │   ├── extractor/  (NewPipeDownloader, VideoExtractorService, YtDlpDownloader, VideoExtractionCallback)
        │   │   ├── model/      (VideoItem, VideoDetail, StreamQuality, DownloadItem)
        │   │   ├── player/     (BackgroundAudioService)
        │   │   └── settings/   (SettingsManager)
        │   ├── ui/
        │   │   ├── adapter/    (VideoAdapter, DownloadAdapter, QualityOptionAdapter)
        │   │   ├── dialog/     (DownloadDialog)
        │   │   ├── fragments/  (HomeFragment, SearchFragment, DownloadsFragment)
        │   │   ├── main/       (MainActivity)
        │   │   ├── player/     (PlayerActivity, AudioPlayerBottomSheet)
        │   │   ├── settings/   (SettingsActivity)
        │   │   └── share/      (ShareReceiverActivity)
        │   └── util/           (AppUtils, NetworkUtils, PermissionUtils)
        └── res/
            ├── color/          (bottom_nav_colors.xml)
            ├── drawable/       (YouTube vector icons, shapes, badges)
            ├── layout/         (activity_main, activity_player, activity_settings, dialog_audio_player, dialog_download_options, etc.)
            ├── menu/           (bottom_nav_menu.xml, main_top_menu.xml)
            └── values/         (colors.xml, strings.xml, themes.xml)
```

---

## 🛠️ How to Open and Build in Android Studio

1. **Open Android Studio**.
2. Select **File -> Open...** and browse to `c:\Users\Admin\Documents\android`.
3. Allow Gradle to sync dependencies from Google, MavenCentral, and JitPack.
4. Connect an Android device (Android 5.0 or newer) or launch an Android emulator.
5. Click **Run -> Run 'app'** (`Shift + F10`) to compile and launch.
