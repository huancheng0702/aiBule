package com.example.sdkdemo.data;

/**
 * Author:null
 * Date:2026/8/18 18:25
 * Description:添加您的描述
 */
public class BleDeviceItem {

    private String name;
    private String mac;
    private int rssi;

    public BleDeviceItem(String name, String mac, int rssi) {
        this.name = name;
        this.mac = mac;
        this.rssi = rssi;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public int getRssi() {
        return rssi;
    }
}
