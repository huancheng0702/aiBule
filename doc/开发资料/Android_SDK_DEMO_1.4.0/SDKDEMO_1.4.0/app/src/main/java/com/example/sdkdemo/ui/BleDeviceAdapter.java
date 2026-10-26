package com.example.sdkdemo.ui;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sdkdemo.R;
import com.example.sdkdemo.data.BleDeviceItem;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.entity.MyBleDevice;

/**
 * Author:null
 * Date:2026/8/18 18:24
 * Description:添加您的描述
 */
public class BleDeviceAdapter
        extends RecyclerView.Adapter<BleDeviceAdapter.ViewHolder> {

    private final List<MyBleDevice> data = new ArrayList<>();

    private OnItemClickListener listener;

    public void setData(List<MyBleDevice> list) {
        data.clear();

        if (list != null) {
            data.addAll(list);
        }

        notifyDataSetChanged();
    }

    public void addOrUpdate(MyBleDevice item) {

        for (int i = 0; i < data.size(); i++) {

            if (data.get(i).getmBluetoothDevice().getAddress().equals(item.getmBluetoothDevice().getAddress())) {
                data.set(i, item);
                notifyItemChanged(i);
                return;
            }
        }

        data.add(item);

        notifyItemInserted(data.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_ble_device,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        MyBleDevice item = data.get(position);

        holder.tvName.setText(
                TextUtils.isEmpty(item.getName())
                        ? "Unknown"
                        : item.getName()
        );

        holder.tvMac.setText(item.getmBluetoothDevice().getAddress());
        holder.tvRssi.setText(
                "RSSI: " + item.getRssi()
        );

        int rssi = item.getRssi();

        if (rssi >= -60) {

            holder.tvRssi.setTextColor(
                    Color.parseColor("#25D47C")
            );

        } else if (rssi >= -70) {

            holder.tvRssi.setTextColor(
                    Color.parseColor("#FFB21C")
            );

        } else {

            holder.tvRssi.setTextColor(
                    Color.parseColor("#FF453A")
            );
        }

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvMac;
        TextView tvRssi;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(
                    R.id.tv_name
            );

            tvMac = itemView.findViewById(
                    R.id.tv_mac
            );

            tvRssi = itemView.findViewById(
                    R.id.tv_rssi
            );
        }
    }

    public interface OnItemClickListener {
        void onItemClick(MyBleDevice item);
    }

    public void setOnItemClickListener(
            OnItemClickListener listener) {

        this.listener = listener;
    }
}