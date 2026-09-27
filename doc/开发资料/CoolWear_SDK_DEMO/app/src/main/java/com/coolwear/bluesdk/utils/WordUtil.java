package com.coolwear.bluesdk.utils;

import android.content.res.Resources;

import com.coolwear.bluesdk.MyApplication;

/**
 * Created by zhengyunbin on 2019/8/13.
 * 获取string.xml中的字
 */

public class WordUtil {

    private static Resources sResources;

    static {
        sResources = MyApplication.getInstance().getResources();
    }

    public static String getString(int res) {
        return sResources.getString(res);
    }
}
