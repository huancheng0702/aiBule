package com.coolwear.bluesdk.AliAgent;

import android.content.Context;

import com.alibaba.aliagentsdk.AliAgentSdk;
import com.alibaba.aliagentsdk.api.IAliAgent;
import com.alibaba.aliagentsdk.callback.IConnectCallback;
import com.alibaba.aliagentsdk.callback.IFgsStateCheckCallback;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.proxy.BleFactory;
import ce.com.cenewbluesdk.uitl.ByteUtil;
import ce.com.cenewbluesdk.uitl.Logger;

public class AliAgentSdkHelper {

    private static final String TAG = "AliAgentSdkHelper";
    private AliAgentSdk aliAgentSdk;
    private IAliAgent agent;
    private MyIJustDataTransportAliBt myIJustDataTransportAliBt;
    private MyCustomGTDDataHandler myCustomGTDDataHandler;
    private volatile boolean isIOTServerConnect;


    public static AliAgentSdkHelper getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final AliAgentSdkHelper INSTANCE = new AliAgentSdkHelper();
    }

    public void init(Context context) {
        if (aliAgentSdk == null) {
            aliAgentSdk = AliAgentSdk.getInstance();
            //init first,true表示有日志输出，false表示不输出日志
            aliAgentSdk.init(context, true);
        }
        initSendAlQueueTask();
//        initSendBlQueueTask();
    }

    /**
     * 如果是开发调试，可以调用开发的测试接口
     */
    public void setAgentDev() {
        if (agent == null) {
            agent = aliAgentSdk.getAgentDev();
        }
    }

    /**
     * 如果是app接口则是调用
     */
    public void setAgent() {
        if (agent == null) {
            agent = aliAgentSdk.getAgent();
        }
    }

    public void customBtImpl() {
        if (myIJustDataTransportAliBt == null) {
            myIJustDataTransportAliBt = new MyIJustDataTransportAliBt();
        }
        if(agent==null)return;
        agent.customBtImpl(myIJustDataTransportAliBt);
    }

    public void setCustomGTDDataHandler() {
        if (myCustomGTDDataHandler == null) {
            myCustomGTDDataHandler = new MyCustomGTDDataHandler();
        }
        if(agent==null)return;
        agent.customGTDDataHandler(myCustomGTDDataHandler);
    }

    /**
     * 解析收到AL的数据
     * 每条数据最长为285，去掉头x0F0F0和数据长度
     * @param data
     */
    int po = 0;
    byte[] toBlData;

    public synchronized void analysisDataToBl(byte[] data) {

        if (po == 0) {
            po++;
            if (data.length > 281) {
                toBlData = new byte[285];
            } else {
                toBlData = new byte[data.length+4];
            }
            //61680 => x0f0f0
            System.arraycopy(ByteUtil.int2bytes2(61680), 0, toBlData, 0, 2);
            System.arraycopy(ByteUtil.int2bytes2(data.length), 0, toBlData, 2, 2);
            if (data.length > 281) {
                System.arraycopy(data, 0, toBlData, 4, 281);
                try {
                    Logger.i("AliAgentSdkHelper", "接收到数据3：" + new String(toBlData,"UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                BleFactory.getInstance().getK6Proxy().getSendHelper().sendAlData(toBlData);
                Logger.i("AliAgentSdkHelper","递归1");
                byte[] bytes = new byte[data.length - 281];
                System.arraycopy(data, 281, bytes, 0, data.length - 281);
                analysisDataToBl(bytes);
            } else {
                System.arraycopy(data, 0, toBlData, 4, data.length);
                try {
                    Logger.i("AliAgentSdkHelper", "接收到数据4：" + new String(toBlData,"UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                BleFactory.getInstance().getK6Proxy().getSendHelper().sendAlData(toBlData);
                po = 0;
            }
        } else {
            if (data.length > 285) {
                toBlData = new byte[285];
                System.arraycopy(data, 0, toBlData, 0, 285);
                try {
                    Logger.i("AliAgentSdkHelper", "接收到数据5：" + new String(toBlData,"UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                BleFactory.getInstance().getK6Proxy().getSendHelper().sendAlData(toBlData);
                Logger.i("AliAgentSdkHelper","递归2");
                byte[] bytes = new byte[data.length - 285];
                System.arraycopy(data, 285, bytes, 0, data.length - 285);
                analysisDataToBl(bytes);

            } else {
                try {
                    Logger.i("AliAgentSdkHelper", "接收到数据6：" + new String(toBlData,"UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                BleFactory.getInstance().getK6Proxy().getSendHelper().sendAlData(data);
                po = 0;
            }
        }
    }

    byte[] bytes;
    int i = 0;
    int pgSize;

    /**
     * 解析收到蓝牙的数据
     *
     * @param data
     */
    public synchronized void analysisDataToLp(byte[] data) {
        //标志位
        int flag = ByteUtil.byte2ToInt(new byte[]{data[0], data[1]});//61680
        if (flag == 61680) {//判断标志位
            //实际数据包大小
            pgSize = ByteUtil.byte2ToInt(new byte[]{data[2], data[3]});
            //当前的数据包大小
            int currentSize = data.length - 4;
            i = pgSize - currentSize;
            if (i == 0) {//说明已经是一个完整的包，可直接发送
                byte[] bytes = new byte[pgSize];
                System.arraycopy(data, 4, bytes, 0, pgSize);
                sendAlQueue.offer(bytes);
            } else if (i < 0) {//说明有多个包，需要截取
                byte[] bytes = new byte[pgSize];
                System.arraycopy(data, 4, bytes, 0, pgSize);
                sendAlQueue.offer(bytes);

                byte[] tempData = new byte[data.length - (pgSize + 4)];
                System.arraycopy(data, pgSize + 4, tempData, 0, data.length - (pgSize + 4));
                Logger.i("AliAgentSdkHelper", "递归1");
                try {
                    Logger.i("AliAgentSdkHelper", "递归1的数据：" + new String(tempData, "UTF-8"));
                    Logger.i("AliAgentSdkHelper", "递归1的数据：" + ByteUtil.byte2hex(tempData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                analysisDataToLp(tempData);
            } else {//说明包不完整，等待下一个包
                bytes = new byte[pgSize];
                try {
                    Logger.i("AliAgentSdkHelper", "收到蓝牙实际数据(不完整1+" + pgSize + "+)：" + new String(data, "UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.arraycopy(data, 4, bytes, 0, currentSize);
                pgSize = pgSize - currentSize;//还剩余多少包要放到数组中
                Logger.i("AliAgentSdkHelper", "1还剩：" + pgSize + "包");
            }
        } else {
            if (pgSize > data.length) {//如果当前的包大小比剩余的数据包大小还小，说明还是一个不完整的包，继续等待下一个包
                try {
                    Logger.i("AliAgentSdkHelper", "收到蓝牙实际数据(不完整2+" + pgSize + "+)：" + new String(data, "UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.arraycopy(data, 0, bytes, returnActualLength(bytes), data.length);
                pgSize = pgSize - data.length;//还剩余多少包要放到数组中
                Logger.i("AliAgentSdkHelper", "2还剩：" + pgSize + "包");
            } else if (pgSize == data.length) {
                try {
                    Logger.i("AliAgentSdkHelper", "收到蓝牙实际数据2：" + new String(data, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                //说明已是最后一个包
                System.arraycopy(data, 0, bytes, returnActualLength(bytes), pgSize);
                sendAlQueue.offer(bytes);
            } else {
                System.arraycopy(data, 0, bytes, returnActualLength(bytes), pgSize);
                byte[] sendData = new byte[bytes.length];
                System.arraycopy(bytes, 0, sendData, 0, bytes.length);

                byte[] tempData = new byte[data.length - pgSize];
                System.arraycopy(data, pgSize, tempData, 0, data.length - pgSize);

                sendAlQueue.offer(sendData);
                Logger.i("AliAgentSdkHelper", "递归2");
                try {
                    Logger.i("AliAgentSdkHelper", "递归2的数据：" + new String(tempData, "UTF-8"));
                    Logger.i("AliAgentSdkHelper", "递归2的数据：" + ByteUtil.byte2hex(tempData));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                analysisDataToLp(tempData);
            }
        }
    }

    public static int returnActualLength(byte[] data) {
        int i = 0;
        for (; i < data.length; i++) {
            if (data[i] == '\0')
                break;
        }
        return i;
    }


    /**
     * 检查飞鸽书(三元组信息)
     */
    public void checkFgsState() {
        Logger.i(TAG, "检查飞鸽书");
        if(agent==null)return;
        agent.checkFgsState(new IFgsStateCheckCallback() {
            /**
             * 飞鸽书检查结果
             */
            @Override
            public void onFgsCheckSuccess() {
                Logger.i(TAG, "onFgsCheckSuccess");
                //检查成功，连接IOT服务器
                connectIOTServer();
            }

            /**
             * 检查出错
             * @param msg 错误消息
             * @param code 错误码
             */

            @Override
            public void onFgsCheckError(String msg, int code) {
                Logger.i(TAG, "onFgsCheckError: msg=" + msg + "  code=" + code);
            }
        });
//        connectIOTServer();
    }

    public void connectIOTServer() {
        if (null == agent) return;
        if (!isIOTServerConnect) {
            agent.connectLp();
        }
    }

    /**
     * 连接之前，先设置连接状态的回调接口
     */
    public void setIotConnectedCallback() {
        if(agent==null)return;
        agent.setLpConnectedCallback(new IConnectCallback() {
            //Iot连接断开
            @Override
            public void onIotDisconnected() {
                Logger.i(TAG, "onIotDisconnected");
                isIOTServerConnect = false;
            }

            //Iot连接成功
            @Override
            public void onIotConnected() {
                Logger.i(TAG, "onIotConnected");
                isIOTServerConnect = true;
            }

            /**
             * Iot连接失败
             * @param msg 失败信息
             * @param code 错误码
             */
            @Override
            public void onIotConnectFailure(String msg, int code) {
                Logger.i(TAG, "onIotConnectFailure: msg=" + msg + "  code=" + code);
                isIOTServerConnect = false;
            }
        });

    }

    /**
     * 开发者主动断开与LP的连接
     * 断开连接的结果，会在连接的回调里通知
     */

    public void disconnectLp() {
        if (null == agent) return;
        Logger.d(TAG, "disconnectIOTServer : isIOTServerConnect = " + isIOTServerConnect);
        if (isIOTServerConnect) {
            try {
                agent.disconnectLp();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isIOTServerConnect = false;
            }
        }
    }


    private TackSendAL tackSendAL;
    private ArrayBlockingQueue<byte[]> sendAlQueue = new ArrayBlockingQueue<byte[]>(2000, true);
    private ExecutorService executorAlService;

    private void initSendAlQueueTask() {
        if (tackSendAL == null) {
            tackSendAL = new TackSendAL();
            tackSendAL.setRunning(true);
        }
        if (executorAlService == null) {
            executorAlService = Executors.newSingleThreadExecutor();
        }
        executorAlService.execute(tackSendAL);
    }

    private class TackSendAL implements Runnable {

        private boolean isRunning;

        public TackSendAL() {
            executorAlService = Executors.newSingleThreadExecutor();
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Logger.i(TAG, "等待接收蓝牙的数据...");
                    byte[] alData = sendAlQueue.take();
                    try {
                        Logger.i("AliAgentSdkHelper", "收到蓝牙实际数据1：" + ByteUtil.byte2hex(alData));
                        Logger.i("AliAgentSdkHelper", "收到蓝牙实际数据2：" + new String(alData, "UTF-8"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (alData.length > 0) {
                        if (myIJustDataTransportAliBt != null) {
                            Logger.i(TAG, "发送蓝牙数据到AL...");
                            myIJustDataTransportAliBt.sendDataToLp(alData);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    private TackSendBL tackSendBL;
//    private ArrayBlockingQueue<byte[]> sendBlQueue = new ArrayBlockingQueue<byte[]>(2000, true);
//    private ExecutorService executorBlService;
//
//    private void initSendBlQueueTask() {
//        if (tackSendBL == null) {
//            tackSendBL = new TackSendBL();
//        }
//        if (executorBlService == null) {
//            executorBlService = Executors.newSingleThreadExecutor();
//        }
//        executorBlService.execute(tackSendBL);
//    }
//
//    private class TackSendBL implements Runnable {
//        private boolean isRunning;
//
//        public TackSendBL() {
//            executorAlService = Executors.newSingleThreadExecutor();
//        }
//
//        public boolean isRunning() {
//            return isRunning;
//        }
//
//        public void setRunning(boolean running) {
//            isRunning = running;
//        }
//
//        @Override
//        public void run() {
//            while (isRunning) {
//
//            }
//        }
//    }
}
