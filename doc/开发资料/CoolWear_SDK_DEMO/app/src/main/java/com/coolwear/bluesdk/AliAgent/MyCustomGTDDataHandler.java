package com.coolwear.bluesdk.AliAgent;

import com.alibaba.aliagentsdk.api.IAli2LpSender;
import com.alibaba.aliagentsdk.api.IGTDDataHandler;
import com.alibaba.aliagentsdk.domain.SendData;

import ce.com.cenewbluesdk.uitl.Logger;

/**
 * GTD通用数据的解析处理，默认走官方的处理方式，如果开发者自定义GTD里的数据和topic
 */
public class MyCustomGTDDataHandler implements IGTDDataHandler {
    /**
     * 来自蓝牙设备的GTD数据，开发者可以实现该方法，自行处理
     *
     * @param sendData 来自蓝牙的GTD数据
     * @param aliLp 可以通过aliLp里的sendId2Data的方法把数据发送到Lp平台
     */
    @Override
    public void handleGTD(SendData sendData, IAli2LpSender aliLp) {
        Logger.i("AliAgentSdkHelper", "来自蓝牙设备的GTD数据："+sendData.toString());
    }
    /**
     * 可以自定义处理来自后台的ID2数据
     *
     * @param topicFragment topic
     * @param msg 内容
     * @return 返回的信息将会以GTD的通道直接发送到蓝牙设备上
     */
    @Override
    public byte[] handleDispatchId2Data(String topicFragment, String msg) {
        Logger.i("AliAgentSdkHelper", "来自后台的ID2数据==>topic:"+topicFragment+"  msg:"+msg);
        return new byte[0];
    }
}
