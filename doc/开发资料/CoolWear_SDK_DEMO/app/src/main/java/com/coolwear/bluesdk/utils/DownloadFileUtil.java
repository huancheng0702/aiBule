package com.coolwear.bluesdk.utils;


import android.util.Log;

import com.coolwear.bluesdk.listenters.OnDownLoadListener;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;


public class DownloadFileUtil {

    public static void execute(String url, String savePath, OnDownLoadListener downLoadListener) {
        RequestParams params = new RequestParams(url);
//        params.setSaveFilePath(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "myapp" + File.separator + "adf.zip");
        params.setSaveFilePath(savePath);
        params.setAutoRename(true);
        x.http().get(params, new Callback.ProgressCallback<File>() {
            @Override
            public void onSuccess(File file) {
                Log.i("DownloadFileUtil", "onSuccess");
                if (downLoadListener != null) {
                    downLoadListener.onSuccess(file);
                }
            }

            @Override
            public void onError(Throwable throwable, boolean isOnCallback) {
                Log.i("DownloadFileUtil", "onError");
                if (downLoadListener != null) {
                    downLoadListener.onError(throwable, isOnCallback);
                }
            }

            @Override
            public void onCancelled(CancelledException cex) {
                Log.i("DownloadFileUtil", "onCancelled");
            }

            @Override
            public void onFinished() {
                Log.i("DownloadFileUtil", "onFinished");
            }

            @Override
            public void onWaiting() {
                Log.i("DownloadFileUtil", "onWaiting");
            }

            @Override
            public void onStarted() {
                Log.i("DownloadFileUtil", "onStarted");
            }

            @Override
            public void onLoading(long total, long current, boolean isDownloading) {
                Log.i("DownloadFileUtil", "current=" + current + " total=" + total);
                if (downLoadListener != null) {
                    downLoadListener.onLoading(total, current, isDownloading);
                }
            }
        });
    }
}
