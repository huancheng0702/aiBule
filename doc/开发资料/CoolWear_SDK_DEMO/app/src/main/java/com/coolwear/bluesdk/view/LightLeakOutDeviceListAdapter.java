package com.coolwear.bluesdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.Constant;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.entity.MyBleDevice;

public class LightLeakOutDeviceListAdapter extends RecyclerView.Adapter<LightLeakOutDeviceListAdapter.MyHolder> {
    private Context context;
    private List<MyBleDevice> list = new ArrayList<>();
    private int baseWTHr = 6000;
    private int baseTr = 10000;
    private int baseWTOb = 10000;

    public List<MyBleDevice> getList() {
        return list;
    }

    public LightLeakOutDeviceListAdapter(Context context) {
        this.context = context;
    }

    public void setList(List<MyBleDevice> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.lls_out_list_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        MyBleDevice item = list.get(position);
        holder.devName.setText(list.get(position).getName());
        holder.devMac.setText("MAC:  " + item.getmBluetoothDevice().getAddress());
        holder.gx.setText(item.getWearingThresholdHr() + "");
        holder.ry.setText(item.getThresholdRelease() + "");
        holder.iz.setText(item.getWearingThresholdOb() + "");
        if (item.getWearingThresholdHr() > Integer.parseInt(SPUtils.getString("FIELD_GREEN_VALUES", "6000"))) {
            item.setTestResults(0);
        }
        if (item.getThresholdRelease() > Integer.parseInt(SPUtils.getString("FIELD_RED_VALUES", "10000"))) {
            item.setTestResults(0);
        }
        if (item.getWearingThresholdOb() > Integer.parseInt(SPUtils.getString("FIELD_INFRARED_VALUES", "10000"))) {
            item.setTestResults(0);
        }
        holder.ivState.setImageResource(item.getTestResults() == 0 ? R.mipmap.ic_fail : R.mipmap.ic_pass);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClickListener(position);
                }
            }
        });
    }

    private ItemClickListener itemClickListener;

    public interface ItemClickListener {
        void onItemClickListener(int position);
    }

    public void setOnItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        private TextView devName;
        private TextView devMac;
        private TextView gx;
        private TextView ry;
        private TextView iz;
        private ImageView ivState;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            devName = itemView.findViewById(R.id.tv_name);
            devMac = itemView.findViewById(R.id.tv_mac);
            gx = itemView.findViewById(R.id.tv_green_x);
            ry = itemView.findViewById(R.id.tv_red_y);
            iz = itemView.findViewById(R.id.tv_infrared_z);
            ivState = itemView.findViewById(R.id.iv_state);
        }
    }

}
