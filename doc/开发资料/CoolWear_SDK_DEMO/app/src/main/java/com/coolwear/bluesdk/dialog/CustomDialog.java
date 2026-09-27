package com.coolwear.bluesdk.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coolwear.bluesdk.R;

public class CustomDialog extends Dialog {

    private TextView tvTitle, tvContent, tvLeftBtn, tvRightBtn;
    private LinearLayout ll_content;
    private View view;

    public CustomDialog(Context context) {
        super(context);
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_dialog, null);
        //去标题栏
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //使背景透明，实现圆角
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        tvTitle = (TextView) mView.findViewById(R.id.tv_title);
        tvContent = (TextView) mView.findViewById(R.id.tv_content);
        tvLeftBtn = (TextView) mView.findViewById(R.id.tv_left_btn);
        tvRightBtn = (TextView) mView.findViewById(R.id.tv_right_btn);
        ll_content = mView.findViewById(R.id.ll_content);
        setContentView(mView);
    }


    //设置内容
    public void setText(String title, String content, String leftText, String rightText) {
        tvTitle.setText(title);
        tvContent.setText(content);
        tvLeftBtn.setText(leftText);
        tvRightBtn.setText(rightText);
    }

    public void setContent(String content) {
        tvContent.setText(content);
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public void setCustomView(Context context, int layout) {
        View view = View.inflate(context, layout, null);
        this.view = view;
        ll_content.removeAllViews();
        ll_content.addView(view);
    }

    public void setCustomView(View view) {
        this.view = view;
        ll_content.removeAllViews();
        ll_content.addView(view);
    }

    public View getCustomView() {
        return view;
    }



    //设置左侧按钮监听器
    public void setOnLeftBtnListener(View.OnClickListener listener) {
        tvLeftBtn.setOnClickListener(listener);
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

    public TextView getRightTv(){
        return tvRightBtn;
    }
}
