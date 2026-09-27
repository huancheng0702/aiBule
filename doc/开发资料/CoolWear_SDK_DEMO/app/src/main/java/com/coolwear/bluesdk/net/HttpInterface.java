package com.coolwear.bluesdk.net;


import com.coolwear.bluesdk.bean.DevVersionBean;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface HttpInterface {

    String URL_1="url_name:url_1";
    String URL_2="url_name:url_2";

    @Headers({URL_1})
    @GET("Ota/VersionList")
    Observable<BaseEntity<DevVersionBean>> versionList(@Query("customerId") String customerId,
                                                       @Query("password") String password);
}
