package com.coolwear.bluesdk.tcp;


/**
 * Author:chenguirui
 * Date:2024/6/6 14:17
 * Description:
 */
public interface SendResultHandle {

    void sendMsgOk(Object msg) ;

    void sendMsgFail() ;

    void connectStatus(boolean isConnect);
}
