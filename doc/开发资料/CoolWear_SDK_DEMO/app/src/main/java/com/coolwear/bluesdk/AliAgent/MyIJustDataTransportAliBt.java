package com.coolwear.bluesdk.AliAgent;

import com.alibaba.aliagentsdk.api.IJustDataTransportAliBt;
import com.alibaba.aliagentsdk.callback.IBtDataUploadCallback;
import com.alibaba.aliagentsdk.callback.ISend2BtCallback;

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.uitl.ByteUtil;
import ce.com.cenewbluesdk.uitl.Logger;

public class MyIJustDataTransportAliBt extends IJustDataTransportAliBt {
    public IBtDataUploadCallback iBtDataUploadCallback;


    @Override
    public void sendData(byte[] bytes, ISend2BtCallback iSend2BtCallback) {
        //TODO:处理数据发送到蓝牙设备上,至于怎么发送，自行处理，使用开发者已经实现好的就行
        try {
            Logger.i("AliAgentSdkHelper", "接收到数据长度：" + bytes.length);
            Logger.i("AliAgentSdkHelper", "接收到数据1：" + ByteUtil.byte2hex(bytes));
            Logger.i("AliAgentSdkHelper", "接收到数据2：" + new String(bytes,"UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        AliAgentSdkHelper.getInstance().analysisDataToBl(bytes);

    }

    @Override
    public void setBtDataUploadCallback(IBtDataUploadCallback iBtDataUploadCallback) {
        //TODO:如果蓝牙设备有数据要发送到后台，请调用iBtDataUploadCallback的onDataUpload方法
        Logger.i("AliAgentSdkHelper", "========setBtDataUploadCallback==========");
//        byte[] data = "{\"sid\":\"aaa\",\"data\":\"bbb\"}".getBytes(StandardCharsets.UTF_8);
//        iBtDataUploadCallback.onDataUpload(data);
        this.iBtDataUploadCallback = iBtDataUploadCallback;
    }

    public void sendDataToLp(byte[] data) {
        if (iBtDataUploadCallback != null) {
            Logger.i("AliAgentSdkHelper", "发送蓝牙数据到AL：长度"+data.length);
            iBtDataUploadCallback.onDataUpload(data);
        }
    }
}
