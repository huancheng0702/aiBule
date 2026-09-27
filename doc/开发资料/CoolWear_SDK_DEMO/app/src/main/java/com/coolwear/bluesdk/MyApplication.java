package com.coolwear.bluesdk;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

import org.xutils.x;

import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.proxy.sdkhelper.Bluetooth_5SDK_Processor;
import ce.com.cenewbluesdk.uitl.Logger;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/17 0017 10:53
 */
public class MyApplication extends Application {
    private static MyApplication sInstance;
    public static boolean isInit;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.e("MyApplication", "onCreate");
        if (!isInit) {
            Logger.e("MyApplication", "onCreate_1");
            BluetoothHelper.getInstance().init();
            BluetoothHelper.getInstance().initProxy(this);
            x.Ext.init(this);
            CrashReport.initCrashReport(getApplicationContext(), "02a30fc849", false);
        }
        isInit = true;
    }

    public static MyApplication getInstance() {
        return sInstance;
    }
}
