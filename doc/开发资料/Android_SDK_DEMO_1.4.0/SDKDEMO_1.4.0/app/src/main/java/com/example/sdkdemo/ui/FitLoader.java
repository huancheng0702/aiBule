package com.example.sdkdemo.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;


import com.example.sdkdemo.R;

import java.util.ArrayList;

public class FitLoader {

    private static final ArrayList<AppCompatDialog> LOADERS = new ArrayList<>();

    public static void showLoading(Context context) {
        final AppCompatDialog dialog = new AppCompatDialog(context, R.style.loadingDialog);
        try {
            View view = View.inflate(context, R.layout.dialog_loading, null);
            TextView content = view.findViewById(R.id.tv_loading);
            dialog.setContentView(view);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            LOADERS.add(dialog);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            stopLoading(context);
        } catch (Error error) {
            error.printStackTrace();
            stopLoading(context);
        }
    }
    public static void showLoading(Context context,String content) {
        final AppCompatDialog dialog = new AppCompatDialog(context, R.style.loadingDialog);
        try {
            View view = View.inflate(context, R.layout.dialog_loading, null);
            TextView info = view.findViewById(R.id.tv_loading);
            if(!TextUtils.isEmpty(content)){
                info.setText(content);
            }
            dialog.setContentView(view);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);
            LOADERS.add(dialog);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            stopLoading(context);
        } catch (Error error) {
            error.printStackTrace();
            stopLoading(context);
        }
    }
    public static void stopLoading(Context context) {
        if (context == null) return;
        try {
            for (AppCompatDialog dialog : LOADERS) {
                if (dialog != null) {
                    if (dialog.isShowing()) {
                        dialog.cancel();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
