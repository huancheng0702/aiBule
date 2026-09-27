package com.coolwear.bluesdk.listenters;

import io.reactivex.ObservableEmitter;

/**
 * @Author：zhengyunbin
 * @CreateDate：2020/9/4
 * @Description：
 */
public interface OnRxJavaBackProcessorListenter<T> {
    void subscribe(ObservableEmitter<T> emitter);

    void onNext(T f);
    void onError(Throwable e);
}
