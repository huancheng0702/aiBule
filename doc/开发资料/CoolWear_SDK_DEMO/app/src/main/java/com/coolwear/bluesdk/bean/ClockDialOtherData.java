package com.coolwear.bluesdk.bean;

/**
 * @author: ZhouMingLiang
 * @description:
 * @date: 2019/10/11 16:04
 */
public class ClockDialOtherData {
    int value;
    int show;
    String showInWatch;
    int bitmap;
    public ClockDialOtherData(int value, int show) {
        this.value = value;
        this.show = show;
    }

    public ClockDialOtherData(int value, int show, String showInWatch) {
        this.value = value;
        this.show = show;
        this.showInWatch = showInWatch;
    }

    public ClockDialOtherData(int value, int show, String showInWatch, int bitmap) {
        this.value = value;
        this.show = show;
        this.showInWatch = showInWatch;
        this.bitmap = bitmap;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getShow() {
        return show;
    }

    public void setShow(int show) {
        this.show = show;
    }

    public String getShowInWatch() {
        return showInWatch;
    }

    public void setShowInWatch(String showInWatch) {
        this.showInWatch = showInWatch;
    }

    public int getBitmap() {
        return bitmap;
    }

    public void setBitmap(int bitmap) {
        this.bitmap = bitmap;
    }
}
