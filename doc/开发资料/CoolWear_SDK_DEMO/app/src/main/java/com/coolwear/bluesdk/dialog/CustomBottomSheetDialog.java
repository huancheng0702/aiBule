package com.coolwear.bluesdk.dialog;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.coolwear.bluesdk.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class CustomBottomSheetDialog extends BottomSheetDialog {
    private Context context;
    private TextView tvCancel,tvSure;
    private LinearLayout llContainerView;
    private View customView,rooView;
    public CustomBottomSheetDialog(@NonNull Context context) {
        this(context,0);
    }

    public CustomBottomSheetDialog(@NonNull Context context, int theme) {
        super(context, theme);
        this.context = context;
        initView();
    }

    private void initView() {
        rooView = View.inflate(context, R.layout.bottom_sheet_layout,null);
        tvCancel = rooView.findViewById(R.id.tv_cancel);
        tvSure = rooView.findViewById(R.id.tv_sure);
        llContainerView = rooView.findViewById(R.id.ll_containerView);
        addContentView(rooView,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setOnSureClickListener(View.OnClickListener onClickListener){
        tvSure.setOnClickListener(onClickListener);
    }
    public void setOnCancelClickListener(View.OnClickListener onClickListener){
        tvCancel.setOnClickListener(onClickListener);
    }

    public void setCustomLayout(int view){
        customView = View.inflate(context, view, null);
        llContainerView.removeAllViews();
        llContainerView.addView(customView);
    }
    public View getCustomLayout(){
        return customView;
    }
}
