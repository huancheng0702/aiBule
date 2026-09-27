package com.coolwear.bluesdk.net;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * <p>作者：zhengyunbin<p>
 * <p>创建时间：2019/8/3 <p>
 * <p>文件描述：<p>
 */
public class AddHeaderInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request()
                .newBuilder()
                .addHeader("Content-Type", "application/json; charset=UTF-8")//以json方式提交
                .build();
        return chain.proceed(request);

    }
}
