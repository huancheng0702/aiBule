package com.coolwear.bluesdk.listenters;

import java.io.File;

public interface OnDownLoadListener {
    void onSuccess(File result);
    void onError(Throwable ex, boolean isOnCallback);
    void onLoading(long total, long current, boolean isDownloading);
}
