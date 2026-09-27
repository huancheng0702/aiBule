package com.coolwear.bluesdk.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.coolwear.bluesdk.MyApplication;
import com.coolwear.bluesdk.R;


public class ToastUtil {

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }




    private static Toast sToast;
    private static long sLastTime;
    private static String sLastString;
    static {
        sToast = makeToast();
    }

    private static Toast makeToast() {
        if(Build.VERSION.SDK_INT < 30){
            Toast toast = new Toast(MyApplication.getInstance());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 200);
            View view = LayoutInflater.from(MyApplication.getInstance()).inflate(R.layout.view_toast, null);
            toast.setView(view);
            return toast;
        }
        return null;
    }


    public static void show(int res) {
        show(WordUtil.getString(res));
    }

    public static void show(String s) {
        if (TextUtils.isEmpty(s)) {
            return;
        }
        if(sToast!=null){
            long curTime = System.currentTimeMillis();
            if (curTime - sLastTime > 2000) {
                sLastTime = curTime;
                sLastString = s;
                try{
                    sToast.setText(s);
                }catch (Exception e){
                    e.printStackTrace();
//                    Toast.makeText(BaseApplication.getInstance(),s,Toast.LENGTH_SHORT).show();
                    return;
                }
                sToast.show();
            } else {
                if (!s.equals(sLastString)) {
                    sLastTime = curTime;
                    sLastString = s;
                    sToast = makeToast();
                    try {
                        sToast.setText(s);
                    }catch (Exception e){
                        e.printStackTrace();
//                        Toast.makeText(BaseApplication.getInstance(),s,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sToast.show();
                }
            }
        }else{
            Toast.makeText(MyApplication.getInstance(),s,Toast.LENGTH_SHORT).show();
        }
    }
}
