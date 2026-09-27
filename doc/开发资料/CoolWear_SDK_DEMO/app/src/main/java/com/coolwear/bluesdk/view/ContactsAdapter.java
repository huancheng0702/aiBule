package com.coolwear.bluesdk.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.R;

import java.util.ArrayList;
import java.util.List;

import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_CONTACT_SYNC;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/9/16 0016 9:07
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.MyHolder> {
    private Context context;
    private List<K6_DATA_TYPE_CONTACT_SYNC> list = new ArrayList<>();

    public ContactsAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactsAdapter.MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        if (list == null || list.size() <= 0) return;
        holder.tv_content.setText(list.get(position).getPhoneName() + " " + list.get(position).getPhoneNumber());
        holder.del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClickListener(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addItem(K6_DATA_TYPE_CONTACT_SYNC k6_data_type_contact_syncs) {
        if (list != null) {
            list.add(k6_data_type_contact_syncs);
            notifyDataSetChanged();
        }
    }

    public K6_DATA_TYPE_CONTACT_SYNC getItem(int position) {
        if (list != null && list.size()>0) {
            return list.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (list != null && list.size()>0) {
            list.remove(position);
            notifyDataSetChanged();
        }
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        private TextView tv_content;
        private Button del;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            tv_content = itemView.findViewById(R.id.tv_content);
            del = itemView.findViewById(R.id.del);
        }
    }

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClickListener(int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
