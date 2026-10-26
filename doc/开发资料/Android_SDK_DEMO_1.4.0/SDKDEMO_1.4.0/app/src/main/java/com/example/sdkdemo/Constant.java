package com.example.sdkdemo;

import java.io.File;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/25 0025 9:30
 */
public class Constant {
    public static String FIELD_VALUES = "3000";//默认阀值为3000
    public static String FIELD_GREEN_VALUES = "6000";//默认阀值为3000
    public static String FIELD_RED_VALUES = "10000";//默认阀值为3000
    public static String FIELD_INFRARED_VALUES = "10000";//默认阀值为3000
    public static String MORE_IMG_PATH = "";
    public static String CUSTOMFILENAME = "custom_pic.jpg";
    public static String CUSTOM_DEFAULT_FILENAME = "custom_default.jpg";
    public static String tempCustomImg = "";
    public static String customImg = "";
    public static String customUrl = "";
    public static String customId = "";

    public static final class FILEPATH {

        public static String getMoreDialPngPath(String devId, String fileName) {
            return getSDPath() + File.separator + "Watch_Face" + File.separator + devId + File.separator + fileName + ".png";
        }

        public static String getMoreDialBinPath(String devId, String fileName) {
            return getSDPath() + File.separator + "Watch_Face" + File.separator + devId + File.separator + fileName + ".bin";
        }

        public static String getCustomPath(String devId) {
            return getSDPath() + File.separator + "Custom_Face" + File.separator + devId + File.separator;
        }

        public static String getSDPath() {
            return MyApplication.getInstance().getFilesDir().getPath();
//            return Objects.requireNonNull(MyApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
        }
    }

}
