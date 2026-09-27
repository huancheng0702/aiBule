package com.coolwear.bluesdk.tcp;

import android.os.SystemClock;

import org.xutils.common.util.LogUtil;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import ce.com.cenewbluesdk.uitl.JsonUtil;
import ce.com.cenewbluesdk.uitl.Logger;

/**
 * Author:chenguirui
 * Date:2024/6/5 18:13
 * Description:
 */
public class TcpClient {
    private static TcpClient instance;
    private final String host = "127.0.0.1";
    private final int port = 31001;
    private Socket socket;
    public DataOutputStream out;
    private DataInputStream dataInputStream;
    private PrintWriter pw;
    private String revMsg = "";
    private static final String TAG = "TCP";
    private static long oldPongTime = System.currentTimeMillis();
    private byte[] dataBytes;
    private byte[] lenbuff = new byte[4];
    private HeartThread heartThread = null;

    private TcpClient() {
    }

    public static TcpClient getInstance() {
        if (instance == null) {
            synchronized (TcpClient.class) {
                if (instance == null) {
                    instance = new TcpClient();
                }
            }
        }
        return instance;
    }

    public void connect() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    socket = new Socket(host, port);
                    out = new DataOutputStream(socket.getOutputStream());
                    pw = new PrintWriter(out, true);
                    dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                   // sendMessage(JsonUtils.getInstance().formtGetJsonCommon("login"));
                    SystemClock.sleep(1000);
                   // sendMessage(JsonUtils.getInstance().formtGetJsonCommon("ping"));
                    sendHeart();
                    int totalLen = 0;
                    ByteBuffer buffer;
                    while (true) {
                        if (dataInputStream == null) {
                            Thread.interrupted();
                            Logger.e("tcp", "==soket in is null");
                            break;
                        }
                        dataInputStream.read(lenbuff);
                        buffer = ByteBuffer.wrap(lenbuff);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);//小端
                        totalLen = buffer.getInt();
                        Logger.e("tcp", "==totalLen==" + Arrays.toString(lenbuff) + "==len==" + totalLen);
                        if (totalLen == 0) {
                            reconnect();
                            break;
                        }
                        dataBytes = new byte[totalLen];
                        dataInputStream.readFully(dataBytes);
                        revMsg = new String(dataBytes);
                        Arrays.fill(dataBytes, (byte) 0); //清空缓存区
//                        if (revMsg.contains("pong")) {
//                            oldPongTime = System.currentTimeMillis();
//                        } else {
//                            JsonUtils.getInstance().parseResultJson(revMsg);
//                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public synchronized void sendMessage(String message) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    //pw.println(message);
                    out.writeBytes(message + "\n");
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void sendHeart() {
        if (heartThread != null && !heartThread.isInterrupted()) {
            heartThread.interrupt();
            heartThread = null;
        }
        heartThread = new HeartThread();
        heartThread.start();
    }

    class HeartThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(20 * 1000);
                    if (pw == null) {
                        Logger.e("tcp","send heart pw is null,so break");
                        break;
                    }
                   // pw.println(JsonUtils.getInstance().formtGetJsonCommon("ping"));
                   // out.writeBytes(JsonUtil.fromJsonObject());
                    out.flush();
                    if (System.currentTimeMillis() - oldPongTime > 40 * 1000) {
                        //TCP 重连
                        TcpClient.getInstance().reconnect();
                        // Thread.interrupted();
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }catch (IOException e){}
            }
        }
    }

    public void reconnect() {
        Logger.e("tcp", "===reconnect==");
        if (dataInputStream != null) {
            try {
                dataInputStream.close();
                dataInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (out != null) {
            try {
                out.close();
                out = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (pw != null) {
            pw.close();
            pw = null;
        }
        connect();

    }
}