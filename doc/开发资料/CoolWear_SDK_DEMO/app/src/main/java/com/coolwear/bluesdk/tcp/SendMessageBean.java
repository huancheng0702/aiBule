package com.coolwear.bluesdk.tcp;

/**
 * Author:chenguirui
 * Date:2024/6/6 11:01
 * Description:
 */
public class SendMessageBean {
    public static final String UserInfoData = "C2SUserInfo";
    public static final String HeartbeatData = "Heartbeat";
    public static final String GsensorData = "C2SGsensor";
    public static final String HeartRateData = "C2SHeartRate";
    public static final String BatteryData = "C2SBattery";
    private String ActionName;
    private String Message;
    public SendMessageBean() {

    }
    public SendMessageBean(String actionName, String message) {
        ActionName = actionName;
        Message = message;
    }

    public String getActionName() {
        return ActionName;
    }

    public void setActionName(String actionName) {
        ActionName = actionName;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }
}
