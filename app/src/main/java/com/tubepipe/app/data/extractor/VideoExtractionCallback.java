package com.tubepipe.app.data.extractor;

import com.tubepipe.app.data.model.VideoDetail;

public interface VideoExtractionCallback {
    void onSuccess(VideoDetail videoDetail);
    void onError(Throwable throwable);
}
