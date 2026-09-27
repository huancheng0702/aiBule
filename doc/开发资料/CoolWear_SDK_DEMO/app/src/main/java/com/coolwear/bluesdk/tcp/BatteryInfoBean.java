package com.coolwear.bluesdk.tcp;

/**
 * Author:chenguirui
 * Date:2024/6/6 11:04
 * Description:
 */
public class BatteryInfoBean {
    private int Voltage;
    private int Percent;

    public BatteryInfoBean(int voltage, int percent) {
        Voltage = voltage;
        Percent = percent;
    }

}
