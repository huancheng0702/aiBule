package com.coolwear.bluesdk.tcp;


import android.util.Log;

import com.coolwear.bluesdk.BuildConfig;

import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import ce.com.cenewbluesdk.uitl.JsonUtil;


public class SessionManager {
    private static SessionManager instance = null;
    public static String TAG = "SOCKET_TPC";
    private IoSession session;
    private HeartThread heartThread = null;
    private SendResultHandle handle;
    private boolean openHeartBeat = true;

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void setHandle(SendResultHandle handle) {
        this.handle = handle;
    }

    private SessionManager() {
    }

    public void setSession(IoSession session) {
        this.session = session;
    }

    public void writeToServer(Object msg) {
        if (session != null) {
            //Log.e(TAG, "ready to send message:" + msg);
            if (session.isConnected()) {
                DefaultWriteFuture future = (DefaultWriteFuture) session.write(msg + "\r\n");
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "是否发送成功:" + (future.getException() == null) + "  message：" + msg);
                }
                if (handle != null) {
                    if (future.getException() == null) {
                        handle.sendMsgOk(msg);
                    } else {
                        handle.sendMsgFail();
                    }
                    handle.connectStatus(future.getSession().isConnected());
                }
            } else {
                Log.e(TAG, "服务器未连接:");
                if (handle != null) {
                    handle.sendMsgFail();
                }
            }
        } else {
            Log.e(TAG, "ready to send message fai:" + msg);
        }
    }

    public void closeSession() {
        if (session != null) {
            session.closeOnFlush();
        }
    }

    public void removeSession() {
        this.session = null;
    }

    public void sendHeart() {
        if (heartThread != null && !heartThread.isInterrupted()) {
            heartThread.interrupt();
            heartThread = null;
        }
        heartThread = new HeartThread();
        openHeartBeat = true;
        heartThread.start();
    }

    public void closeHeartBeat() {
        openHeartBeat = false;
    }

    class HeartThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (openHeartBeat) {
                try {
                    Thread.sleep(20 * 1000);
                    SendMessageBean body = new SendMessageBean(SendMessageBean.HeartbeatData, "");
                    writeToServer(JsonUtil.beanToJson(body));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
