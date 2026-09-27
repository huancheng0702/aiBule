package com.coolwear.bluesdk.net;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitFactory {
    private static RetrofitFactory mRetrofitFactory;
    private Retrofit mRetrofit;
    private OkHttpClient.Builder mOkHttpBuilder;
    private OkHttpClient mOkHttpClient;
    private final String crtName = "general.cer";

    private RetrofitFactory() {
//        try {
//            sslParams = HttpsUtils.getSslSocketFactory(BaseApplication.getInstance().getAssets().open(crtName));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        HttpsTrustManager httpsTrustManager = new HttpsTrustManager();
        mOkHttpClient = getUnsafeOkHttpClient();  // build on your own
//        mOkHttpBuilder = new OkHttpClient.Builder()
//                .connectTimeout(HttpConfig.HTTP_TIME, TimeUnit.MILLISECONDS)
//                .readTimeout(HttpConfig.HTTP_TIME, TimeUnit.MILLISECONDS)
//                .writeTimeout(HttpConfig.HTTP_TIME, TimeUnit.MILLISECONDS)
//                .followRedirects(true)
//                .followSslRedirects(true)
//                .retryOnConnectionFailure(true)
//                .cache(null)
////                .sslSocketFactory(sslParams.sSLSocketFactory,sslParams.trustManager)
////                .sslSocketFactory(new SSLSocketFactoryCompat(httpsTrustManager),httpsTrustManager)
////                .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(),new HttpsTrustManager())
////                .sslSocketFactory(sslParams.sSLSocketFactory,sslParams.trustManager)
////                .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
//                .addInterceptor(new AddHeaderInterceptor())//添加头信息拦截器
////                .addInterceptor(new CommonParameterInterceptor(BaseApplication.getInstance()))//添加公共参数拦截器
//                .addInterceptor(new CommonParameterInterceptor())//添加公共参数拦截器
//                .addInterceptor(new HttpLoggingInterceptor(HttpConfig.TAG));//添加日志拦截器
////        mOkHttpBuilder = enableTls12OnPreLollipop(mOkHttpBuilder);
//        mOkHttpClient = mOkHttpBuilder.build();

        mRetrofit = new Retrofit.Builder()
                .baseUrl(HttpConfig.HTTP_CLOUDURL)//测试与线上环境切换
                .addConverterFactory(GsonConverterFactory.create())//添加gson转换器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//添加rxjava转换器
                .client(mOkHttpClient)
                .build();

    }
    public OkHttpClient getUnsafeOkHttpClient() {
        try {

//            final SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, new TrustManager[] { httpsTrustManager },
//                    new java.security.SecureRandom());
//            final SSLSocketFactory sslSocketFactory = new Tls12SocketFactory(sslContext
//                    .getSocketFactory());
            OkHttpClient okHttpClient = new OkHttpClient()
                    .newBuilder()
                    .connectTimeout(HttpConfig.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(HttpConfig.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(HttpConfig.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .cache(null)
//                    .sslSocketFactory(factory)
//                    .sslSocketFactory(new SSLSocketFactoryCompat(httpsTrustManager),httpsTrustManager)
////                  .sslSocketFactory(sslParams.sSLSocketFactory,sslParams.trustManager)
////                  .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(),new HttpsTrustManager())
////                  .sslSocketFactory(sslParams.sSLSocketFactory,sslParams.trustManager)
//                    .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
                    .addInterceptor(new AddHeaderInterceptor())//添加头信息拦截器
////                  .addInterceptor(new CommonParameterInterceptor(BaseApplication.getInstance()))//添加公共参数拦截器
//                    .addInterceptor(new CommonParameterInterceptor())//添加公共参数拦截器
                    .addInterceptor(new BaseUrlInterceptor())//地址切换拦截器
                    .addInterceptor(new HttpLoggingInterceptor(HttpConfig.TAG))//添加日志拦截器
////        mOkHttpBuilder = enableTls12OnPreLollipop(mOkHttpBuilder);
                    .build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public static RetrofitFactory getInstence() {
        if (mRetrofitFactory == null) {
            synchronized (RetrofitFactory.class) {
                if (mRetrofitFactory == null)
                    mRetrofitFactory = new RetrofitFactory();
            }

        }
        return mRetrofitFactory;
    }

    public <T> T API(Class<T> clazz) {
        return mRetrofit.create(clazz);
    }

}
