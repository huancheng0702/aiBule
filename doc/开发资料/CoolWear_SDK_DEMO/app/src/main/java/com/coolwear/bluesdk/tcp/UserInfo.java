package com.coolwear.bluesdk.tcp;

/**
 * Author:chenguirui
 * Date:2024/6/6 11:04
 * Description:
 */
public class UserInfo {
    private String UserName;

    public UserInfo() {
    }

    public UserInfo(String userName) {
        UserName = userName;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }
}
