package com.coolwear.bluesdk.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ce.com.cenewbluesdk.CEBC;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/23 0023 17:39
 */
public class PushListAdapter extends RecyclerView.Adapter<PushListAdapter.MyHolder> {
    private Context mContext;
    private List<HashMap> installApps = new ArrayList<>();

    public PushListAdapter(Context context) {
        this.mContext = context;
    }

    public void setInstallApps(List<HashMap> list) {
        if (installApps == null) {
            installApps = new ArrayList<>();
        }
        installApps.clear();
        this.installApps = list;
        notifyDataSetChanged();
    }

    public List<HashMap> getInstallApps() {
        return installApps;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.push_list_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PushListAdapter.MyHolder holder, int position) {
        final HashMap tPushApp = installApps.get(position);
        Log.i("pushlist",(String)tPushApp.get(CEBC.PACKAGEINFO.KEYAPPNAME)+"------"+(String)tPushApp.get("keyAppPackageName"));
        if (((String) tPushApp.get(CEBC.PACKAGEINFO.KEYAPPPACKAGENAME)).contains("contacts") ||
                ((String) tPushApp.get(CEBC.PACKAGEINFO.KEYAPPPACKAGENAME)).contains("dialer")) {
            holder.iv_icon.setImageResource(R.mipmap.icon_phone_call);
        } else {
            holder.iv_icon.setImageDrawable((Drawable) tPushApp.get(CEBC.PACKAGEINFO.KEYAPPICON));
        }
        holder.tv_title.setText((String) tPushApp.get(CEBC.PACKAGEINFO.KEYAPPNAME));
        holder.cb_switch.setOpen((boolean) tPushApp.get(CEBC.PACKAGEINFO.KEYAPPISOPEN));

        holder.cb_switch.setmOnSwithButonChangedListener(new MySwitchButton.OnSwithButonChangedListener() {
            @Override
            public void onCheckedChanged(boolean isOPen) {
                installApps.get(position).put(CEBC.PACKAGEINFO.KEYAPPISOPEN, isOPen);
            }
        });

    }

    @Override
    public int getItemCount() {
        return installApps.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        ImageView iv_icon;
        TextView tv_title;
        MySwitchButton cb_switch;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            iv_icon = itemView.findViewById(R.id.pushItem_icon);
            tv_title = itemView.findViewById(R.id.pushItem_title);
            cb_switch = itemView.findViewById(R.id.pushItem_switch);
        }
    }
}
