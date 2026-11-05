package com.tubepipe.app.data.extractor;

import androidx.annotation.NonNull;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Custom OkHttp-based Downloader implementation required by NewPipeExtractor.
 */
public class NewPipeDownloader extends Downloader {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final OkHttpClient client;

    public NewPipeDownloader() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public NewPipeDownloader(OkHttpClient client) {
        this.client = client;
    }

    @NonNull
    @Override
    public Response execute(@NonNull Request request) throws IOException {
        String httpMethod = request.httpMethod();
        String url = request.url();
        Map<String, List<String>> headers = request.headers();
        byte[] dataToSend = request.dataToSend();

        RequestBody body = null;
        if (dataToSend != null) {
            String contentType = request.headers() != null && request.headers().containsKey("Content-Type")
                    ? request.headers().get("Content-Type").get(0)
                    : "application/json";
            body = RequestBody.create(dataToSend, MediaType.parse(contentType));
        }

        Headers.Builder okHeadersBuilder = new Headers.Builder();
        okHeadersBuilder.add("User-Agent", USER_AGENT);

        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String value : entry.getValue()) {
                    okHeadersBuilder.add(entry.getKey(), value);
                }
            }
        }

        okhttp3.Request okRequest = new okhttp3.Request.Builder()
                .url(url)
                .headers(okHeadersBuilder.build())
                .method(httpMethod, body)
                .build();

        try (okhttp3.Response okResponse = client.newCall(okRequest).execute()) {
            ResponseBody okBody = okResponse.body();
            String responseBodyString = okBody != null ? okBody.string() : "";

            Map<String, List<String>> responseHeaders = new HashMap<>();
            for (String name : okResponse.headers().names()) {
                responseHeaders.put(name, okResponse.headers().values(name));
            }

            return new Response(
                    okResponse.code(),
                    okResponse.message(),
                    responseHeaders,
                    responseBodyString,
                    okResponse.request().url().toString()
            );
        }
    }
}
