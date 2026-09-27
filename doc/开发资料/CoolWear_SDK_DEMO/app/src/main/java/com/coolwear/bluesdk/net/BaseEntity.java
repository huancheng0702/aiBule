package com.coolwear.bluesdk.net;


import java.io.Serializable;

public class BaseEntity<T> implements Serializable {
    private int state;
    private String message;
    private T data;


    public boolean isSuccess(){
        return getState()== ResponseCode.SUCCESS_CODE.getValue();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
