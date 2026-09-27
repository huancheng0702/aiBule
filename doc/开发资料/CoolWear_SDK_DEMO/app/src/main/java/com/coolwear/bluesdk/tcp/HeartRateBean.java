package com.coolwear.bluesdk.tcp;

/**
 * Author:chenguirui
 * Date:2024/6/6 11:04
 * Description:
 */
public class HeartRateBean {
    private String Datatime;

    private int Data;

    public HeartRateBean() {
    }

    public HeartRateBean(String datatime, int data) {
        Datatime = datatime;
        Data = data;
    }

    public String getDatatime() {
        return Datatime;
    }

    public void setDatatime(String datatime) {
        Datatime = datatime;
    }

    public int getData() {
        return Data;
    }

    public void setData(int data) {
        Data = data;
    }
}
