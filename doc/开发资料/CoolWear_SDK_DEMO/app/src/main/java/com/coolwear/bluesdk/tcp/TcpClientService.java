package com.coolwear.bluesdk.tcp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


public class TcpClientService extends Service {
    public static final String GS_IS_CHECKED_KEY = "GS_IS_CHECKED";
    public static final String SERVER_TYPE_KEY = "SERVER_TYPE";
    public static final String CHECK_DATA_TYPE_KEY = "CHECK_DATA_TYPE";
    //private static final String LOCAL_NET = "192.168.101.6";//局域网
    private static final String LOCAL_NET = "192.168.101.252";//局域网
    private static final String GLOBAL_NET = "47.107.88.95";//外网
    private static final int PORT_LOCAL = 1111;
    private static final int PORT_GLOBAL = 1111;
    private ConnectionThread thread;
    private int serverType = 1;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            serverType = bundle.getInt(TcpClientService.SERVER_TYPE_KEY);
        }
        thread = new ConnectionThread("tcp", getApplicationContext());
        thread.start();
        Log.e(SessionManager.TAG, "start thread to connect");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.disConnect();
        thread = null;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void reConnect() {
        thread.start();
    }

    class ConnectionThread extends HandlerThread {
        boolean isConnected;
        ConnectionManager manager;

        public ConnectionThread(String name, Context context) {
            super(name);
            ConnectionConfig config = new ConnectionConfig.Builder(context)
                    .setIp(serverType == 1 ? LOCAL_NET : GLOBAL_NET).setPort(serverType == 1 ? PORT_LOCAL : PORT_GLOBAL)
                    .setReadBufferSize(10240).setConnectionTimeout(10000)
                    .builder();
            manager = new ConnectionManager(config);
        }

        @Override
        protected void onLooperPrepared() {
            while (true) {
                Log.e(SessionManager.TAG, "onLooperPrepared");
                isConnected = manager.connect();
                if (isConnected) {
                    Log.e(SessionManager.TAG, "connect successfully.");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            manager.sendCallBack(ConnectionManager.CONNECT_SUCC_CODE + "", "connect successfully");
                        }
                    }, 1 * 1000);

                    break;
                }
                try {
                    Log.e(SessionManager.TAG, "connect fail.");
                    Thread.sleep(10*1000);
                    manager.sendCallBack(ConnectionManager.CONNECT_ERROR_CODE + "", "connect fail");
                } catch (Exception e) {
                    Log.e(SessionManager.TAG, "fail with error");
                }
            }
        }

        public void disConnect() {
            manager.disConnect();
            SessionManager.getInstance().closeSession();
            SessionManager.getInstance().removeSession();
            SessionManager.getInstance().closeHeartBeat();
        }
    }
}
