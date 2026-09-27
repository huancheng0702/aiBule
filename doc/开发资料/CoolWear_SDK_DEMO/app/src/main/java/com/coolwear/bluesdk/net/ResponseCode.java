package com.coolwear.bluesdk.net;

/**
 * @Author：zhengyunbin
 * @CreateDate：2019/8/16 11:58
 * @Description：
 */
public enum ResponseCode {
    SUCCESS_CODE(200),//请求成功
    NETWORK_ERROR(-1);//网络错误
    private final int value;

    ResponseCode(int value) {
        this.value = value;

    }

    public int getValue() {
        return value;
    }
}
