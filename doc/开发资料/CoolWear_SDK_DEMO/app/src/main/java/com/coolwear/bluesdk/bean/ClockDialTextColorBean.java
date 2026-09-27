package com.coolwear.bluesdk.bean;

/**
 * @author: ZhouMingLiang
 * @description:
 * @date: 2019/10/11 15:19
 */
public class ClockDialTextColorBean {
    int color;
    boolean choice;
    public ClockDialTextColorBean(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isChoice() {
        return choice;
    }

    public void setChoice(boolean choice) {
        this.choice = choice;
    }
}
