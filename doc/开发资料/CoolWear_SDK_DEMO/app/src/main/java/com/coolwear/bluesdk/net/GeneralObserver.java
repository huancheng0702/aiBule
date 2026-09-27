package com.coolwear.bluesdk.net;

import android.content.Context;

/**
 * @Author：zhengyunbin
 * @CreateDate：2020/09/17
 * @Description：
 */
public abstract class GeneralObserver<T> extends BaseObserver<T> {

    public GeneralObserver(Context cxt, boolean isShowLoading) {
        super(cxt, isShowLoading);
    }

    @Override
    protected void reLogin(Context context) {

    }




    @Override
    protected void getAppVersion(Context context) {


    }
}
