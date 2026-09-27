package com.coolwear.bluesdk.net;

public class HttpConfig {

    public static int HTTP_TIMEOUT = 50000;

    public static final String DEBUG_URL = "http://94.191.40.177/";//测试
    public static final String HTTP_CLOUDURL = "http://tool.ota.coolwear.fit:9200/";
    public static final String HTTP_LOCALURL = "http://192.168.101.117:9200/";
    public static final String TAG = "net";

//    public static boolean isDebug(){
//        if(HTTP_CLOUDURL.equals(DEBUG_URL)){
//            return true;
//        }else{
//            return false;
//        }
//    }

    /**
     * 公共参数
     */
    public static final class COMMONPARAMETER {
        public static final String TIMESTAMP = "timestamp";
        public static final String APPVERSION = "version";
        public static final String APPVERSIONNAME = "versionName";
        public static final String UID = "uid";
        public static final String DEVICEID = "deviceId";
        public static final String DEVICETYPE = "deviceType";
        public static final String SIGN = "sign";
        public static final String TOKEN = "token";//在请求头
        public static final String ANDROID = "ANDROID";
        public static final String LATITUDE = "latitude";//纬度
        public static final String LONGITUDE = "longitude";//经度
        public static final String NONCE = "nonce";//时间值加随机值
        public static final String param = "param";//rsa加密后的参数
    }

}