package com.coolwear.bluesdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.R;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.entity.MyBleDevice;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/17 0017 14:25
 */
public class ScanDeviceListAdapter extends RecyclerView.Adapter<ScanDeviceListAdapter.MyHolder>{
    private Context context;
    private List<MyBleDevice> list = new ArrayList<>();

    public List<MyBleDevice> getList() {
        return list;
    }

    public ScanDeviceListAdapter(Context context) {
        this.context = context;
    }
    public void setList(List<MyBleDevice> list) {
        this.list = list;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.scan_device_list_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.tvIndex.setText((position+1)+"");
        holder.devName.setText(list.get(position).getName());
        holder.devMac.setText("ID:" + list.get(position).getmK6ManufacturerInfo().name + "  BTmac:" + list.get(position).getmBluetoothDevice().getAddress());
      //  holder.tvInfo.setText(list.get(position).getmK6ManufacturerInfo().versionInfo);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemClickListener!=null){
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
        private TextView tvIndex;
        private TextView tvInfo;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            devName =itemView.findViewById(R.id.tv_name);
            devMac = itemView.findViewById(R.id.tv_mac);
            tvIndex = itemView.findViewById(R.id.tv_index);
            tvInfo = itemView.findViewById(R.id.tv_info);
        }
    }
}
