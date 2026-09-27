package com.coolwear.bluesdk.bean;

import java.io.Serializable;
import java.util.List;


public class CheckDataTypeBean implements Serializable {

    private boolean hrIsCheck;
    private boolean gsIsCheck;
    private boolean batteryIsCheck;
    private String userName;

    public boolean isHrIsCheck() {
        return hrIsCheck;
    }

    public void setHrIsCheck(boolean hrIsCheck) {
        this.hrIsCheck = hrIsCheck;
    }

    public boolean isGsIsCheck() {
        return gsIsCheck;
    }

    public void setGsIsCheck(boolean gsIsCheck) {
        this.gsIsCheck = gsIsCheck;
    }

    public boolean isBatteryIsCheck() {
        return batteryIsCheck;
    }

    public void setBatteryIsCheck(boolean batteryIsCheck) {
        this.batteryIsCheck = batteryIsCheck;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
