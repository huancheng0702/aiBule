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

import com.coolwear.bluesdk.R;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.entity.MyBleDevice;

public class LightLeakScanDeviceListAdapter extends RecyclerView.Adapter<LightLeakScanDeviceListAdapter.MyHolder> {
    private Context context;
    private List<MyBleDevice> list = new ArrayList<>();

    public List<MyBleDevice> getList() {
        return list;
    }

    public LightLeakScanDeviceListAdapter(Context context) {
        this.context = context;
    }

    public void setList(List<MyBleDevice> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.lls_dev_list_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.devName.setText(list.get(position).getName());
        holder.devMac.setText("MAC:  " + list.get(position).getmBluetoothDevice().getAddress());
        holder.ivCheck.setSelected(list.get(position).isSelected());
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
        private ImageView ivCheck;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            devName = itemView.findViewById(R.id.tv_name);
            devMac = itemView.findViewById(R.id.tv_mac);
            ivCheck = itemView.findViewById(R.id.iv_check);
        }
    }

    public void notifyItem(int position) {
        for (int i = 0; i < list.size(); i++) {
            MyBleDevice stubItem = list.get(i);
            if (position == i) {
                stubItem.setSelected(!stubItem.isSelected());
            } else {
                stubItem.setSelected(false);
            }
            list.set(i, stubItem);
        }
        setList(list);
    }

    public void notifyItemMultiple(int position) {
        MyBleDevice stubItem = list.get(position);
        stubItem.setSelected(!stubItem.isSelected());
        list.set(position, stubItem);
        setList(list);
    }

    public void notifyItemAllCheck(boolean isCheckAll) {
        for (int i = 0; i < list.size(); i++) {
            MyBleDevice stubItem = list.get(i);
            stubItem.setSelected(isCheckAll);
            list.set(i, stubItem);
        }
        setList(list);
    }

    public List<MyBleDevice> getSelectedDevices() {
        List<MyBleDevice> list = new ArrayList<>();
        for (MyBleDevice device : getList()) {
            if (device.isSelected()) {
                list.add(device);
            }
        }
        return list;
    }
}
