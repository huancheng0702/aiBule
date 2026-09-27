package com.coolwear.bluesdk.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coolwear.bluesdk.R;

public class CustomBarDialog extends Dialog {

    private TextView tvTitle, tvRightBtn;
    private ProgressBar bar;

    public CustomBarDialog(Context context) {
        super(context);
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_bar_dialog, null);
        //去标题栏
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //使背景透明，实现圆角
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        tvTitle = (TextView) mView.findViewById(R.id.tv_title);
        tvRightBtn = (TextView) mView.findViewById(R.id.tv_right_btn);
        bar = (ProgressBar) mView.findViewById(R.id.pbar);
        setContentView(mView);
    }


    //设置内容
    public void setText(String title) {
        tvTitle.setText(title);
    }


    public void setTitle(String title) {
        tvTitle.setText(title);
    }


    public void setProgress(int progress) {
        bar.setProgress(progress);
    }

    public int getProgress() {
        return bar.getProgress();
    }

    //设置右侧按钮监听器
    public void setOnRightBtnListener(View.OnClickListener listener) {
        tvRightBtn.setOnClickListener(listener);
    }

    public void setTitleBg(int drawable) {
        if (tvTitle != null) {
            tvTitle.setBackgroundResource(drawable);
        }
    }

    public TextView getRightTv() {
        return tvRightBtn;
    }
}
