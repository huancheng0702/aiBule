package com.coolwear.bluesdk.utils;


import android.content.Context;

import androidx.annotation.NonNull;

import com.coolwear.bluesdk.listenters.OnRxJavaBackProcessorListenter;
import com.coolwear.bluesdk.listenters.OnRxJavaRollPollingListenter;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @Author：zhengyunbin
 * @CreateDate：2020/9/4
 * @Description：
 */
public class RXJavaUtils {

    /**
     * 后台子线程执行，完成后发射到主线程执行
     *
     * @param onRxJavaBackProcessorListenter
     * @param <T>
     */
    public static <T> void backProcessor(Context context, final OnRxJavaBackProcessorListenter onRxJavaBackProcessorListenter) {
        Observable<T> observable = Observable
                .create(new ObservableOnSubscribe<T>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<T> emitter) throws Exception {
                        try{
                            if (onRxJavaBackProcessorListenter != null) {
                                onRxJavaBackProcessorListenter.subscribe(emitter);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }catch (Error error){
                            error.printStackTrace();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(T f) {
                if (onRxJavaBackProcessorListenter != null) {
                    onRxJavaBackProcessorListenter.onNext(f);
                }

            }

            @Override
            public void onError(Throwable e) {
                if (onRxJavaBackProcessorListenter != null) {
                    onRxJavaBackProcessorListenter.onError(e);
                }
            }

            @Override
            public void onComplete() {
            }
        });
    }


    /**
     * 轮询器
     */
    public static Disposable disposable;
    public static boolean refresh = true;
    public static int refreshFrequencySeconds = 30;

    public static Disposable rxStartRollPolling(final OnRxJavaRollPollingListenter onRxJavaRollPollingListenter) {
        try {
            disposable = Observable.interval(refreshFrequencySeconds, refreshFrequencySeconds, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            if (onRxJavaRollPollingListenter != null) {
                                onRxJavaRollPollingListenter.onRollPolling();
                            }
//                            if (UserBean.getInstance().isLogin() && refresh) {
//
//                            }else {
//                                rxStopRollPolling(disposable);
//                            }
                        }
                    });
            return disposable;
            // 有限制次数的轮询器
//        return Observable.intervalRange(0,10,0,1,TimeUnit.SECONDS)
//                .subscribe(new Consumer<Long>() {
//                    @Override
//                    public void accept(Long aLong) throws Exception {
//                        Log.i("ccb", "accept: "+aLong);
//                    }
//                });
        } catch (Exception e) {
            e.printStackTrace();
//            rxStopRollPolling(disposable);
            return null;
        } catch (Error e) {
            e.printStackTrace();
//            rxStopRollPolling(disposable);
            return null;
        }
    }

    public static void rxStopRollPolling(Disposable disposable) {
        try {
            if (disposable == null) return;
            if (!disposable.isDisposed()) disposable.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }
}
