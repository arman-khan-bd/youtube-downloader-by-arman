package com.tubepipe.app.data.extractor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.data.model.VideoDetail;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.util.AppUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.Image;
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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * High-performance video extraction service combining multi-instance REST API
 * for ultra-fast Bangladesh feeds & search with NewPipeExtractor for deep stream decoding.
 */
public class VideoExtractorService {

    private static final String TAG = "VideoExtractorService";
    private static volatile VideoExtractorService instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient httpClient;

    private static final String[] PIPED_INSTANCES = new String[]{
            "https://api.piped.private.coffee",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.kavin.rocks"
    };

    private VideoExtractorService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

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

    private List<VideoItem> fetchPipedTrendingBD() {
        for (String baseUrl : PIPED_INSTANCES) {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/trending?region=BD")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody body = response.body();
                    if (response.isSuccessful() && body != null) {
                        JSONArray array = new JSONArray(body.string());
                        List<VideoItem> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String rawUrl = obj.optString("url", "");
                            String videoUrl = rawUrl.startsWith("http") ? rawUrl : "https://www.youtube.com" + rawUrl;
                            String title = obj.optString("title", "Video");
                            String thumb = obj.optString("thumbnail", "");
                            String uploader = obj.optString("uploaderName", "YouTube Channel");
                            String avatar = obj.optString("uploaderAvatar", "");
                            long duration = obj.optLong("duration", 0);
                            long views = obj.optLong("views", 0);
                            String uploadDate = obj.optString("uploadedDate", "");

                            list.add(new VideoItem(videoUrl, title, videoUrl, uploader, avatar, thumb, duration, views, uploadDate));
                        }
                        if (!list.isEmpty()) {
                            return list;
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Piped trending failed for " + baseUrl + ": " + t.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private List<VideoItem> fetchPipedSearch(String query) {
        for (String baseUrl : PIPED_INSTANCES) {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                Request request = new Request.Builder()
                        .url(baseUrl + "/search?q=" + encodedQuery + "&filter=all")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody body = response.body();
                    if (response.isSuccessful() && body != null) {
                        JSONObject json = new JSONObject(body.string());
                        JSONArray items = json.optJSONArray("items");
                        if (items != null) {
                            List<VideoItem> list = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);
                                String type = obj.optString("type", "");
                                if ("channel".equalsIgnoreCase(type) || "playlist".equalsIgnoreCase(type)) {
                                    continue;
                                }
                                String rawUrl = obj.optString("url", "");
                                String videoUrl = rawUrl.startsWith("http") ? rawUrl : "https://www.youtube.com" + rawUrl;
                                String title = obj.optString("title", obj.optString("name", "Video"));
                                String thumb = obj.optString("thumbnail", "");
                                String uploader = obj.optString("uploaderName", "");
                                String avatar = obj.optString("uploaderAvatar", "");
                                long duration = obj.optLong("duration", 0);
                                long views = obj.optLong("views", 0);
                                String uploadDate = obj.optString("uploadedDate", "");

                                list.add(new VideoItem(videoUrl, title, videoUrl, uploader, avatar, thumb, duration, views, uploadDate));
                            }
                            if (!list.isEmpty()) {
                                return list;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Piped search failed for " + baseUrl + ": " + t.getMessage());
            }
        }
        return Collections.emptyList();
    }

    /**
     * Loads YouTube Trending/Home feed with multi-tier API redundancy:
     * Tier 1: Piped Bangladesh Trending REST API (fastest, guaranteed BD content).
     * Tier 2: NewPipeExtractor Kiosk.
     * Tier 3: Direct Bangladesh search query fallback.
     */
    public void getTrendingVideos(Page page, PaginatedCallback callback) {
        executor.execute(() -> {
            try {
                List<VideoItem> videoList = new ArrayList<>();
                Page nextPage = null;
                boolean hasMore = false;

                if (page == null) {
                    // Tier 1: Piped Bangladesh Trending API
                    List<VideoItem> pipedResults = fetchPipedTrendingBD();
                    if (!pipedResults.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(pipedResults, null, false));
                        return;
                    }

                    // Tier 2: NewPipeExtractor Kiosk
                    try {
                        String kioskId = ServiceList.YouTube.getKioskList().getDefaultKioskId();
                        KioskInfo kioskInfo = KioskInfo.getInfo(ServiceList.YouTube, kioskId);
                        for (InfoItem item : kioskInfo.getRelatedItems()) {
                            if (item instanceof StreamInfoItem) {
                                StreamInfoItem s = (StreamInfoItem) item;
                                videoList.add(new VideoItem(
                                        s.getUrl(), s.getName(), s.getUrl(),
                                        s.getUploaderName(), getAvatarUrl(s.getUploaderAvatars()),
                                        getImageUrl(s.getThumbnails()), s.getDuration(),
                                        s.getViewCount(), s.getTextualUploadDate()
                                ));
                            }
                        }
                        nextPage = kioskInfo.getNextPage();
                        hasMore = kioskInfo.hasNextPage() && nextPage != null;
                    } catch (Throwable t) {
                        Log.w(TAG, "Default kiosk failed, falling back to Bangladesh search: " + t.getMessage());
                    }

                    // Tier 3: Direct Bangladesh trending search query fallback
                    if (videoList.isEmpty()) {
                        SearchQueryHandler qh = ServiceList.YouTube.getSearchQHFactory().fromQuery("trending bangladesh");
                        SearchInfo searchInfo = SearchInfo.getInfo(ServiceList.YouTube, qh);
                        for (InfoItem item : searchInfo.getRelatedItems()) {
                            if (item instanceof StreamInfoItem) {
                                StreamInfoItem s = (StreamInfoItem) item;
                                videoList.add(new VideoItem(
                                        s.getUrl(), s.getName(), s.getUrl(),
                                        s.getUploaderName(), getAvatarUrl(s.getUploaderAvatars()),
                                        getImageUrl(s.getThumbnails()), s.getDuration(),
                                        s.getViewCount(), s.getTextualUploadDate()
                                ));
                            }
                        }
                        nextPage = searchInfo.getNextPage();
                        hasMore = searchInfo.hasNextPage() && nextPage != null;
                    }
                } else {
                    ListExtractor.InfoItemsPage<?> moreItems =
                            KioskInfo.getMoreItems(ServiceList.YouTube, ServiceList.YouTube.getKioskList().getDefaultKioskId(), page);
                    for (Object obj : moreItems.getItems()) {
                        if (obj instanceof StreamInfoItem) {
                            StreamInfoItem s = (StreamInfoItem) obj;
                            videoList.add(new VideoItem(
                                    s.getUrl(), s.getName(), s.getUrl(),
                                    s.getUploaderName(), getAvatarUrl(s.getUploaderAvatars()),
                                    getImageUrl(s.getThumbnails()), s.getDuration(),
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
                Log.e(TAG, "Failed to load trending videos", t);
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
     * Searches YouTube for videos using multi-tier API:
     * Tier 1: Piped Search REST API (super fast, no captcha blocks).
     * Tier 2: NewPipeExtractor native search.
     */
    public void searchYouTube(String query, Page page, PaginatedCallback callback) {
        executor.execute(() -> {
            try {
                if (query == null || query.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(), null, false));
                    return;
                }

                String cleanQuery = query.trim();

                // If user pasted a direct video link, extract directly and return
                if (cleanQuery.contains("youtube.com/watch") || cleanQuery.contains("youtu.be/")) {
                    try {
                        StreamInfo streamInfo = StreamInfo.getInfo(ServiceList.YouTube, cleanQuery);
                        List<VideoItem> singleList = new ArrayList<>();
                        singleList.add(new VideoItem(
                                streamInfo.getUrl(), streamInfo.getName(), streamInfo.getUrl(),
                                streamInfo.getUploaderName(), getAvatarUrl(streamInfo.getUploaderAvatars()),
                                getImageUrl(streamInfo.getThumbnails()), streamInfo.getDuration(),
                                streamInfo.getViewCount(), streamInfo.getTextualUploadDate()
                        ));
                        mainHandler.post(() -> callback.onSuccess(singleList, null, false));
                        return;
                    } catch (Throwable ignored) {
                        // fallback to standard search
                    }
                }

                if (page == null) {
                    // Tier 1: Piped Search REST API
                    List<VideoItem> pipedResults = fetchPipedSearch(cleanQuery);
                    if (!pipedResults.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(pipedResults, null, false));
                        return;
                    }
                }

                // Tier 2: NewPipeExtractor search
                SearchQueryHandler qh = ServiceList.YouTube.getSearchQHFactory().fromQuery(cleanQuery);
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
                                    s.getUploaderName(), getAvatarUrl(s.getUploaderAvatars()),
                                    getImageUrl(s.getThumbnails()), s.getDuration(),
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
                                    s.getUploaderName(), getAvatarUrl(s.getUploaderAvatars()),
                                    getImageUrl(s.getThumbnails()), s.getDuration(),
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

    private VideoDetail extractViaPipedStreams(String url) {
        String videoId = null;
        if (url.contains("v=")) {
            videoId = url.substring(url.indexOf("v=") + 2);
            int amp = videoId.indexOf('&');
            if (amp != -1) videoId = videoId.substring(0, amp);
        } else if (url.contains("youtu.be/")) {
            videoId = url.substring(url.indexOf("youtu.be/") + 9);
            int q = videoId.indexOf('?');
            if (q != -1) videoId = videoId.substring(0, q);
        }

        if (videoId == null || videoId.isEmpty()) return null;

        for (String baseUrl : PIPED_INSTANCES) {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/streams/" + videoId)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody body = response.body();
                    if (response.isSuccessful() && body != null) {
                        JSONObject json = new JSONObject(body.string());
                        VideoDetail detail = new VideoDetail();
                        detail.setId(url);
                        detail.setTitle(json.optString("title", "YouTube Video"));
                        detail.setDescription(json.optString("description", ""));
                        detail.setWebUrl(url);
                        detail.setUploaderName(json.optString("uploader", "YouTube Channel"));
                        detail.setUploaderAvatarUrl(json.optString("uploaderAvatar", ""));
                        detail.setDurationSeconds(json.optLong("duration", 0));
                        detail.setViewCount(json.optLong("views", 0));
                        detail.setUploadDate(json.optString("uploadDate", ""));
                        detail.setThumbnailUrl(json.optString("thumbnailUrl", ""));

                        List<StreamQuality> vq = new ArrayList<>();
                        JSONArray vsList = json.optJSONArray("videoStreams");
                        if (vsList != null) {
                            for (int i = 0; i < vsList.length(); i++) {
                                JSONObject vs = vsList.getJSONObject(i);
                                String sUrl = vs.optString("url", "");
                                String quality = vs.optString("quality", "Video");
                                String format = vs.optString("format", "mp4");
                                if (!sUrl.isEmpty()) {
                                    vq.add(new StreamQuality(
                                            format + "_" + quality,
                                            quality + " (" + format + ")",
                                            format,
                                            false,
                                            sUrl,
                                            0,
                                            vs.optInt("bitrate", 0)
                                    ));
                                }
                            }
                        }

                        List<StreamQuality> aq = new ArrayList<>();
                        JSONArray asList = json.optJSONArray("audioStreams");
                        if (asList != null) {
                            for (int i = 0; i < asList.length(); i++) {
                                JSONObject as = asList.getJSONObject(i);
                                String sUrl = as.optString("url", "");
                                String quality = as.optString("quality", "Audio");
                                String format = as.optString("format", "m4a");
                                if (!sUrl.isEmpty()) {
                                    aq.add(new StreamQuality(
                                            format + "_" + quality,
                                            "Audio (" + format.toUpperCase() + " " + quality + ")",
                                            format,
                                            true,
                                            sUrl,
                                            0,
                                            as.optInt("bitrate", 0)
                                    ));
                                }
                            }
                        }

                        detail.setVideoStreams(vq);
                        detail.setAudioStreams(aq);
                        return detail;
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Piped streams API error for " + baseUrl + ": " + t.getMessage());
            }
        }
        return null;
    }

    /**
     * Extracts full video details, streaming links, and downloadable formats.
     * Automatically routes to NewPipeExtractor with Piped API streams fallback.
     */
    public void extractVideoInfo(String url, VideoExtractionCallback callback) {
        executor.execute(() -> {
            try {
                String platform = AppUtils.detectPlatform(url);

                if ("YouTube".equals(platform)) {
                    VideoDetail detail = null;
                    try {
                        StreamInfo streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url);
                        detail = new VideoDetail();
                        detail.setId(streamInfo.getUrl());
                        detail.setTitle(streamInfo.getName());
                        detail.setDescription(streamInfo.getDescription() != null ? streamInfo.getDescription().getContent() : "");
                        detail.setWebUrl(streamInfo.getUrl());
                        detail.setUploaderName(streamInfo.getUploaderName());
                        detail.setUploaderAvatarUrl(getAvatarUrl(streamInfo.getUploaderAvatars()));
                        detail.setUploaderSubscriberCount(streamInfo.getUploaderSubscriberCount() != -1
                                ? AppUtils.formatViews(streamInfo.getUploaderSubscriberCount()) + " subscribers"
                                : "");
                        detail.setViewCount(streamInfo.getViewCount());
                        detail.setDurationSeconds(streamInfo.getDuration());
                        detail.setUploadDate(streamInfo.getTextualUploadDate());
                        detail.setThumbnailUrl(getImageUrl(streamInfo.getThumbnails()));

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
                                        getAvatarUrl(s.getUploaderAvatars()),
                                        getImageUrl(s.getThumbnails()),
                                        s.getDuration(),
                                        s.getViewCount(),
                                        s.getTextualUploadDate()
                                ));
                            }
                        }

                        detail.setVideoStreams(videoQualities);
                        detail.setAudioStreams(audioQualities);
                        detail.setRelatedVideos(related);
                    } catch (Throwable t) {
                        Log.w(TAG, "NewPipe stream extraction notice, attempting Piped streams API: " + t.getMessage());
                        detail = extractViaPipedStreams(url);
                    }

                    if (detail != null && (!detail.getVideoStreams().isEmpty() || !detail.getAudioStreams().isEmpty())) {
                        VideoDetail finalDetail = detail;
                        mainHandler.post(() -> callback.onSuccess(finalDetail));
                    } else {
                        mainHandler.post(() -> callback.onError(new Exception("Could not retrieve playable video streams")));
                    }
                } else {
                    // Non-YouTube: TikTok, Facebook, Instagram, Twitter/X
                    YtDlpDownloader.getInstance().extractInfo(url, new YtDlpDownloader.ExtractionCallback() {
                        @Override
                        public void onSuccess(VideoDetail detail) {
                            mainHandler.post(() -> callback.onSuccess(detail));
                        }

                        @Override
                        public void onError(Throwable error) {
                            Log.w(TAG, "MediaDownloader extraction fallback: " + error.getMessage());
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

    private static String getImageUrl(List<Image> images) {
        if (images != null && !images.isEmpty()) {
            Image img = images.get(images.size() - 1);
            if (img != null && img.getUrl() != null) {
                return img.getUrl();
            }
        }
        return "";
    }

    private static String getAvatarUrl(List<Image> images) {
        if (images != null && !images.isEmpty()) {
            Image img = images.get(0);
            if (img != null && img.getUrl() != null) {
                return img.getUrl();
            }
        }
        return "";
    }
}
