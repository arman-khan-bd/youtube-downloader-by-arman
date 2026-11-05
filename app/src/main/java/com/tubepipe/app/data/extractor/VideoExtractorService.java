package com.tubepipe.app.data.extractor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.data.model.VideoDetail;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.util.AppUtils;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance video extraction service combining NewPipeExtractor for native YouTube
 * playback & browsing with yt-dlp for universal social media video downloads (FB, TikTok, IG, etc.).
 */
public class VideoExtractorService {

    private static final String TAG = "VideoExtractorService";
    private static volatile VideoExtractorService instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VideoExtractorService() {}

    public static VideoExtractorService getInstance() {
        if (instance == null) {
            synchronized (VideoExtractorService.class) {
                if (instance == null) {
                    instance = new VideoExtractorService();
                }
            }
        }
        return instance;
    }

    public interface FeedCallback {
        void onSuccess(List<VideoItem> videos);
        void onError(Throwable throwable);
    }

    public interface PaginatedCallback {
        void onSuccess(List<VideoItem> videos, Page nextPage, boolean hasMore);
        void onError(Throwable throwable);
    }

    /**
     * Loads YouTube Trending/Home feed using NewPipeExtractor with pagination.
     */
    public void getTrendingVideos(Page page, PaginatedCallback callback) {
        executor.execute(() -> {
            try {
                String kioskId = ServiceList.YouTube.getKioskList().getDefaultKioskId();
                List<VideoItem> videoList = new ArrayList<>();
                Page nextPage = null;
                boolean hasMore = false;

                if (page == null) {
                    KioskInfo kioskInfo = KioskInfo.getInfo(ServiceList.YouTube, kioskId);
                    for (InfoItem item : kioskInfo.getRelatedItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem s = (StreamInfoItem) item;
                            videoList.add(new VideoItem(
                                    s.getUrl(), s.getName(), s.getUrl(),
                                    s.getUploaderName(), s.getUploaderAvatarUrl(),
                                    s.getThumbnailUrl(), s.getDuration(),
                                    s.getViewCount(), s.getTextualUploadDate()
                            ));
                        }
                    }
                    nextPage = kioskInfo.getNextPage();
                    hasMore = kioskInfo.hasNextPage() && nextPage != null;
                } else {
                    ListExtractor.InfoItemsPage<InfoItem> moreItems =
                            KioskInfo.getMoreItems(ServiceList.YouTube, kioskId, page);
                    for (InfoItem item : moreItems.getItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem s = (StreamInfoItem) item;
                            videoList.add(new VideoItem(
                                    s.getUrl(), s.getName(), s.getUrl(),
                                    s.getUploaderName(), s.getUploaderAvatarUrl(),
                                    s.getThumbnailUrl(), s.getDuration(),
                                    s.getViewCount(), s.getTextualUploadDate()
                            ));
                        }
                    }
                    nextPage = moreItems.getNextPage();
                    hasMore = moreItems.hasNextPage() && nextPage != null;
                }

                Page finalNext = nextPage;
                boolean finalHasMore = hasMore;
                mainHandler.post(() -> callback.onSuccess(videoList, finalNext, finalHasMore));
            } catch (Throwable t) {
                Log.e(TAG, "Failed to load trending videos from NewPipe", t);
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }

    public void getTrendingVideos(FeedCallback callback) {
        getTrendingVideos(null, new PaginatedCallback() {
            @Override
            public void onSuccess(List<VideoItem> videos, Page nextPage, boolean hasMore) {
                callback.onSuccess(videos);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    /**
     * Searches YouTube for videos using NewPipeExtractor with pagination.
     */
    public void searchYouTube(String query, Page page, PaginatedCallback callback) {
        executor.execute(() -> {
            try {
                SearchQueryHandler qh = ServiceList.YouTube.getSearchQHFactory().fromQuery(query);
                List<VideoItem> results = new ArrayList<>();
                Page nextPage = null;
                boolean hasMore = false;

                if (page == null) {
                    SearchInfo searchInfo = SearchInfo.getInfo(ServiceList.YouTube, qh);
                    for (InfoItem item : searchInfo.getRelatedItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem s = (StreamInfoItem) item;
                            results.add(new VideoItem(
                                    s.getUrl(), s.getName(), s.getUrl(),
                                    s.getUploaderName(), s.getUploaderAvatarUrl(),
                                    s.getThumbnailUrl(), s.getDuration(),
                                    s.getViewCount(), s.getTextualUploadDate()
                            ));
                        }
                    }
                    nextPage = searchInfo.getNextPage();
                    hasMore = searchInfo.hasNextPage() && nextPage != null;
                } else {
                    ListExtractor.InfoItemsPage<InfoItem> moreItems =
                            SearchInfo.getMoreItems(ServiceList.YouTube, qh, page);
                    for (InfoItem item : moreItems.getItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem s = (StreamInfoItem) item;
                            results.add(new VideoItem(
                                    s.getUrl(), s.getName(), s.getUrl(),
                                    s.getUploaderName(), s.getUploaderAvatarUrl(),
                                    s.getThumbnailUrl(), s.getDuration(),
                                    s.getViewCount(), s.getTextualUploadDate()
                            ));
                        }
                    }
                    nextPage = moreItems.getNextPage();
                    hasMore = moreItems.hasNextPage() && nextPage != null;
                }

                Page finalNext = nextPage;
                boolean finalHasMore = hasMore;
                mainHandler.post(() -> callback.onSuccess(results, finalNext, finalHasMore));
            } catch (Throwable t) {
                Log.e(TAG, "Search failed for query: " + query, t);
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }

    public void searchYouTube(String query, FeedCallback callback) {
        searchYouTube(query, null, new PaginatedCallback() {
            @Override
            public void onSuccess(List<VideoItem> videos, Page nextPage, boolean hasMore) {
                callback.onSuccess(videos);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    /**
     * Extracts full video details, streaming links, and downloadable formats.
     * Automatically routes to NewPipeExtractor for YouTube or yt-dlp for TikTok, FB, IG, etc.
     */
    public void extractVideoInfo(String url, VideoExtractionCallback callback) {
        executor.execute(() -> {
            try {
                String platform = AppUtils.detectPlatform(url);

                if ("YouTube".equals(platform)) {
                    // Extract via NewPipe
                    StreamInfo streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url);
                    VideoDetail detail = new VideoDetail();
                    detail.setId(streamInfo.getUrl());
                    detail.setTitle(streamInfo.getName());
                    detail.setDescription(streamInfo.getDescription() != null ? streamInfo.getDescription().getContent() : "");
                    detail.setWebUrl(streamInfo.getUrl());
                    detail.setUploaderName(streamInfo.getUploaderName());
                    detail.setUploaderAvatarUrl(streamInfo.getUploaderAvatarUrl());
                    detail.setUploaderSubscriberCount(streamInfo.getUploaderSubscriberCount() != -1
                            ? AppUtils.formatViews(streamInfo.getUploaderSubscriberCount()) + " subscribers"
                            : "");
                    detail.setViewCount(streamInfo.getViewCount());
                    detail.setDurationSeconds(streamInfo.getDuration());
                    detail.setUploadDate(streamInfo.getTextualUploadDate());
                    detail.setThumbnailUrl(streamInfo.getThumbnailUrl());

                    // Video Streams (Combined Video + Audio preferred)
                    List<StreamQuality> videoQualities = new ArrayList<>();
                    for (VideoStream vs : streamInfo.getVideoStreams()) {
                        if (vs.getContent() != null) {
                            String res = vs.getResolution() != null ? vs.getResolution() : "Video";
                            videoQualities.add(new StreamQuality(
                                    vs.getFormat().getName(),
                                    res + " (" + vs.getFormat().getName() + ")",
                                    vs.getFormat().getSuffix(),
                                    false,
                                    vs.getContent(),
                                    vs.getBitrate() * streamInfo.getDuration() / 8,
                                    vs.getBitrate()
                            ));
                        }
                    }

                    // Video-only streams (High Definition e.g. 1080p / 1440p / 4K)
                    for (VideoStream vs : streamInfo.getVideoOnlyStreams()) {
                        if (vs.getContent() != null) {
                            String res = vs.getResolution() != null ? vs.getResolution() : "HD Video";
                            videoQualities.add(new StreamQuality(
                                    vs.getFormat().getName() + "_hd",
                                    res + " HD (" + vs.getFormat().getName() + ")",
                                    vs.getFormat().getSuffix(),
                                    false,
                                    vs.getContent(),
                                    vs.getBitrate() * streamInfo.getDuration() / 8,
                                    vs.getBitrate()
                            ));
                        }
                    }

                    // Audio Streams (MP3/M4A/WebM)
                    List<StreamQuality> audioQualities = new ArrayList<>();
                    for (AudioStream as : streamInfo.getAudioStreams()) {
                        if (as.getContent() != null) {
                            String format = as.getFormat().getName().toUpperCase();
                            audioQualities.add(new StreamQuality(
                                    as.getFormat().getName(),
                                    "Audio " + format + " (" + (as.getAverageBitrate() > 0 ? as.getAverageBitrate() + " kbps" : "HQ") + ")",
                                    as.getFormat().getSuffix(),
                                    true,
                                    as.getContent(),
                                    as.getAverageBitrate() * 1000L * streamInfo.getDuration() / 8,
                                    as.getAverageBitrate()
                            ));
                        }
                    }

                    // Related videos
                    List<VideoItem> related = new ArrayList<>();
                    for (InfoItem item : streamInfo.getRelatedItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem s = (StreamInfoItem) item;
                            related.add(new VideoItem(
                                    s.getUrl(),
                                    s.getName(),
                                    s.getUrl(),
                                    s.getUploaderName(),
                                    s.getUploaderAvatarUrl(),
                                    s.getThumbnailUrl(),
                                    s.getDuration(),
                                    s.getViewCount(),
                                    s.getTextualUploadDate()
                            ));
                        }
                    }

                    detail.setVideoStreams(videoQualities);
                    detail.setAudioStreams(audioQualities);
                    detail.setRelatedVideos(related);

                    mainHandler.post(() -> callback.onSuccess(detail));
                } else {
                    // Non-YouTube: TikTok, Facebook, Instagram, Twitter/X
                    // Extract via yt-dlp native wrapper
                    YtDlpDownloader.getInstance().extractInfo(url, new YtDlpDownloader.ExtractionCallback() {
                        @Override
                        public void onSuccess(VideoDetail detail) {
                            mainHandler.post(() -> callback.onSuccess(detail));
                        }

                        @Override
                        public void onError(Throwable error) {
                            Log.w(TAG, "yt-dlp extraction fallback: " + error.getMessage());
                            // Fallback generic representation for direct download
                            VideoDetail fallback = new VideoDetail();
                            fallback.setId(url);
                            fallback.setTitle(platform + " Video");
                            fallback.setWebUrl(url);
                            fallback.setUploaderName(platform);

                            List<StreamQuality> fallbackQualities = new ArrayList<>();
                            fallbackQualities.add(new StreamQuality(
                                    "best",
                                    "Best Video (Auto - " + platform + ")",
                                    "mp4",
                                    false,
                                    url,
                                    0,
                                    0
                            ));
                            fallbackQualities.add(new StreamQuality(
                                    "bestaudio",
                                    "Audio Only (MP3)",
                                    "mp3",
                                    true,
                                    url,
                                    0,
                                    0
                            ));
                            fallback.setVideoStreams(fallbackQualities);

                            mainHandler.post(() -> callback.onSuccess(fallback));
                        }
                    });
                }
            } catch (Throwable t) {
                Log.e(TAG, "Extraction failed for URL: " + url, t);
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }
}
