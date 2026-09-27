package com.coolwear.bluesdk.net;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.utils.FitLoader;
import com.coolwear.bluesdk.utils.ToastUtil;
import com.coolwear.bluesdk.utils.WordUtil;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import ce.com.cenewbluesdk.uitl.Lg;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;


public abstract class BaseObserver<T> implements Observer<BaseEntity<T>> {
    private static final String TAG = "BaseObserver";
    protected Context context;

    public BaseObserver(Context cxt) {
        this.context = cxt;
    }
    private boolean isShowProgress;
    public BaseObserver(Context cxt, boolean isShowProgress) {
        this.context = cxt;
        this.isShowProgress = isShowProgress;
    }

    @Override
    public void onSubscribe(Disposable d) {
        startHandler();
        onRequestStart();
    }
    Handler handler = new Handler(Looper.getMainLooper());
    private void startHandler() {
        handler.removeCallbacks(netRequestRun);
        handler.postDelayed(netRequestRun, HttpConfig.HTTP_TIMEOUT);
    }
    private void clearHandler() {
        handler.removeCallbacks(netRequestRun);
    }
    private NetRequestRun netRequestRun = new NetRequestRun();
    private class NetRequestRun implements Runnable {
        @Override
        public void run() {
            FitLoader.stopLoading(context);
            ToastUtil.show("Request timeout");
        }
    }
    @Override
    public void onNext(BaseEntity<T> tBaseEntity) {
        clearHandler();
        onRequestEnd(context);
        if (tBaseEntity.isSuccess()) {
            try {
                onSuccess(tBaseEntity.getData());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                onCodeError(tBaseEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(Throwable e) {
//        Log.w(TAG, "onError: ", );这里可以打印错误信息
        clearHandler();
        onRequestEnd(context);
        try {
            if (e instanceof ConnectException
                    || e instanceof TimeoutException
                    || e instanceof NetworkErrorException
                    || e instanceof UnknownHostException) {
                //网络异常
                ToastUtil.show(WordUtil.getString(R.string.Comment_netWorkError));
                onFailure(e, true);
            } else {
                //ToastUtil.show(e.getMessage());//如404等错误
                onFailure(e, false);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void onComplete() {
        onRequestEnd(context);
    }

    /**
     * 返回成功
     *
     * @param t
     */
    protected abstract void onSuccess(T t);

    /**
     * 返回成功了,但是code错误
     *
     * @param t
     */
    protected void onCodeError(BaseEntity<T> t) {
        if (t.getState() == ResponseCode.NETWORK_ERROR.getValue()) {//网络错误
            ToastUtil.show(WordUtil.getString(R.string.Comment_netWorkError));
        }else{//其它错误,弹Toast提示用户
            ToastUtil.show(t.getMessage());
        }
        return;
    }

    protected abstract void reLogin(Context context);
    protected abstract void getAppVersion(Context context);

    /**
     * 返回失败
     *
     * @param e
     * @param isNetWorkError 是否是网络错误
     */
    protected abstract void onFailure(Throwable e, boolean isNetWorkError);

    protected void onRequestStart() {
        if(isShowProgress){
            showProgressDialog();
        }
    }

    protected void onRequestEnd(Context context) {
        closeProgressDialog(context);
    }

    public void showProgressDialog() {
        FitLoader.showLoading(context);
//        ProgressDialogUtil.show(mContext, false, mContext.getResources().getString(R.string.please_wait));
    }

    public void closeProgressDialog(Context context) {
        FitLoader.stopLoading(context);
//        ProgressDialogUtil.cancle();
    }
}
